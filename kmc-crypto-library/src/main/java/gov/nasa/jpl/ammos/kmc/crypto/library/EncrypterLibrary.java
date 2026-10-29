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
import java.security.SecureRandom;
import java.security.interfaces.RSAKey;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.Encrypter;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;

/**
 * The Encrypter encrypts data using the key retrieved from KMS or keystore.
 * The encryption algorithm and key length used for encryption are embodied in the key.
 * <p>
 * The results of encryption include the cipher text and the metadata that has all the information needed
 * for decrypting the cipher text.
 * </p>
 * <p>
 * The default transformation for symmetric encryption uses the CBC mode of operation and PKCS5Padding padding.
 * </p>
 *
 */
public class EncrypterLibrary implements Encrypter {
    private static final int BYTE_SIZE = 8;
    private static final int BUFFER_SIZE = 1024;    // buffer size for reading input stream
    private static final int AES_BLOCK_SIZE = 16;   // AES block size is always 128 bits
    private static final int TRIPLE_DES_BLOCK_SIZE = 8;
    private static final int TRIPLE_DES_PARITY_BITS = 24;
    private static final int GCM_IV_LENGTH = 12;    // GCM most efficient with 96 bits IV
    private static final int DEFAULT_GCM_TAG_LENGTH = 16 * BYTE_SIZE;   // GCM tag length 16 bytes in bits

    private final KmcCryptoManager cryptoManager;
    private KeyServiceClient keyClient;
    private final SecureRandom random;

    private final String keyRef;
    private String keyAlgorithm;
    private int keyLength;
    private String transformation;

    private Key key;
    private Cipher ecipher;

    private static final Logger logger = LoggerFactory.getLogger(EncrypterLibrary.class);
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    /**
     * Constructor of the {@link Encrypter} implementation class using the specified keystore or Key Management Service (KMS).
     * During construction of the instance the encryption key as specified in keyRef is retrieved
     * from the keystore or KMS.  The key determines the algorithm and key length that are used for encryption.
     *
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @param keyRef A string for identifying the key, i.e. the name of the key.
     * @throws KmcCryptoException if error in retrieving the key.
     */
    public EncrypterLibrary(final KmcCryptoManager cryptoManager, final String keyRef)
                        throws KmcCryptoException {
        this.cryptoManager = cryptoManager;

        this.random = new SecureRandom();

        if (keyRef == null) {
            String msg = "Null keyRef.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }
        this.keyRef = keyRef;

        keyClient = new KmcKeyServiceClient(this.cryptoManager);
        try {
            this.key = keyClient.getCryptoKey(keyRef, KeyServiceClient.USAGE_MASK_ENCRYPT);
        } catch (KmcCryptoException e) {
            logger.error("EncrypterLibrary: Failed to retrieve key {} from keystore and KMS", keyRef);
            throw e;
        }
        createCipher(key);
        audit.info("EncrypterLibrary: User created local-library Encrypter with key {}", keyRef);
    }

    /**
     * Constructor of the {@link Encrypter} implementation class using the keystore.
     * During the construction the instance, the encryption key as specified in keyRef is retrieved
     * from the specified keystore.  The key determines the algorithm and key length that are used for encryption.
     * <p>
     * The keystorePass or keyPass can be null if they are not used by the keystore.
     * All the keys in the keystore must have the same key password (keyPass) if a key password is used.
     * </p>
     * @param cryptoManager The KmcCryptoManager for accessing the configuration parameters.
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @param keyRef A string for identifying the key, also known as alias.
     * @param keyPass The password of the key to be retrieved.
     * @throws KmcCryptoException if error in retrieving the key.
     */
    public EncrypterLibrary(final KmcCryptoManager cryptoManager,
            final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyRef, final String keyPass)
                    throws KmcCryptoException {
        this.cryptoManager = cryptoManager;
        this.keyRef = keyRef;

        this.random = new SecureRandom();

        KeystoreKeyServiceClient keystoreClient = new KeystoreKeyServiceClient(
                        keystoreLocation, keystorePass, keystoreType);
        try {
            this.key = keystoreClient.getCryptoKey(keyRef, keyPass, KeyServiceClient.USAGE_MASK_ENCRYPT);
        } catch (KmcCryptoException e) {
            audit.info("EncrypterLibrary: Failed to obtain key {} obtained from keystore {}",
                    keyRef, keystoreLocation);
            throw e;
        }
        try {
            createCipher(key);
        } catch (KmcCryptoException e) {
            audit.info("EncrypterLibrary: Failed to create cipher with key {} obtained from keystore {}",
                    keyRef, keystoreLocation);
            throw e;
        }
        audit.info("EncrypterLibrary: User created local-library Encrypter with key {} obtained from keystore {}",
                    keyRef, keystoreLocation);
    }


    /**
     * Creates the cipher with the key.
     * @param key The key for the cipher.
     * @throws KmcCryptoException if error occurs creating the cipher.
     */
    private void createCipher(final Key key) throws KmcCryptoException {
        keyAlgorithm = key.getAlgorithm();
        if (key instanceof RSAKey aKey) {
            keyLength = aKey.getModulus().bitLength();
        } else {
            keyLength = key.getEncoded().length * BYTE_SIZE;
            if ("DESede".equals(keyAlgorithm)) {
                keyLength = keyLength - TRIPLE_DES_PARITY_BITS;
            }
        }

        String algorithmLength = keyAlgorithm + "-" + String.valueOf(keyLength);
        boolean allowedSymmetricEncryption = cryptoManager.isAllowedAlgorithm(
                        algorithmLength, KmcCryptoManager.CFG_ALLOWED_SYMMETRIC_ENCRYPTION_ALGORITHMS);
        boolean allowedAsymmetricEncryption = cryptoManager.isAllowedAlgorithm(
                algorithmLength, KmcCryptoManager.CFG_ALLOWED_ASYMMETRIC_ENCRYPTION_ALGORITHMS);
        if (!allowedSymmetricEncryption && !allowedAsymmetricEncryption) {
            String msg = "The encryption algorithm is not allowed: " + algorithmLength;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, null);
        }
        this.transformation = cryptoManager.getCipherTransformation(keyAlgorithm);
        if (transformation == null) {
            String msg = "No cipher transformation for the algorithm: " + keyAlgorithm;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, null);
        }
        logger.info("Encryption keyRef = {}, algorithm = {}, transformation = {}",
            keyRef, algorithmLength, transformation);

        String cryptoAlgorithm = keyAlgorithm;
        if (transformation.startsWith("AES/GCM")) {
            cryptoAlgorithm = "AESGCM";
        }

        String provider = cryptoManager.getAlgorithmProvider(cryptoAlgorithm);
        if (provider != null) {
            try {
                logger.info("Encryption algorithm {}, provider = {}", cryptoAlgorithm, provider);
                String className = cryptoManager.getProviderClass(provider);
                if (className != null) {
                    logger.debug("Class name for provider {} = {}", provider, className);
                    CryptoLibraryUtilities.addCryptoProvider(className);
                }
            } catch (KmcCryptoException e) {
                throw e;
            }
        }

        try {
            if (provider == null) {
                ecipher = Cipher.getInstance(transformation);
                logger.debug("Cipher created for " + transformation);
            } else {
                ecipher = Cipher.getInstance(transformation, provider);
                logger.debug("Cipher created for {}, provider = {}", transformation, provider);
            }
        } catch (NoSuchProviderException e) {
            String msg = "Invalid crypto algorithm provider " + provider + " for algorithm " + transformation;
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
            logger.error(msg + ": {}", e);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Initializes the AES cipher with the key and IV.
     * @throws KmcCryptoException if error occurs during initialization.
     */
    private void initAEScipher(final String iv) throws KmcCryptoException {
        logger.trace("initAEScipher() iv = {}", iv);
        byte[] initialVector;
        if (iv == null) {
            initialVector = new byte[AES_BLOCK_SIZE];
            random.nextBytes(initialVector);
        } else {
            try {
                initialVector = Base64.getUrlDecoder().decode(iv);
                if (initialVector.length != AES_BLOCK_SIZE) {
                    String msg = "Input IV has size " + initialVector.length
                            + " bytes, expected AES IV size = " + AES_BLOCK_SIZE;
                    logger.error(msg);
                    throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
                }
            } catch (IllegalArgumentException e) {
                String error = "Invalid initial vector (" + iv + "): " + e;
                logger.error(error);
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, error, e);
            }

        }
        IvParameterSpec ivSpec = new IvParameterSpec(initialVector);
        try {
            ecipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        } catch (InvalidKeyException e) {
            String msg = "Exception on key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (InvalidAlgorithmParameterException e) {
            String msg = "Exception on initial vector: " + e;
            logger.error(msg);
            if (iv == null) {
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
            } else {
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, e);
            }
        }
    }

    /**
     * Initializes the AES/GCM cipher with the key, tag length, and IV.
     * @throws KmcCryptoException if error occurs during initialization.
     */
    private void initGCMcipher(final String iv) throws KmcCryptoException {
        logger.trace("initGCMcipher() iv = {}", iv);
        byte[] initialVector;
        if (iv == null) {
            initialVector = new byte[GCM_IV_LENGTH];
            random.nextBytes(initialVector);
        } else {
            try {
                initialVector = Base64.getUrlDecoder().decode(iv);
                if (initialVector.length != GCM_IV_LENGTH) {
                    String msg = "Input IV has size " + initialVector.length
                            + " bytes, expected GCM IV size = " + GCM_IV_LENGTH;
                    logger.error(msg);
                    throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
                }
            } catch (IllegalArgumentException e) {
                String error = "Invalid initial vector (" + iv + "): " + e;
                logger.error(error);
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, error, e);
            }

        }
        int tagLength = DEFAULT_GCM_TAG_LENGTH;
        if (cryptoManager.getMacLength() != -1) {
            tagLength = cryptoManager.getMacLength();
        }
        GCMParameterSpec gcmSpec = new GCMParameterSpec(tagLength, initialVector);
        try {
            ecipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        } catch (InvalidKeyException e) {
            String msg = "Exception on key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (InvalidAlgorithmParameterException e) {
            String msg = "Exception on initial vector: " + e;
            logger.error(msg);
            if (iv == null) {
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
            } else {
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, e);
            }
        }
    }

    /**
     * Initializes the DESede cipher with the key.
     * @throws KmcCryptoException if error occurs during initialization.
     */
    private void init3DEScipher(final String iv) throws KmcCryptoException {
        logger.trace("init3DEScipher() iv = {}", iv);
        byte[] initialVector;
        if (iv == null) {
            initialVector = new byte[TRIPLE_DES_BLOCK_SIZE];
            random.nextBytes(initialVector);
        } else {
            try {
                initialVector = Base64.getUrlDecoder().decode(iv);
                if (initialVector.length != TRIPLE_DES_BLOCK_SIZE) {
                    String msg = "Input IV has size " + initialVector.length
                            + " bytes, expected 3DES IV size = " + TRIPLE_DES_BLOCK_SIZE;
                    logger.error(msg);
                    throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
                }
            } catch (IllegalArgumentException e) {
                String error = "Invalid initial vector (" + iv + "): " + e;
                logger.error(error);
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, error, e);
            }

        }
        IvParameterSpec ivSpec = new IvParameterSpec(initialVector);
        try {
            ecipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        } catch (InvalidKeyException e) {
            String msg = "Exception on key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        } catch (InvalidAlgorithmParameterException e) {
            String msg = "Exception on initial vector: " + e;
            logger.error(msg);
            if (iv == null) {
                throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
            } else {
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, e);
            }
        }
    }

    /**
     * Initializes the RSA cipher with the key.
     * @throws KmcCryptoException if error occurs during initialization.
     */
    private void initRSAcipher() throws KmcCryptoException {
        try {
            ecipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            String msg = "Exception on key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }
    }

    @Override
    public final String encrypt(final InputStream inputStream, final OutputStream outputStream)
            throws KmcCryptoException {
        return encrypt(inputStream, 0, null, outputStream);
    }

    @Override
    public final String encrypt(final InputStream inputStream, final int encryptOffset,
            final String iv, final OutputStream outputStream) throws KmcCryptoException {
        if (inputStream == null) {
            String msg = "Null input stream.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }
        if (outputStream == null) {
            String msg = "Null output stream.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }
        if (encryptOffset < 0 || encryptOffset > KmcCryptoManager.MAX_CRYPTO_SIZE) {
            String msg = "encryptOffset less than 0 or exceeds maximum size of " + KmcCryptoManager.MAX_CRYPTO_SIZE + " bytes.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }
        if (encryptOffset > 0 && !transformation.contains("AES/GCM/")) {
            String msg = "Non-zero encryptOffset can only be used for AES-GCM encryption.";
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
        }

        String algorithm = key.getAlgorithm();
        if ("AES".equals(algorithm)) {
            if (transformation.contains("/GCM/")) {
                initGCMcipher(iv);
            } else {
                initAEScipher(iv);
            }
        } else if ("DESede".equals(algorithm)) {
            init3DEScipher(iv);
        } else if ("RSA".equals(algorithm)) {
            initRSAcipher();
        } else {
            String msg = "Unsupported encryption algorithm: " + algorithm;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, null);
        }
        logger.debug("Cipher initialized for encryption algorithm: " + algorithm);

        if (encryptOffset > 0) {
            processAad(inputStream, outputStream, encryptOffset);
        }
        int totalEncrypted = processEncryption(inputStream, outputStream, encryptOffset);
        logger.info("encrypt() total number of bytes encrypted = {}", totalEncrypted);

        if (encryptOffset > 0) {
            audit.info("EncrypterLibrary: User encrypted {} bytes of data using {} with key length {}, and AAD of {} bytes",
                    totalEncrypted, transformation, keyLength, encryptOffset);
        } else {
            audit.info("EncrypterLibrary: User encrypted {} bytes of data using {} with key length {}",
                totalEncrypted, transformation, keyLength);
        }

        EncryptionMetadata metadata = new EncryptionMetadata(keyRef, keyAlgorithm);
        metadata.addEncryptionAttributes(keyLength, transformation, encryptOffset,
                ecipher.getIV(), cryptoManager.getMacLength());
        logger.info("encrypt() metadata: " + metadata);
        return metadata.toString();
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
                    logger.trace("encryptWithAad() read {} bytes, totaBytes = {}", nData, totalBytes);
                }
            }
            if (totalBytes != encryptOffset) {
                String msg = "Inupt stream has " + totalBytes + " bytes, less than the encryptOffset " + encryptOffset;
                logger.error(msg);
                throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
            }
            outputStream.write(data);
            ecipher.updateAAD(data);
            logger.debug("encryptWithAad() Finished processing {} bytes of AAD", totalBytes);
        } catch (IOException e) {
            audit.info("EncrypterLibrary: Failed to process additional associated data using {} with key length {}",
                transformation, keyLength);
            String msg = "Exception in processing additional associated data: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        }
    }

    private final int processEncryption(final InputStream inputStream, final OutputStream outputStream,
            final int encryptOffset) throws KmcCryptoException {
        CipherOutputStream cos = new CipherOutputStream(outputStream, ecipher);
        int totalBytes = 0;
        byte[] data = new byte[BUFFER_SIZE];
        try {
            // We can't accept empty input because it produces empty output stream.
            // Then we can't distinguish if it's an error or not
            // (CipherOutputStream is empty if error).  But empty input is ok if there is AAD,
            // then the CipherOutputStream will not be empty.
            int available = inputStream.available();
            logger.debug("processEncryption() inputStream available bytes =  " + available);
            if (available == 0) {
                if (encryptOffset == 0) {
                    String msg = "Input stream for encryption cannot be empty.";
                    logger.error(msg);
                    throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
                } else {
                    cos.write(data, 0, 0);
                }
            }
            while (inputStream.available() > 0) {
                if (totalBytes > KmcCryptoManager.MAX_CRYPTO_SIZE) {
                    String msg = "Inupt stream exceeds maximum size of " + KmcCryptoManager.MAX_CRYPTO_SIZE + " bytes.";
                    logger.error(msg);
                    closeStream(cos);
                    closeStream(outputStream);
                    closeStream(inputStream);
                    throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, null);
                }
                int nData = inputStream.read(data);
                if (nData == -1) {
                    break;
                }
                logger.trace("encrypt() read {} bytes.", nData);
                totalBytes = totalBytes + nData;
                cos.write(data, 0, nData);
            }
            logger.debug("processEncryption() encrypted {} bytes of data", totalBytes);
            //cos.flush();
            closeStream(cos);
        } catch (ArrayIndexOutOfBoundsException e) {
            // BouncyCastle exception when input data larger than allowed (e.g. encrypting too much data with RSA keys)
            audit.info("EncrypterLibrary: Failed to encrypt data using {} with key length {}",
                transformation, keyLength);
            String msg = "Exception on encrypting data in i/o stream: " + e;
            logger.error(msg);
            // closeStream(cos); will throw ArrayIndexOutOfBoundsException again
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, msg, e);
        } catch (IOException e) {
            audit.info("EncrypterLibrary: Failed to encrypt data using {} with key length {}",
                transformation, keyLength);
            String msg = "Exception on encrypting data in i/o stream: " + e;
            logger.error(msg);
            closeStream(cos);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, msg, e);
        } finally {
            closeStream(outputStream);
            closeStream(inputStream);
        }
        return totalBytes;
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
            String msg = "Encrypter that uses keystore does not load keys to cache.";
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_MISC_ERROR, msg, null);
        }
        // get the key and cache it
        keyClient.getKmcKey(keyRef);
        logger.info("User retrieved crypto key {} from keystore or KMS", keyRef);
        audit.info("EncrypterLibrary: User retrieved crypto key {} from keystore or KMS", keyRef);
    }

}
