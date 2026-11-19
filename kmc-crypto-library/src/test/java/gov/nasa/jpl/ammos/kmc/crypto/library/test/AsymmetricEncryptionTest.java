package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.util.Arrays;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import gov.nasa.jpl.ammos.kmc.crypto.Decrypter;
import gov.nasa.jpl.ammos.kmc.crypto.Encrypter;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.library.KeystoreKeyServiceClient;

/**
 * Unit tests for Asymmetric Encryption.
 *
 */
public class AsymmetricEncryptionTest {
    private static final String KEYNAME_HEAD = "kmc/test/";
    private static final String KEYREF_RSA2048 = KEYNAME_HEAD + "RSA2048";
    private static final String KEYREF_RSA3072 = KEYNAME_HEAD + "RSA3072";
    private static final String KEYREF_RSA4096 = KEYNAME_HEAD + "RSA4096";

    private static final String KEYSTORE_PATH = "/ammos/kmc-test/input/";
    private static final String KEYSTORE_NAME = "asymmetric-keys.jks";
    private static final String KEYSTORE_PASS = "kmcstorepass";
    private static final String KEYSTORE_KEYPASS = "kmckeypass";

    private static final String TMP_ENCRYPTED_FILE = "/tmp/encrypted-data";
    private static final String TMP_DECRYPTED_FILE = "/tmp/decrypted-data";

    private static KmcCryptoManager cryptoManager;
    // use a different KmcCryptoManager, otherwise testCorruptedCipherText() will fail
    private static KmcCryptoManager cryptoManagerChangedCipher;

    @BeforeAll
    public static void setUp() throws KmcCryptoManagerException {
        Security.addProvider(new BouncyCastleFipsProvider());
        cryptoManager = new KmcCryptoManager(null);
        cryptoManagerChangedCipher = new KmcCryptoManager(null);
    }

    @Test
    public final void testAsymmetricEncryptionRSA2048()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        testAsymmetricEncryption(KEYREF_RSA2048);
    }

    @Test
    public final void testAsymmetricEncryptionRSA3072()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        testAsymmetricEncryption(KEYREF_RSA3072);
    }

    @Test
    public final void testAsymmetricEncryptionRSA4096()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        testAsymmetricEncryption(KEYREF_RSA4096);
    }

    private void testAsymmetricEncryption(final String keyRef)
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String testString = "This is the test string for keyRef: " + keyRef;

        // the encrypted output size is always the key size in bytes.
        int index = keyRef.lastIndexOf("RSA");
        String keySize = keyRef.substring(index + 3);
        int encryptedSize = Integer.parseInt(keySize) / 8;

        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);
        Encrypter encrypter = cryptoManager.createEncrypter(keyRef);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();

        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(testString.length());
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(testString, decryptedString);
    }

    @Test
    public final void testRepeatedUse()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_RSA2048);
        String testString = "Test repeatedly using the same Encrypter to encrypt the same string produces different cipher texts.";
        int encryptedSize = 2048 / 8;

        InputStream bis1 = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos1 = new ByteArrayOutputStream(encryptedSize);
        String metadata1 = encrypter.encrypt(bis1, eos1);
        byte[] encryptedData1 = eos1.toByteArray();

        InputStream bis2 = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos2 = new ByteArrayOutputStream(encryptedSize);
        String metadata2 = encrypter.encrypt(bis2, eos2);
        byte[] encryptedData2 = eos2.toByteArray();

        assertTrue(metadata1.equals(metadata2));
        assertTrue(!Arrays.equals(encryptedData1, encryptedData2));

        Decrypter decrypter = cryptoManager.createDecrypter();
        InputStream eis1 = new ByteArrayInputStream(encryptedData1);
        InputStream eis2 = new ByteArrayInputStream(encryptedData2);
        ByteArrayOutputStream dos1 = new ByteArrayOutputStream(testString.length());
        ByteArrayOutputStream dos2 = new ByteArrayOutputStream(testString.length());
        decrypter.decrypt(eis1, dos1, metadata1);
        decrypter.decrypt(eis2, dos2, metadata2);
        String decryptedString1 = dos1.toString("UTF-8");
        String decryptedString2 = dos2.toString("UTF-8");
        assertEquals(testString, decryptedString1);
        assertEquals(testString, decryptedString2);
    }

    // comment out as KEYSTORE_TYPE_JKS is not supported under FIPS
    //@Test
    public final void testKeyfromKeystore()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        Encrypter encrypter = cryptoManager.createEncrypter(
                KEYSTORE_PATH + KEYSTORE_NAME, KEYSTORE_PASS,
                KeystoreKeyServiceClient.KEYSTORE_TYPE_JKS, KEYREF_RSA2048, KEYSTORE_KEYPASS);
        Decrypter decrypter = cryptoManager.createDecrypter(
                KEYSTORE_PATH + KEYSTORE_NAME, KEYSTORE_PASS,
                KeystoreKeyServiceClient.KEYSTORE_TYPE_JKS, KEYSTORE_KEYPASS);

        String testString = "This is the test string for local key.";
        int encryptedSize = 2048 / 8;
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();
        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(testString.length());
        decrypter.decrypt(eis, dos, metadata);
        assertEquals(testString, dos.toString("UTF-8"));

        // use the Encrypter and Decrypter again
        String testString2 = "This is another string for local key.";
        InputStream bis2 = new ByteArrayInputStream(testString2.getBytes("UTF-8"));
        ByteArrayOutputStream eos2 = new ByteArrayOutputStream(encryptedSize);
        String metadata2 = encrypter.encrypt(bis2, eos2);
        byte[] encryptedData2 = eos2.toByteArray();
        InputStream eis2 = new ByteArrayInputStream(encryptedData2);
        ByteArrayOutputStream dos2 = new ByteArrayOutputStream(testString2.length());
        decrypter.decrypt(eis2, dos2, metadata2);
        assertEquals(testString2, dos2.toString("UTF-8"));
    }

    @Test
    public final void testEncryptText() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String keyRef = KEYREF_RSA2048;

        String testString = "This is a test string for using io streams.";

        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        File encryptedFile = new File(TMP_ENCRYPTED_FILE);
        OutputStream eos = new FileOutputStream(encryptedFile);
        Encrypter encrypter = cryptoManager.createEncrypter(keyRef);
        String metadata = encrypter.encrypt(bis, eos);

        InputStream eis = new FileInputStream(encryptedFile);
        File decryptedFile = new File(TMP_DECRYPTED_FILE);
        OutputStream dos = new FileOutputStream(decryptedFile);
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);

        byte[] decryptedData = readFile(decryptedFile);
        String decryptedString = new String(decryptedData, "UTF-8");
        assertEquals(testString, decryptedString);

        // delete the temporary files
        bis.close();
        eos.close();
        eis.close();
        dos.close();
        encryptedFile.delete();
        decryptedFile.delete();
    }

    @Test
    public final void testEncryptMaxSizeWithPKCS1()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        // PKCS1: keyLength / 8 - 11
        cryptoManagerChangedCipher.setCipherTransformation("RSA/ECB/PKCS1Padding");
        encryptBytes(KEYREF_RSA2048, 245);
        encryptBytes(KEYREF_RSA3072, 373);
        encryptBytes(KEYREF_RSA4096, 501);
    }

    @Test
    public final void testEncryptExceedMaxSizeWithPKCS1()
            throws KmcCryptoManagerException, IOException {
        // PKCS1: keyLength / 8 - 11
        cryptoManagerChangedCipher.setCipherTransformation("RSA/ECB/PKCS1Padding");
        encryptExpectFail(KEYREF_RSA2048, 246);
        encryptExpectFail(KEYREF_RSA3072, 374);
        encryptExpectFail(KEYREF_RSA4096, 502);
    }

    @Test
    public final void testEncryptMaxDataSizeWithOAEP1()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        // OAEP:  keyLength / 8 - 2 - 2 * hash size (hash size = 20)
        cryptoManagerChangedCipher.setCipherTransformation("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
        encryptBytes(KEYREF_RSA2048, 214);
        encryptBytes(KEYREF_RSA3072, 342);
        encryptBytes(KEYREF_RSA4096, 470);
    }

    @Test
    public final void testEncryptExceedMaxSizeWithOAEP1()
            throws KmcCryptoManagerException, IOException {
        // OAEP:  keyLength / 8 - 2 - 2 * hash size (hash size = 20)
        cryptoManagerChangedCipher.setCipherTransformation("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
        encryptExpectFail(KEYREF_RSA2048, 215);
        encryptExpectFail(KEYREF_RSA3072, 343);
        encryptExpectFail(KEYREF_RSA4096, 471);
    }

    @Test
    public final void testEncryptMaxDataSizeWithOAEP256()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        // OAEP:  keyLength / 8 - 2 - 2 * hash size (hash size = 32)
        cryptoManagerChangedCipher.setCipherTransformation("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        encryptBytes(KEYREF_RSA2048, 190);
        encryptBytes(KEYREF_RSA3072, 318);
        encryptBytes(KEYREF_RSA4096, 446);
    }

    @Test
    public final void testEncryptExceedMaxSizeWithOAEP256()
            throws KmcCryptoManagerException, IOException {
        // OAEP:  keyLength / 8 - 2 - 2 * hash size (hash size = 32)
        cryptoManagerChangedCipher.setCipherTransformation("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        encryptExpectFail(KEYREF_RSA2048, 191);
        encryptExpectFail(KEYREF_RSA3072, 319);
        encryptExpectFail(KEYREF_RSA4096, 447);
    }

    private void encryptBytes(final String keyRef, final int dataSize)
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        byte[] testData = CryptoTestUtils.createTestData(dataSize);

        InputStream bis = new ByteArrayInputStream(testData);
        ByteArrayOutputStream eos = new ByteArrayOutputStream(128); // 128 is initial size, it can grow.
        Encrypter encrypter = cryptoManagerChangedCipher.createEncrypter(keyRef);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();
        if (encryptedData.length == 0) {
            throw new KmcCryptoException(KmcCryptoErrorCode.INVALID_INPUT_VALUE, "Data size exceeded the maxinum.", null);
        }

        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(dataSize);
        Decrypter decrypter = cryptoManagerChangedCipher.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);

        byte[] decryptedData = dos.toByteArray();
        assertArrayEquals(testData, decryptedData);
    }

    private void encryptExpectFail(final String keyRef, final int dataSize)
            throws KmcCryptoManagerException, IOException {
        try {
            encryptBytes(keyRef, dataSize);
            fail("Failed to throw KmcCryptoException for exceeding max data size.");
        } catch (KmcCryptoException e) {
            KmcCryptoErrorCode errorCode = e.getErrorCode();
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, errorCode);
        }
    }

    @Test
    public final void testCorruptedCipherText()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString = "This is the test string for corrupted cipher text.";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(256);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_RSA2048);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();

        // modify encryptedBytes
        byte[] badCipherText = Arrays.copyOf(encryptedData, encryptedData.length);
        badCipherText[0] += 1;
        InputStream eis = new ByteArrayInputStream(badCipherText);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(256);
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, metadata);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(IOException.class, e.getCause().getClass());
            assertTrue(e.getMessage().contains("BadPaddingException") ||
                       e.getMessage().contains("BadBlockException"));
        }
    }

    @Test
    public final void testCorruptedMetadata()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString = "This is the test string for corrupted metadata.";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(256);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_RSA2048);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();

        // modify metadata
        String badMetadata = metadata.substring(0, 30)
                + "bad"
                + metadata.substring(31);

        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(256);
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, badMetadata);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            // could be any error depending on the attribute being modified.
            assertTrue(e instanceof KmcCryptoException);
        }
    }

    @Test
    public final void testDecryptWithDifferentTransformation()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        cryptoManager.setCipherTransformation("RSA/ECB/PKCS1Padding");
        String testString = "Test string for decrypting with a different transformation.";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(256);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_RSA2048);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();

        // modify metadata with a different transformation
        int fromIndex = metadata.indexOf("cipherTransformation");
        int toIndex = metadata.indexOf(',', fromIndex);
        String badMetadata = metadata.substring(0, fromIndex)
                + "cipherTransformation:RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
                + metadata.substring(toIndex);

        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(256);
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, badMetadata);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(IOException.class, e.getCause().getClass());
            //assertTrue(e.getMessage().contains("BadPaddingException"));
        }
        cryptoManager.setCipherTransformation(KmcCryptoManager.DEFAULT_RSA_TRANSFORMATION);
    }

    @Test
    public final void testZeroByte() throws KmcCryptoManagerException {
        byte[] inBytes = new byte[0];
        InputStream is = new ByteArrayInputStream(inBytes);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_RSA2048);
        try {
            encrypter.encrypt(is, os);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
    }

    @Test
    public final void testValidProvider() throws KmcCryptoManagerException, KmcCryptoException {
        cryptoManager.setAlgorithmProvider("RSA", "SunJCE");
        cryptoManager.setProviderClass("SunJCE", "com.sun.crypto.provider.SunJCE");
        String keyRef = KEYREF_RSA2048;

        byte[] inBytes = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        InputStream is = new ByteArrayInputStream(inBytes);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Encrypter encrypter = cryptoManager.createEncrypter(keyRef);
        String metadata = encrypter.encrypt(is, os);
        byte[] encryptedBytes = os.toByteArray();

        InputStream eis = new ByteArrayInputStream(encryptedBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream();
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);

        byte[] decryptedBytes = dos.toByteArray();
        assertArrayEquals(inBytes, decryptedBytes);

        cryptoManager.removeAlgorithmProvider("RSA");
        cryptoManager.removeProviderClass("SunJCE");
    }

    @Test
    public final void testWrongProvider() throws KmcCryptoManagerException {
        cryptoManager.setAlgorithmProvider("RSA", "SUN");
        cryptoManager.setProviderClass("SUN", "sun.security.provider.Sun");
        String keyRef = KEYREF_RSA2048;
        try {
            cryptoManager.createEncrypter(keyRef);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            Throwable cause2 = e.getCause();
            if (cause2.getCause() instanceof KmcCryptoException) {
                // Encrypter using keystore has one more level of cause.
                cause2 = cause2.getCause();
            }
            assertTrue(cause2 instanceof KmcCryptoException);
            KmcCryptoException cause = (KmcCryptoException) cause2;
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, cause.getErrorCode());
            assertTrue(cause.getCause() instanceof java.security.NoSuchAlgorithmException);
        } finally {
            cryptoManager.removeAlgorithmProvider("RSA");
            cryptoManager.removeProviderClass("SUN");
        }
    }

    @Test
    public final void testInvalidProvider() throws KmcCryptoManagerException {
        cryptoManager.setAlgorithmProvider("RSA", "SunJCEx");
        String keyRef = KEYREF_RSA2048;
        try {
            cryptoManager.createEncrypter(keyRef);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            Throwable cause2 = e.getCause();
            if (cause2.getCause() instanceof KmcCryptoException) {
                // Encrypter using keystore has one more level of cause.
                cause2 = cause2.getCause();
            }
            assertTrue(cause2 instanceof KmcCryptoException);
            KmcCryptoException cause = (KmcCryptoException) cause2;
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, cause.getErrorCode());
        } finally {
            cryptoManager.removeAlgorithmProvider("RSA");
        }
    }

    private byte[] readFile(final File file) throws IOException {
        int size = (int) file.length();
        InputStream fis = new FileInputStream(file);
        byte[] data = new byte[size];
        int totalRead = 0;
        while (totalRead < size) {
            int nData = fis.read(data, totalRead, size - totalRead);
            totalRead = totalRead + nData;
        }
        fis.close();
        return data;
    }

}
