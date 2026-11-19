package ch.ntb.inf.kmip.stub.transport;
/**
 * KMIPStubTransportLayerHTTPSLocalHost.java
 * -----------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * -----------------------------------------------------------------
 * Description:
 * The KMIPStubTransportLayerHTTPSLocalHost provides the
 * communication between a server and a client via HTTPS, using a
 * HttpsUrlConnection, where the server and the client both are
 * running on the local host.
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ntb.inf.kmip.utils.KMIPUtils;

/**
 * The KMIPStubTransportLayerHTTPSLocalHost provides the communication between a server and a client via HTTPS,
 * using a HttpsUrlConnection, where the server and the client both are running on the local host.
 */
public class KMIPStubTransportLayerHTTPSLocalHost implements KMIPStubTransportLayerInterface{

	private static final Logger logger = LoggerFactory.getLogger(KMIPStubTransportLayerHTTPSLocalHost.class);

	static {
	    // for localhost testing only
	    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
	        (hostname, sslSession) -> hostname.equals("localhost"));
	}

	private String url;
	private String ssoCookie = null;
	private String keyStoreFileName;
	private String keyStorePassword;
	private String trustStoreFileName;
	private String trustStorePassword;
	private final String alias = null;

	public KMIPStubTransportLayerHTTPSLocalHost() {
		logger.info("KMIPStubTransportLayerHTTPS initialized...");
	}

    @Override
    public ArrayList<Byte> send(final ArrayList<Byte> al) throws Exception {
    	String kmipRequest = KMIPUtils.convertArrayListToHexString(al);
    	try {
    		// prepare Post-Parameter-String
    		String parameter = "KMIPRequest="+URLEncoder.encode(kmipRequest, StandardCharsets.UTF_8);
            // create key and trust managers
            KeyManager[] keyManagers = createKeyManagers(keyStoreFileName, keyStorePassword, alias);
            TrustManager[] trustManagers = createTrustManagers(trustStoreFileName, trustStorePassword);
            // init context with managers data
            SSLSocketFactory factory = initItAll(keyManagers, trustManagers);
            // execute Post
            String responseString = executePost(url, ssoCookie, parameter, factory);
            return KMIPUtils.convertHexStringToArrayList(responseString);
        } catch (Exception e) {
	    logger.error("send(): " + e);
	    throw e;
        }
    }

    private static String executePost(final String targetURL, final String ssoCookie, final String urlParameters,
		final SSLSocketFactory sslSocketFactory) throws Exception {
        URL url = URI.create(targetURL).toURL();
        URLConnection connection = url.openConnection();
        HttpsURLConnection httpsConnection;
        if (connection instanceof HttpsURLConnection lConnection) {
        	httpsConnection = lConnection;
        }
        else{
        	logger.warn("Connection is no HttpsURLConnection!");
        	throw new IOException("Connection is no HttpsURLConnection: " + targetURL);
        }

        try{
            httpsConnection.setSSLSocketFactory(sslSocketFactory);
        	httpsConnection.setRequestMethod("POST");
        	httpsConnection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        	httpsConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
        	httpsConnection.setRequestProperty("Content-Language", "en-US");
        	httpsConnection.setRequestProperty("Cash-Control","no-cache");
		if (ssoCookie != null) {
			connection.setRequestProperty("Cookie", ssoCookie);
		}
        	httpsConnection.setUseCaches(false);
        	httpsConnection.setDoInput(true);
        	httpsConnection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(httpsConnection.getOutputStream());
			wr.writeBytes(urlParameters);

			wr.flush();
			wr.close();

			// Get Response
			InputStream is = httpsConnection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuilder response = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();

			return response.toString();
		} catch (Exception e) {
			logger.error("executePost(): " + e);
			throw e;
		} finally {
			httpsConnection.disconnect();
		}
    }

    private static SSLSocketFactory initItAll(final KeyManager[] keyManagers, final TrustManager[] trustManagers)
        throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, trustManagers, null);
        return context.getSocketFactory();
    }

    private static KeyManager[] createKeyManagers(final String keyStoreFileName, final String keyStorePassword, final String alias)
        throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        //create Inputstream to keystore file
        java.io.InputStream inputStream = new java.io.FileInputStream(keyStoreFileName);
        //create keystore object, load it with keystorefile data
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(inputStream, keyStorePassword == null ? null : keyStorePassword.toCharArray());

        KeyManager[] managers;
        if (alias != null) {
        	managers = new KeyManager[] {new KMIPStubTransportLayerHTTPSLocalHost().new AliasKeyManager(keyStore, alias, keyStorePassword)};
        } else {
            //create keymanager factory and load the keystore object in it
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword == null ? null : keyStorePassword.toCharArray());
            managers = keyManagerFactory.getKeyManagers();
        }
        //return
        return managers;
    }

    private static TrustManager[] createTrustManagers(final String trustStoreFileName, final String trustStorePassword)
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        //create Inputstream to truststore file
        java.io.InputStream inputStream = new java.io.FileInputStream(trustStoreFileName);
        //create keystore object, load it with truststorefile data
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(inputStream, trustStorePassword == null ? null : trustStorePassword.toCharArray());

        //create trustmanager factory and load the keystore object in it
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        //return
        return trustManagerFactory.getTrustManagers();
    }

//    private static void printKeystoreInfo(KeyStore keystore) throws KeyStoreException {
//        System.out.println("Provider : " + keystore.getProvider().getName());
//        System.out.println("Type : " + keystore.getType());
//        System.out.println("Size : " + keystore.size());
//
//        Enumeration<?> en = keystore.aliases();
//        while (en.hasMoreElements()) {
//            System.out.println("Alias: " + en.nextElement());
//        }
//    }


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
