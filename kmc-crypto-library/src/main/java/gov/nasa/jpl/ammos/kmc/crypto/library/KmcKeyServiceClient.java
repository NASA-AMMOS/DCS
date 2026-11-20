package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.security.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoFailures;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;

/**
 * The KmcKeyServiceClient retrieves keys from either the keystore or KMS.
 * If a key exists in both, the key from the keystore will be returned.
 *
 *
 */
public class KmcKeyServiceClient implements KeyServiceClient {

    public static final String NO_KEY_SOURCE_ERROR_MSG = KmcCryptoFailures.NO_KEY_SOURCE_ERROR_MSG;

    private KmipKeyServiceClient kmipKeyClient = null;
    private KeystoreKeyServiceClient keystoreKeyClient = null;

    private static final Logger logger = LoggerFactory.getLogger(KmcKeyServiceClient.class);

    /**
     * Constructor of KmcKeyServiceClient.
     *
     * @param cryptoManager The cryptoManger whose config parameters will be used to configure the keystore and KMS.
     * @throws KmcCryptoException if keystore and KMS are not defined, or error in connecting to KMS.
     *
     */
    public KmcKeyServiceClient(final KmcCryptoManager cryptoManager) throws KmcCryptoException {
        String kmsURI = cryptoManager.getKeyManagementServiceURI();
        if (kmsURI != null && !kmsURI.contains("fully-qualified-domain-name")) {
            kmipKeyClient = new KmipKeyServiceClient(cryptoManager);
        }

        String keystoreLocation = cryptoManager.getCryptoKeystoreLocation();
        String keystoreType = cryptoManager.getCryptoKeystoreType();
        String keystorePass = cryptoManager.getCryptoKeystorePassword();
        if (keystoreLocation != null) {
            keystoreKeyClient = new KeystoreKeyServiceClient(keystoreLocation, keystorePass, keystoreType);
        }

        if (kmipKeyClient == null && keystoreKeyClient == null) {
            logger.error(NO_KEY_SOURCE_ERROR_MSG);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, NO_KEY_SOURCE_ERROR_MSG, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getKmcKey(final String keyRef) throws KmcCryptoException {
        KmcCryptoException ex = null;

        // try to get the key from keystore first
        if (keystoreKeyClient != null) {
            try {
                return keystoreKeyClient.getKmcKey(keyRef);
            } catch (KmcCryptoException e) {
                if (kmipKeyClient == null) {
                    throw e;
                } else {
                    ex = e;
                }
            }
        }
        if (kmipKeyClient != null) {
            try {
                return kmipKeyClient.getKmcKey(keyRef);
            } catch (KmcCryptoException e) {
                if (keystoreKeyClient == null) {
                    throw e;
                } else {
                    ex = e;
                }
            }
        }

        String msg = "Cryptographic key \"" + keyRef + "\" does not exist in keystore or KMS";
        logger.error(msg);
        throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, ex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final KmcKey getKmcKey(final String keyRef, final String keyPass) throws KmcCryptoException {
        return keystoreKeyClient.getKmcKey(keyRef, keyPass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Key getCryptoKey(final String keyRef, final int keyUsage)
            throws KmcCryptoException {
        KmcCryptoException ex = null;

        // try to get the key from keystore first
        if (keystoreKeyClient != null) {
            try {
                return keystoreKeyClient.getCryptoKey(keyRef, keyUsage);
            } catch (KmcCryptoException e) {
                if (kmipKeyClient == null) {
                    throw e;
                }
            }
        }
        if (kmipKeyClient != null) {
            try {
                return kmipKeyClient.getCryptoKey(keyRef, keyUsage);
            } catch (KmcCryptoException e) {
                if (keystoreKeyClient == null) {
                    throw e;
                } else {
                    ex = e;
                }
            }
        }

        String msg = "Cryptographic key \"" + keyRef + "\" does not exist in keystore or KMS";
        if (ex != null) {
            // kmipKeyClient throws exception if key state is not acceptable for the crypto operation.
            // In this case the error message should not be "does not exist".
            String error = ex.getMessage();
            if (error.contains("key state")) {
                msg = error;
            }
        }
        logger.error(msg);
        throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, ex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Key getCryptoKey(final String keyRef, final String keyPass, final int keyUsage)
            throws KmcCryptoException {
        return keystoreKeyClient.getCryptoKey(keyRef, keyPass, keyUsage);
    }

}
