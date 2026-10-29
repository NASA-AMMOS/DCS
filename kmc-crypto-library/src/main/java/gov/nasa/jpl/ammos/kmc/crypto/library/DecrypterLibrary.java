package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.RSAKey;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.Decrypter;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;

/**
 * This class performs decryption based on the provided metadata associated with the cipher text.
 *
 */
public class DecrypterLibrary implements Decrypter {
    private static final int BYTE_SIZE = 8;
    private static final int BUFFER_SIZE = 1024;    // buffer size for reading input stream
    private static final int TRIPLE_DES_PARITY_BITS = 24;
    private static final int DEFAULT_GCM_TAG_LENGTH = 16 * BYTE_SIZE;   // GCM tag length 16 bytes in bits

    private final KmcCryptoManager cryptoManager;

    private KeyServiceClient keyClient;
    private KeyServiceClient keystoreClient;
    private Cipher dcipher;
    private String keyPass;

    private static final Logger logger = LoggerFactory.getLogger(DecrypterLibrary.class);
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    /**
     * Constructor of the {@link Decrypter} implementation that obtains the decryption key
     * from the keystore or Key Management Service (KMS).
     *
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @throws KmcCryptoException if keystore or KMS is not defined, or error occurred in connecting to KMS.
     */
    public DecrypterLibrary(final KmcCryptoManager cryptoManager) throws KmcCryptoException {
        this.cryptoManager = cryptoManager;

        keyClient = new KmcKeyServiceClient(this.cryptoManager);
        String keystore = this.cryptoManager.getCryptoKeystoreLocation();
        String kms = this.cryptoManager.getKeyManagementServiceURI();
        if (keystore != null && kms != null) {
            audit.info("DecrypterLibrary: User created local-library Decrypter that uses keystore at {} and KMS at {}", keystore, kms);
        } else if (keystore != null) {
            audit.info("DecrypterLibrary: User created local-library Decrypter that uses keystore at {}", keystore);
        } else if (kms != null) {
            audit.info("DecrypterLibrary: User created local-library Decrypter that uses KMS at {}", kms);
        } else {
            logger.error(KmcKeyServiceClient.NO_KEY_SOURCE_ERROR_MSG);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR,
                    KmcKeyServiceClient.NO_KEY_SOURCE_ERROR_MSG, null);
        }
    }

    /**
     * Constructor of the {@link Decrypter} implementation that obtains the decryption key
     * from the specified keystore.
     *
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @param keyPass The password of the key to be retrieved.
     * @throws KmcCryptoException if error occurred in loading the keystore.
     */
    public DecrypterLibrary(final KmcCryptoManager cryptoManager,
            final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyPass) throws KmcCryptoException {
        this.cryptoManager = cryptoManager;
        this.keyPass = keyPass;

        keystoreClient = new KeystoreKeyServiceClient(keystoreLocation, keystorePass, keystoreType);
        audit.info("DecrypterLibrary: User created local-library Decrypter that uses keystore in " + keystoreLocation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void decrypt(final InputStream is, final OutputStream os, final String metadata) throws KmcCryptoException {
        if (is == null) {
            String msg = "Null input stream.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }
        if (os == null) {
            String msg = "Null output stream.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }
        if (metadata == null) {
            String msg = "Null metadata.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }

        EncryptionMetadata md = new EncryptionMetadata();
        md.parseMetadata(metadata);
        String keyRef = md.getKeyRef();

        Key key;
        try {
            if (keyClient == null) {
                key = keystoreClient.getCryptoKey(keyRef, keyPass, KeyServiceClient.USAGE_MASK_DECRYPT);
                audit.info("DecrypterLibrary: User obtained key {} from keystore for decryption.", keyRef);
            } else {
                key = keyClient.getCryptoKey(keyRef, KeyServiceClient.USAGE_MASK_DECRYPT);
                audit.info("DecrypterLibrary: User obtained key {} for decryption.", keyRef);
            }
        } catch (KmcCryptoException e) {
            if (keyClient == null) {
                audit.info("DecrypterLibrary: User failed to obtain key {} from keystore for decryption.", keyRef);
            } else {
                audit.info("DecrypterLibrary: User failed to obtain key {} for decryption.", keyRef);
            }
            throw e;
        }
        String keyAlgorithm = md.getCryptoAlgorithm();
        if (!keyAlgorithm.equals(key.getAlgorithm())) {
            String msg = "Key algorithm (" + key.getAlgorithm() + ") does not match metadata cryptoAlgorithm (" + md.getCryptoAlgorithm() + ")";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }
        int keyLength = md.getKeyLength();
        int keySize;
        if (key instanceof RSAKey aKey) {
            keySize = aKey.getModulus().bitLength();
        } else {
            keySize = key.getEncoded().length * BYTE_SIZE;
            if ("DESede".equals(keyAlgorithm)) {
                keySize = keySize - TRIPLE_DES_PARITY_BITS;
            }
        }
        if (keyLength != keySize) {
            String msg = "Key length (" + keySize + ") does not match metadata (" + keyLength + ")";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }

        String transformation = md.getTransformation();
        logger.info("Decrypter: keyRef = " + keyRef + ", algorithm = " + keyAlgorithm + String.valueOf(keyLength)
                    + ", transformation = " + transformation);

        String provider = cryptoManager.getAlgorithmProvider(keyAlgorithm);
        if (provider != null) {
            try {
                logger.info("Decryption algorithm " + keyAlgorithm + " provider = " + provider);
                String className = cryptoManager.getProviderClass(provider);
                if (className != null) {
                    logger.debug("Class name for provider " + provider + " = " + className);
                    CryptoLibraryUtilities.addCryptoProvider(className);
                }
            } catch (KmcCryptoException e) {
                throw e;
            }
        }

        try {
            if (provider == null) {
                dcipher = Cipher.getInstance(transformation);
            } else {
                logger.debug("Provider {} is used for {} decryption.", provider, keyAlgorithm);
                dcipher = Cipher.getInstance(transformation, provider);
            }
        } catch (NoSuchProviderException e) {
            String msg = "Invalid crypto algorithm provider " + provider + " for transformtion " + transformation;
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (NoSuchAlgorithmException e) {
            String msg;
            if (provider == null) {
                msg = "Invalid cipher transformation " + transformation;
            } else {
                msg = "Invalid crypto algorithm provider " + provider + " or transformation " + transformation;
            }
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (NoSuchPaddingException e) {
            String msg = "Invalid padding scheme in " + transformation;
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (Exception e) {
            String msg = "Unexpected exception: " + e;
            logger.error(msg + ": " + e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_MISC_ERROR, msg, e);
        } catch (Throwable t) {
            String msg = "Unexpected error: " + t;
            logger.error(msg + ": " + t);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_MISC_ERROR, msg, t);
        }

        if ("AES".equals(keyAlgorithm) || "DESede".equals(keyAlgorithm)) {
            if (transformation.contains("/GCM/")) {
                initGCMcipher(dcipher, key, md.getInitialVector(), md.getMacLength());
            } else {
                initSymmetricCipher(dcipher, key, md.getInitialVector());
            }
        } else if ("RSA".equals(keyAlgorithm)) {
            initRSAcipher(dcipher, key);
        } else {
            String msg = "Unsupported encryption algorithm: " + keyAlgorithm;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, null);
        }

        int encryptOffset = md.getEncryptOffset();
        if (encryptOffset > 0) {
            processAad(is, os, encryptOffset);
        }
        int totalBytes = processDecryption(is, os, encryptOffset);
        logger.info("decrypt() total number of bytes decrypted = {}", totalBytes);

        if (encryptOffset > 0) {
            audit.info("DecrypterLibrary: User decrypted {} bytes of data using {}, key length {}, and {} bytes of AAD",
                    totalBytes, transformation, keyLength, encryptOffset);
        } else {
            audit.info("DecrypterLibrary: User decrypted {} bytes of data using {}, key length {}",
                    totalBytes, transformation, keyLength);
        }
    }

    private final void processAad(final InputStream inputStream, final OutputStream outputStream,
            final int encryptOffset) throws KmcCryptoException {
        // process the Additional Associated Data (AAD)
        byte[] data = new byte[encryptOffset];
        int totalBytes = 0;
        try {
            while (totalBytes < encryptOffset) {
                int nData = inputStream.read(data, totalBytes, encryptOffset - totalBytes);
                if (nData == -1) {
                    break;
                }
                totalBytes = totalBytes + nData;
                if (logger.isTraceEnabled()) {
                    logger.trace("processAad() read {} bytes, totaBytes = {}", nData, totalBytes);
                }
            }
            if (totalBytes != encryptOffset) {
                String msg = "Inupt stream has " + totalBytes + " bytes, less than the encryptOffset " + encryptOffset;
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
            }
            outputStream.write(data);
            dcipher.updateAAD(data);
            logger.debug("Finished processing {} bytes of AAD", totalBytes);
        } catch (IOException e) {
            audit.info("DecrypterLibrary: Failed to process additional associated data");
            String msg = "Exception in processing additional associated data: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }
    }

    private final int processDecryption(final InputStream is, final OutputStream os,
            final int encryptOffset) throws KmcCryptoException {
        CipherInputStream cis = new CipherInputStream(is, dcipher);
        int totalBytes = 0;
        try {
            byte[] data = new byte[BUFFER_SIZE];
            while (true) {
                if (totalBytes > KmcCryptoManager.MAX_CRYPTO_SIZE) {
                    String msg = "Inupt stream exceeds maximum size of " + KmcCryptoManager.MAX_CRYPTO_SIZE + " bytes.";
                    logger.error(msg);
                    throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
                }
                int nData = cis.read(data);
                if (nData == -1) {
                    break;
                }
                os.write(data, 0, nData);
                totalBytes = totalBytes + nData;
            }
            // If the encrypted data is corrupted, CipherInputStream can't be read and returns -1;
            if (totalBytes == 0 && encryptOffset == 0) {
                String msg = "Invalid input encrypted data.";
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, new IOException());
            }
        } catch (IOException e) {
            String msg = "Exception on reading/writing io stream: " + e;
            logger.error(msg);
            if (e.getMessage().contains("BadPaddingException")) {
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
            } else if (e.getMessage().contains("Tag mismatch")) {
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
            } else if (e.getMessage().contains("AEADBadTagException")) {
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
            } else if (e.getMessage().contains("BadBlockException")) {
                // org.bouncycastle.jcajce.provider.BaseSingleBlockCipher$BadBlockException: unable to decrypt block
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
            } else {
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, e);
            }
        } finally {
            closeStream(cis);
            closeStream(os);
            closeStream(is);
        }
        return totalBytes;
    }

    /**
     * Initializes the AES/DESede cipher with the key and IV.
     * @throws KmcCryptoException if error occurs during initialization.
     */
    private void initSymmetricCipher(final Cipher dcipher, final Key key, final byte[] ivBytes) throws KmcCryptoException {
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        logger.debug("initialVector = " + Base64.getUrlEncoder().encodeToString(ivBytes));

        try {
            dcipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        } catch (InvalidKeyException e) {
            String msg = "Failed to initialize cipher: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } catch (InvalidAlgorithmParameterException e) {
            String msg = "Failed to initialize cipher: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }
    }

    /**
     * Initializes the AES/GCM cipher with the key, tag length, and IV.
     * @throws KmcCryptoException if error occurs during initialization.
     */
    private void initGCMcipher(final Cipher dcipher, final Key key, final byte[] ivBytes, final int macLen)
            throws KmcCryptoException {
        int macLength = macLen;
        if (macLen == -1) {
            macLength = DEFAULT_GCM_TAG_LENGTH;
        }
        GCMParameterSpec gcmSpec = new GCMParameterSpec(macLength, ivBytes);
        try {
            dcipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        } catch (InvalidKeyException e) {
            String msg = "Exception on key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (InvalidAlgorithmParameterException e) {
            String msg = "Exception on initial vector: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }
    }

    /**
     * Initializes the RSA cipher with the key.
     * @throws KmcCryptoException if error occurs during initialization.
     */
    private void initRSAcipher(final Cipher dcipher, final Key key) throws KmcCryptoException {
        try {
            dcipher.init(Cipher.DECRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            String msg = "Exception on key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }
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
            String msg = "Decrypter that uses keystore does not retrieve keys.";
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_MISC_ERROR, msg, null);
        }
        // get the key and cache it
        keyClient.getKmcKey(keyRef);
        logger.info("User loaded crypto key {} from keystore/KMS", keyRef);
        audit.info("DecrypterLibrary: User loaded crypto key {} from keystore/KMS", keyRef);
    }

}
