package ch.ntb.inf.kmip.stub.transport;
/**
 * KMIPStubTransportLayerHTTPS.java
 * ------------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * ------------------------------------------------------------------
 * Description:
 * The KMIPStubTransportLayerHTTPS provides the communication between
 * a server and a client via HTTPS, using a HttpsUrlConnection.
 *
 * @author     Stefanie Meile <stefaniemeile@gmail.com>
 * @author     Michael Guster <michael.guster@gmail.com>
 * @org.       NTB - University of Applied Sciences Buchs, (CH)
 * @copyright  Copyright ��� 2013, Stefanie Meile, Michael Guster
 * @license    Simplified BSD License (see LICENSE.TXT)
 * @version    1.0, 2013/08/09
 * @since      Class available since Release 1.0
 *
 *
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ntb.inf.kmip.utils.KMIPUtils;

/**
 * The KMIPStubTransportLayerHTTPS provides the communication between a server and a client via HTTPS,
 * using a HttpsUrlConnection.
 */
public class KMIPStubTransportLayerHTTPS implements KMIPStubTransportLayerInterface{

	private static final Logger logger = LoggerFactory.getLogger(KMIPStubTransportLayerHTTP.class);

	private SSLSocketFactory factory;
	private String url;
	private String ssoCookie = null;
	private String keyStoreFileName;
	private String keyStorePassword;
	private final String alias = "ntb";



	public KMIPStubTransportLayerHTTPS() {
		logger.info("KMIPStubTransportLayerHTTPS initialized...");

		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
			    new javax.net.ssl.HostnameVerifier(){

			        @Override
                    public boolean verify(final String hostname,
			                final javax.net.ssl.SSLSession sslSession) {
			        		return true;
			        }
			    });
	}

    @Override
    public ArrayList<Byte> send(final ArrayList<Byte> al) throws Exception {
    	try {
            // create key and trust managers
            KeyManager[] keyManagers = createKeyManagers(keyStoreFileName, keyStorePassword, alias);

            TrustManager[] trustManagers = createTrustManagers();

            // init context with managers data
            factory = initItAll(keyManagers, trustManagers);

            // execute Post
            return executePost(url, ssoCookie, al, factory);
        } catch (Exception e) {
	    logger.error("send(): " + e);
	    throw e;
        }
    }


	private ArrayList<Byte> executePost(final String targetURL, final String ssoCookie, final ArrayList<Byte> al,
		final SSLSocketFactory sslSocketFactory) throws Exception {
        URLConnection connection = new URL(targetURL).openConnection();
        HttpsURLConnection httpsConnection = null;
        if (connection instanceof HttpsURLConnection) {
        	httpsConnection = (HttpsURLConnection) connection;
        }
        else{
        	logger.warn("Connection is no HttpsURLConnection!");
        }

        try{
        	sendRequest(httpsConnection, ssoCookie, al);
        	byte[] response = getResponse(httpsConnection);
			return KMIPUtils.convertByteArrayToArrayList(response);
		}
        catch (Exception e) {
			logger.error("executePost(): " + e);
			throw e;
		}
        finally {
			if (httpsConnection != null) {
				httpsConnection.disconnect();
			}
		}
    }


	private void sendRequest(final HttpsURLConnection httpsConnection, final String ssoCookie, final ArrayList<Byte> al) throws IOException {
    	httpsConnection.setSSLSocketFactory(factory);
    	httpsConnection.setRequestMethod("POST");
    	httpsConnection.setRequestProperty("Content-Type","*/*");
		if (ssoCookie != null) {
			httpsConnection.setRequestProperty("Cookie", ssoCookie);
		}
    	httpsConnection.setDoInput(true);
    	httpsConnection.setDoOutput(true);

		// Send request
		DataOutputStream wr = new DataOutputStream(httpsConnection.getOutputStream());
		wr.write(KMIPUtils.toByteArray(al));

		wr.flush();
		wr.close();
	}


	private byte[] getResponse(final HttpsURLConnection httpsConnection) throws IOException {
		InputStream is = httpsConnection.getInputStream();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			 buffer.write(data, 0, nRead);
		}

		buffer.flush();
		return buffer.toByteArray();
	}

	private SSLSocketFactory initItAll(final KeyManager[] keyManagers, final TrustManager[] trustManagers)
        throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(keyManagers, trustManagers, null);
        return context.getSocketFactory();
    }

    private KeyManager[] createKeyManagers(final String keyStoreFileName, final String keyStorePassword, final String alias)
       throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, NoSuchProviderException, UnrecoverableKeyException {
        //create keystore object, load it with keystorefile data
        logger.debug("Keystore for client socket = {}", keyStoreFileName);
        java.io.InputStream inputStream = new java.io.FileInputStream(keyStoreFileName);
        KeyStore keyStore;
        if (keyStoreFileName.endsWith("bcfks")) {
          Provider provider = new BouncyCastleFipsProvider();
          Security.addProvider(provider);
          logger.debug("createClientSocket() add provider = {}", provider.getName());
          keyStore = KeyStore.getInstance("BCFKS", provider.getName());
        } else if (keyStoreFileName.endsWith("p12")) {
          keyStore = KeyStore.getInstance("PKCS12");
        } else if (keyStoreFileName.endsWith("jks")) {
          keyStore = KeyStore.getInstance("JKS");
        } else {
          logger.error("Only .bcfks, .p12, and .jks keystore are supported.  Unsupported keystore type for {}", keyStoreFileName);
          return null;
        }
        keyStore.load(inputStream, keyStorePassword == null ? null : keyStorePassword.toCharArray());

        KeyManager[] managers;
        if (alias != null) {
        	managers = new KeyManager[] {new KMIPStubTransportLayerHTTPS().new AliasKeyManager(keyStore, alias, keyStorePassword)};
        } else {
            //create keymanager factory and load the keystore object in it
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword == null ? null : keyStorePassword.toCharArray());
            managers = keyManagerFactory.getKeyManagers();
        }
        return managers;
    }

	private TrustManager[] createTrustManagers() {
		return new TrustManager[] {
        	    new X509TrustManager() {
        	        @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        	            return null;
        	        }
        	        @Override
                    public void checkClientTrusted(
        	            final java.security.cert.X509Certificate[] certs, final String authType) {
        	            }
        	        @Override
                    public void checkServerTrusted(
        	            final java.security.cert.X509Certificate[] certs, final String authType) {
        	        }
        	    }
        	};

	}


    private class AliasKeyManager implements X509KeyManager {

        private final KeyStore _ks;
        private final String _alias;
        private final String _password;

        public AliasKeyManager(final KeyStore ks, final String alias, final String password) {
            _ks = ks;
            _alias = alias;
            _password = password;
        }

        @Override
        public String chooseClientAlias(final String[] str, final Principal[] principal, final Socket socket) {
            return _alias;
        }

        @Override
        public String chooseServerAlias(final String str, final Principal[] principal, final Socket socket) {
            return _alias;
        }

        @Override
        public X509Certificate[] getCertificateChain(final String alias) {
            try {
                java.security.cert.Certificate[] certificates = this._ks.getCertificateChain(alias);
                if(certificates == null){
                	throw new FileNotFoundException("no certificate found for alias:" + alias);
                }
                X509Certificate[] x509Certificates = new X509Certificate[certificates.length];
                System.arraycopy(certificates, 0, x509Certificates, 0, certificates.length);
                return x509Certificates;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String[] getClientAliases(final String str, final Principal[] principal) {
            return new String[] { _alias };
        }

        @Override
        public PrivateKey getPrivateKey(final String alias) {
            try {
                return (PrivateKey) _ks.getKey(alias, _password == null ? null : _password.toCharArray());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String[] getServerAliases(final String str, final Principal[] principal) {
            return new String[] { _alias };
        }

    }

    /**
     * Sets the configuration parameters of the transport layer.
     *
     * @param configParams :  the configuration parameters to be set.
     */
    @Override
    public void setConfigParameters(final Map<String, Object> configParams) throws IllegalArgumentException {
        String msg;

        url = (String) configParams.get(CFG_KMS_URI);
        logger.info("Kmip Service URI = " + url);

        // keystore
        keyStoreFileName = (String) configParams.get(CFG_KEYSTORE_FILE);
        if (keyStoreFileName == null) {
            msg = "Keystore file not found in config file.";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        keyStorePassword = (String) configParams.get(CFG_KEYSTORE_PASSWORD);
        if (keyStorePassword == null) {
            msg = "Keystore password not found in config file.";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
        ssoCookie = (String) configParams.get(CFG_SSO_COOKIE);
    }

}
