package gov.nasa.jpl.ammos.kmc.keyclient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientException.KmcKeyOpsErrorCode;

/**
 * A {@link KmcKeyClient} that manages keys in a keystore.
 *
 *
 */
public class KmcKeystoreKeyClient implements KmcKeyClient {

    public static final String KEYSTORE_TYPE_JKS = "JKS";
    public static final String KEYSTORE_TYPE_JCEKS = "JCEKS";
    public static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";
    public static final String KEYSTORE_TYPE_BCFKS = "BCFKS";

    private final String keystoreLocation;
    private final String keystorePass;
    private final KeyStore keystore;
    private InputStream keystoreStream;

    private static final Logger logger = LoggerFactory.getLogger(KmcKeystoreKeyClient.class);

    /**
     * Constructor of KmcKeyClient for managing keys in the KMS.
     * @param keystoreLocation File path to the keystore.
     * @param keystorePass Password of the keystore.
     * @param keystoreType Type of keystore.
     * @throws KmcKeyClientException if errors getting the config parameters.
     */
    public KmcKeystoreKeyClient(final String keystoreLocation,
            final String keystorePass, final String keystoreType)
                    throws KmcKeyClientException {
        this.keystoreLocation = keystoreLocation;
        if (keystorePass == null) {
            this.keystorePass = "changeit";
        } else {
            this.keystorePass = keystorePass;
        }

        try {
            keystoreStream = new FileInputStream(keystoreLocation);
            logger.info("Load keystore at " + keystoreLocation);
        } catch (FileNotFoundException e) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            keystoreStream = classLoader.getResourceAsStream(keystoreLocation);
            if (keystoreStream == null) {
                String msg = "Failed to open keystore from " + keystoreLocation + " and from CLASSPATH: " + e;
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
            }
            logger.info("Load keystore " + keystoreLocation + " from CLASSPATH");
        }
        try {
            if (KEYSTORE_TYPE_BCFKS.equals(keystoreType)) {
                Provider provider = new BouncyCastleFipsProvider();
                Security.addProvider(provider);
                keystore = KeyStore.getInstance(keystoreType, "BCFIPS");
                logger.debug("Create keystore of type {} with provider {}", keystoreType, provider.getName());
            } else if (KEYSTORE_TYPE_PKCS12.equals(keystoreType) ||
                       KEYSTORE_TYPE_JCEKS.equals(keystoreType) ||
                       KEYSTORE_TYPE_JKS.equals(keystoreType)) {
                keystore = KeyStore.getInstance(keystoreType);
                logger.debug("Create {} keystore", keystoreType);
            } else {
                String msg = "Unsupported keystore type: " + keystoreType + ", support only PKCS12, BCFKS, JCEKS, and JKS keystores";
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
            keystore.load(keystoreStream, this.keystorePass.toCharArray());
        } catch (KeyStoreException e) {
            String msg = "Exception getting instance of keystore of type: " + keystoreType;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        } catch (NoSuchAlgorithmException e) {
            String msg = "Exception in loading keystore: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        } catch (CertificateException e) {
            String msg = "Exception in loading keystore: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        } catch (IOException e) {
            String msg = "Exception in loading keystore: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "Exception in creating BCFIPS provider: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        } finally {
            try {
                keystoreStream.close();
            } catch (IOException e) {
                logger.error("Exception in closing keystore: " + e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String locateCryptoKey(final String keyRef) throws KmcKeyClientException {
        try {
            if (keystore.containsAlias(keyRef)) {
                return keyRef;
            } else {
                String msg = "Key of keyRef " + keyRef + " not found in keystore " + keystoreLocation;
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        } catch (KeyStoreException e) {
            String msg = "Exception in locating key " + keyRef + " in keystore " + keystoreLocation + " :" + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<KmcKey> getAllKeys() throws KmcKeyClientException {
        String msg = "To be implemented in KMC V1.1";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getKey(final String keyRef) throws KmcKeyClientException {
        return getKey(keyRef, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getKeyById(final String keyId) throws KmcKeyClientException {
        return getKey(keyId, null);
    }

    @Override
    public final KmcKey getSymmetricKey(final String keyRef) throws KmcKeyClientException {
        return getSymmetricKey(keyRef, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getKey(final String keyRef, final String keyPass) throws KmcKeyClientException {
        try {
            if (!keystore.containsAlias(keyRef)) {
                String msg = "Key of keyRef " + keyRef + " not found in keystore " + keystoreLocation;
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        } catch (KeyStoreException e) {
            String msg = "Exception in retrieving key " + keyRef + " in keystore " + keystoreLocation + " :" + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        try {
            Key key;
            if (keyPass == null) {
                key = keystore.getKey(keyRef, keystorePass.toCharArray());
            } else {
                key = keystore.getKey(keyRef, keyPass.toCharArray());
            }
            return new KmcKey(keyRef, key);
        } catch (UnrecoverableKeyException e) {
            String msg = "Exception in retrieving key " + keyRef + " from keystore " + keystoreLocation + " :" + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        } catch (KeyStoreException e) {
            String msg = "Exception in retrieving key " + keyRef + " from keystore " + keystoreLocation + " :" + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        } catch (NoSuchAlgorithmException e) {
            String msg = "Exception in retrieving key " + keyRef + " from keystore " + keystoreLocation + " :" + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getSymmetricKey(final String keyRef, final String keyPass) throws KmcKeyClientException {
        try {
            if (!keystore.containsAlias(keyRef)) {
                String msg = "Key of keyRef " + keyRef + " not found in keystore " + keystoreLocation;
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        } catch (KeyStoreException e) {
            String msg = "Exception in retrieving key " + keyRef + " in keystore " + keystoreLocation + " :" + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        try {
            Key key;
            if (keyPass == null) {
                key = keystore.getKey(keyRef, keystorePass.toCharArray());
            } else {
                key = keystore.getKey(keyRef, keyPass.toCharArray());
            }
            return new KmcKey(keyRef, key);
        } catch (UnrecoverableKeyException e) {
            String msg = "Exception in retrieving key " + keyRef + " from keystore " + keystoreLocation + " :" + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        } catch (KeyStoreException e) {
            String msg = "Exception in retrieving key " + keyRef + " from keystore " + keystoreLocation + " :" + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        } catch (NoSuchAlgorithmException e) {
            String msg = "Exception in retrieving key " + keyRef + " from keystore " + keystoreLocation + " :" + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getPublicKey(final String keyRef) throws KmcKeyClientException {
        String msg = "To be implemented in KMC V1.1";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getPrivateKey(final String keyRef) throws KmcKeyClientException {
        String msg = "To be implemented in KMC V1.1";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey createEncryptionKey(final String creator, final String keyRef,
            final String algorithm, final int keyLength) throws KmcKeyClientException {
        String msg = "To be implemented in KMC V1.1";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey createIntegrityCheckKey(final String creator, final String keyRef,
            final String algorithm, final int keyLength) throws KmcKeyClientException {
        String msg = "To be implemented in KMC V1.1";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey createAsymmetricKeyPair(final String creator, final String keyRef,
            final String algorithm, final int keyLength) throws KmcKeyClientException {
        String msg = "To be implemented in KMC V1.1";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void destroyKey(final String keyRef) throws KmcKeyClientException {
        String msg = "To be implemented in KMC V1.1";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void destroySymmetricKey(final String keyRef) throws KmcKeyClientException {
        String msg = "To be implemented in KMC V1.1";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void destroyAsymmetricKeyPair(final String keyRef) throws KmcKeyClientException {
        String msg = "To be implemented in KMC V1.1";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

}
