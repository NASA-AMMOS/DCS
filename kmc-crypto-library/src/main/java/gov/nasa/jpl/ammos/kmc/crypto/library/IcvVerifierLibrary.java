package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.IcvCreator;
import gov.nasa.jpl.ammos.kmc.crypto.IcvVerifier;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;

/**
 * This class implements {@link IcvVerifier} for verifying input data against the integrity check value.
 *
 */
public class IcvVerifierLibrary implements IcvVerifier {
    private static final int BUFFER_SIZE = 1024;    // buffer size for reading input stream

    private final KmcCryptoManager cryptoManager;
    private String keystoreLocation;
    private String keystorePass;
    private String keystoreType;
    private String keyPass;

    private KeyServiceClient keyClient;

    private static final Logger logger = LoggerFactory.getLogger(IcvVerifierLibrary.class);
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    /**
     * Constructor of the {@link IcvVerifier} implementation that does not need a crypto key
     * (for example, Message Digest algorithms are used)
     * or obtains the cryptographic key from the keystore or Key Management Service (KMS).
     * The metadata, associated with the data to be verified, provides all the information needed
     * to perform the verification of the data.
     *
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     */
    public IcvVerifierLibrary(final KmcCryptoManager cryptoManager) {
        this.cryptoManager = cryptoManager;
        audit.info("IcvVerifierLibrary: User created local-library ICV Verifier that uses keystore at {} or KMS at {}",
                cryptoManager.getCryptoKeystoreLocation(), cryptoManager.getKeyManagementServiceURI());
    }

    /**
     * Constructor of the {@link IcvVerifier} implementation that obtains the
     * cryptographic key from the specified keystore.
     *
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @param keyPass The password of the key to be retrieved.
     */
    public IcvVerifierLibrary(final KmcCryptoManager cryptoManager,
            final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyPass) {
        this.cryptoManager = cryptoManager;
        this.keystoreLocation = keystoreLocation;
        this.keystorePass = keystorePass;
        this.keystoreType = keystoreType;
        this.keyPass = keyPass;
        audit.info("IcvVerifierLibrary: User created local-library ICV Verifier that uses keystore at {}", keystoreLocation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean verifyIntegrityCheckValue(final InputStream inputStream, final String icvMetadata)
            throws KmcCryptoException {
        if (inputStream == null) {
            String msg = "Null input stream.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }
        if (icvMetadata == null) {
            String msg = "Null metadata.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }

        boolean result;
        IntegrityCheckMetadata metadata = new IntegrityCheckMetadata();
        metadata.parseMetadata(icvMetadata);

        String algorithm = metadata.getCryptoAlgorithm();
        logger.info("IcvVerifyLibrary: verify ICV with algorithm {}", algorithm);

        boolean mdAlgorithm = cryptoManager.isAllowedAlgorithm(
                                algorithm, KmcCryptoManager.CFG_ALLOWED_MESSAGE_DIGEST_ALGORITHMS);
        boolean hmacAlgorithm = cryptoManager.isAllowedAlgorithm(
                                algorithm, KmcCryptoManager.CFG_ALLOWED_HMAC_ALGORITHMS);
        boolean cmacAlgorithm = cryptoManager.isAllowedAlgorithm(
                                algorithm, KmcCryptoManager.CFG_ALLOWED_CMAC_ALGORITHMS);
        boolean dsAlgorithm = cryptoManager.isAllowedAlgorithm(
                                algorithm, KmcCryptoManager.CFG_ALLOWED_DIGITAL_SIGNATURE_ALGORITHMS);
        if (mdAlgorithm || hmacAlgorithm || cmacAlgorithm) {
            // macLength can only be used for MD, HMAC, and CMAC.
            int macLength = -1;
            try {
                int icvLength = metadata.getIntegrityCheckValue().length * 8;
                macLength = metadata.getMacLength();
                if (macLength == -1) {
                    // macLength not in metadata, use the ICV length as macLength
                    macLength = icvLength;
                    logger.info("IcvVerifyLibrary: use ICV length {} as macLength", macLength);
                } else if (macLength != icvLength) {
                    String msg = "The MAC length (" + macLength
                                 + ") in the metadata is different from the length of the ICV (" + icvLength + ")";
                    logger.error(msg);
                    throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
                }
                this.cryptoManager.setMacLength(macLength);
            } catch (KmcCryptoManagerException e) {
                String msg = "Invalid MAC length attribute in metadata: " + macLength;
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
            }

            try {
                byte[] icv = generateICV(inputStream, metadata);
                result = Arrays.equals(icv, metadata.getIntegrityCheckValue());
                if (result) {
                    audit.info("IcvVerifyLibrary: User verified data using ICV algorithm " + algorithm);
                } else {
                    audit.info("IcvVerifyLibrary: User failed to verify data using ICV algorithm " + algorithm);
                }
            } catch (KmcCryptoException e) {
                throw e;
            } finally {
                // remove macLength from KmcCryptoManager so that it will not affect future operation
                try {
                    this.cryptoManager.setMacLength(-1);
                } catch (KmcCryptoManagerException e) {
                    String msg = "Failed to remove setMacLength in KmcCryptoManager";
                    logger.error(msg);
                    throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_MISC_ERROR, msg, e);
                }
            }
        } else if (dsAlgorithm) {
            result = verifyDigitalSignature(inputStream, metadata);
            if (result) {
                audit.info("IcvVerifyLibrary: User verified data using digital signature algorithm " + dsAlgorithm);
            } else {
                audit.info("IcvVerifyLibrary: User failed to verify data using digital signature algorithm " + dsAlgorithm);
            }
        } else {
            String msg = "Invalid algorithm for integrity check: " + algorithm;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, null);
        }
        logger.info("ICV verification result = " + result);
        return result;
    }

    /**
     * Generate the ICV of the input data for verification.
     * @param inputStream The data to be verified.
     * @param keyRef The keyRef of the key for HMAC, null for Message Digest.
     * @return The ICV value in array of bytes.
     * @throws KmcCryptoException if error in input data or getting the IcvCreator.
     */
    private byte[] generateICV(final InputStream inputStream, final IntegrityCheckMetadata metadata)
            throws KmcCryptoException {
        IcvCreator icvCreator;

        if (keystoreLocation == null) {
            icvCreator = new IcvCreatorLibrary(cryptoManager, metadata, true);
        } else {
            icvCreator = new IcvCreatorLibrary(cryptoManager,
                    keystoreLocation, keystorePass, keystoreType, keyPass,
                    metadata, true);
        }

        String metadataString = icvCreator.createIntegrityCheckValue(inputStream);
        IntegrityCheckMetadata verifyMetadata = new IntegrityCheckMetadata();
        verifyMetadata.parseMetadata(metadataString);

        return verifyMetadata.getIntegrityCheckValue();
    }

    private boolean verifyDigitalSignature(final InputStream is, final IntegrityCheckMetadata metadata)
            throws KmcCryptoException {
        String keyRef = metadata.getKeyRef();
        Key key;

        if (keyClient == null) {
            if (keystoreLocation == null) {
                keyClient = new KmcKeyServiceClient(cryptoManager);
            } else {
                keyClient = new KeystoreKeyServiceClient(keystoreLocation, keystorePass, keystoreType);

            }
        }
        try {
            if (keystoreLocation == null) {
                key = keyClient.getCryptoKey(keyRef, KeyServiceClient.USAGE_MASK_VERIFY);
                audit.info("IcvVerifierLibrary: User obtained key {} for ICV verification.", keyRef);
            } else {
                key = keyClient.getCryptoKey(keyRef, keyPass, KeyServiceClient.USAGE_MASK_VERIFY);
                audit.info("IcvVerifierLibrary: User obtained key {} from keystore for ICV verification.", keyRef);
            }
        } catch (KmcCryptoException e) {
            if (keystoreLocation == null) {
                audit.info("IcvVerifyLibrary: User failed to obtain key {} for ICV verification.", keyRef);
                logger.error("verifyDigitalSignature() failed to obtain key {} from keystore or KMS", keyRef);
            } else {
                logger.error("verifyDigitalSignature() failed to obtain key {} from keystore {}", keyRef, keystoreLocation);
                audit.info("IcvVerifyLibrary: User failed to obtain key {} from keystore for ICV verification.", keyRef);
            }
            throw e;
        }
        PublicKey publicKey;
        if (key instanceof PublicKey publicKey1) {
            publicKey = publicKey1;
        }  else {
            String msg = "The retrieved key (keyRef = " + keyRef + ") is not a RSA public key.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }

        String algorithm = metadata.getCryptoAlgorithm();
        logger.info("keyRef = " + keyRef + ", algorithm = " + algorithm);

        String provider = cryptoManager.getAlgorithmProvider(algorithm);
        if (provider != null) {
            try {
                logger.info("Encryption algorithm " + algorithm + " provider = " + provider);
                String className = cryptoManager.getProviderClass(provider);
                if (className != null) {
                    logger.debug("Class name for provider " + provider + " = " + className);
                    CryptoLibraryUtilities.addCryptoProvider(className);
                }
            } catch (KmcCryptoException e) {
                throw e;
            }
        }

        Signature digitalSignature = createDigitalSignature(publicKey, algorithm, provider);

        int totalBytes = 0;
        byte[] data = new byte[BUFFER_SIZE];
        try {
            while (true) {
                if (totalBytes > KmcCryptoManager.MAX_CRYPTO_SIZE) {
                    String msg = "Inupt stream exceeds maximum size of " + KmcCryptoManager.MAX_CRYPTO_SIZE + " bytes.";
                    logger.error(msg);
                    throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
                }
                int nData = is.read(data);
                if (nData == -1) {
                    break;
                }
                digitalSignature.update(data, 0, nData);
                totalBytes = totalBytes + nData;
            }
            byte[] sigBytes = metadata.getIntegrityCheckValue();
            boolean result = digitalSignature.verify(sigBytes);
            if (result) {
                audit.info("IcvVerifyLibrary: User verified " + totalBytes + " bytes of data using ICV algorithm " + algorithm);
            } else {
                audit.info("IcvVerifyLibrary: User failed to verify data using ICV algorithm " + algorithm);
            }
            return result;
        } catch (SignatureException e) {
            audit.info("IcvVerifyLibrary: User failed to verify data using ICV algorithm " + algorithm);
            String msg = "Exception in verifying the data: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (IOException e) {
            audit.info("IcvVerifyLibrary: User failed to verify data using ICV algorithm " + algorithm);
            String msg = "Exception in reading/writing io stream: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } finally {
            closeStream(is);
        }
    }

    private Signature createDigitalSignature(final PublicKey key, final String algorithm, final String provider) throws KmcCryptoException {
        Signature signature;
        try {
            if (provider == null) {
                signature = Signature.getInstance(algorithm);
            } else {
                logger.info("Provider " + provider + " is used for Digital Signature algorithm " + algorithm);
                signature = Signature.getInstance(algorithm, provider);
            }
        } catch (NoSuchAlgorithmException e) {
            String msg;
            if (provider == null) {
                msg = "Invalid algorithm " + algorithm + " for Digital Signature";
            } else {
                msg = "Invalid provider " + provider + " or algorithm " + algorithm + " for Digital Signature";
            }
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "Invalid provider " + provider + " for Digital Signature algorithm " + algorithm;
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }
        try {
            signature.initVerify(key);
        } catch (InvalidKeyException e) {
            String msg = "Exception in initializing signature";
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
        return signature;
    }

    private void closeStream(final Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e) {
            logger.error("Failed to close a stream in the finally block: " + e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadCryptoKey(final String keyRef) throws KmcCryptoException {
        if (keyClient == null) {
            String msg = "IcvVerifier that uses keystore does not retrieve keys.";
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_MISC_ERROR, msg, null);
        }
        // get the key and cache it
        keyClient.getKmcKey(keyRef);
        logger.info("IcvVerifierLibrary: User loaded crypto key {}", keyRef);
        audit.info("IcvVerifierLibrary: User loaded crypto key {}", keyRef);
    }

}
