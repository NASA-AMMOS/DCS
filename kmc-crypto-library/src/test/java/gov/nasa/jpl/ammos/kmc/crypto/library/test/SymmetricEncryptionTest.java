package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.junit.BeforeClass;
import org.junit.Test;

import gov.nasa.jpl.ammos.kmc.crypto.Decrypter;
import gov.nasa.jpl.ammos.kmc.crypto.Encrypter;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.library.KeystoreKeyServiceClient;

/**
 * Unit tests for Symmetric Encryption.
 *
 * Before running the tests, the test keys should have been created at KMS by running
 * /ammos/kmc-test/scripts/create-crypto-test-keys.sh
 *
 * @author Kam Tso
 *
 */
public class SymmetricEncryptionTest {
    private static final int AES_BLOCK_SIZE = 16;  // AES block size in bytes

    private static final String KEYNAME_HEAD = "kmc/test/";
    private static final String KEYREF_AES128 = KEYNAME_HEAD + "AES128";
    private static final String KEYREF_AES256 = KEYNAME_HEAD + "AES256";

    private static final String KEYSTORE_PATH = "/ammos/kmc-test/input/";
    private static final String KEYSTORE_NAME = "symmetric-keys.jck";
    private static final String KEYSTORE_PASS = "kmcstorepass";
    private static final String KEYSTORE_KEYPASS = "kmckeypass";

    private static final String TMP_ENCRYPTED_FILE = "/tmp/encrypted-data";
    private static final String TMP_DECRYPTED_FILE = "/tmp/decrypted-data";

    private static KmcCryptoManager cryptoManager;

    @BeforeClass
    public static void setUp() throws KmcCryptoManagerException {
        Security.addProvider(new BouncyCastleFipsProvider());
        cryptoManager = new KmcCryptoManager(null);
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

    @Test
    public final void testSymmetricEncryptionAES128()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        testSymmetricEncryption(KEYREF_AES128);
    }

    @Test
    public final void testSymmetricEncryptionAES256()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        testSymmetricEncryption(KEYREF_AES256);
    }

    private void testSymmetricEncryption(final String keyRef)
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String testString = "This is the test string for keyRef: " + keyRef;
        int encryptedSize = (testString.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;

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
    public final void testRepeatedUse() throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES128);
        String testString = "Test repeatedly using the same Encrypter to encrypt the same string produces different cipher texts.";
        int encryptedSize = (testString.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;

        InputStream bis1 = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos1 = new ByteArrayOutputStream(encryptedSize);
        String metadata1 = encrypter.encrypt(bis1, eos1);
        byte[] encryptedData1 = eos1.toByteArray();

        InputStream bis2 = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos2 = new ByteArrayOutputStream(encryptedSize);
        String metadata2 = encrypter.encrypt(bis2, eos2);
        byte[] encryptedData2 = eos2.toByteArray();

        // IVs are different
        assertTrue(!metadata1.equals(metadata2));
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

    // comment out as KEYSTORE_TYPE_JCEKS is not supported under FIPS
    //@Test
    public final void testAESfromKeystore()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        Encrypter encrypter = cryptoManager.createEncrypter(
                KEYSTORE_PATH + KEYSTORE_NAME, KEYSTORE_PASS,
                KeystoreKeyServiceClient.KEYSTORE_TYPE_JCEKS, KEYREF_AES256, KEYSTORE_KEYPASS);
        Decrypter decrypter = cryptoManager.createDecrypter(
                KEYSTORE_PATH + KEYSTORE_NAME, KEYSTORE_PASS,
                KeystoreKeyServiceClient.KEYSTORE_TYPE_JCEKS, KEYSTORE_KEYPASS);

        String testString = "this is the test string for local AES key.";
        int encryptedSize = (testString.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;
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
        int encryptedSize2 = (testString2.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;
        InputStream bis2 = new ByteArrayInputStream(testString2.getBytes("UTF-8"));
        ByteArrayOutputStream eos2 = new ByteArrayOutputStream(encryptedSize2);
        String metadata2 = encrypter.encrypt(bis2, eos2);
        byte[] encryptedData2 = eos2.toByteArray();
        InputStream eis2 = new ByteArrayInputStream(encryptedData2);
        ByteArrayOutputStream dos2 = new ByteArrayOutputStream(testString2.length());
        decrypter.decrypt(eis2, dos2, metadata2);
        assertEquals(testString2, dos2.toString("UTF-8"));

        // modified encryptedBytes
        byte[] badCipherText = Arrays.copyOf(encryptedData, encryptedData.length);
        badCipherText[0] += 1;
        InputStream eis3 = new ByteArrayInputStream(badCipherText);
        ByteArrayOutputStream dos3 = new ByteArrayOutputStream(encryptedSize);
        decrypter.decrypt(eis3, dos3, metadata);
        assertEquals(dos.toByteArray().length, dos3.toByteArray().length);
        assertFalse(Arrays.equals(dos.toByteArray(), dos3.toByteArray()));
    }

    @Test
    public final void testEncryptText() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String keyRef = KEYREF_AES128;

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

        // delete temporary files
        bis.close();
        eos.close();
        eis.close();
        dos.close();
        encryptedFile.delete();
        decryptedFile.delete();
    }

    @Test
    public final void testEncryptBytes() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String keyRef = KEYREF_AES128;
        int dataSize = 260000 / 8; // BC number of bits per request limited to 262144
        int encryptedSize = (dataSize / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;

        byte[] testData = CryptoTestUtils.createTestData(dataSize);

        InputStream bis = new ByteArrayInputStream(testData);
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);
        Encrypter encrypter = cryptoManager.createEncrypter(keyRef);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();

        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(dataSize);
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);

        byte[] decryptedData = dos.toByteArray();
        assertArrayEquals(testData, decryptedData);
    }

    @Test
    public final void testZeroByte() throws KmcCryptoManagerException {
        byte[] inBytes = new byte[0];
        InputStream is = new ByteArrayInputStream(inBytes);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES128);
        try {
            encrypter.encrypt(is, os);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
    }

    @Test
    public final void testNullKeyRef() throws KmcCryptoManagerException  {
        try {
            cryptoManager.createEncrypter(null);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.NULL_VALUE, e.getErrorCode());
        }
    }

    @Test
    public final void testNonExistKey() throws KmcCryptoManagerException  {
        try {
            cryptoManager.createEncrypter("kmc/test/nonExistKey");
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }
    }

    @Test
    public final void testWrongKeyAlg() throws KmcCryptoManagerException  {
        String keyRef = "kmc/test/HmacSHA256";
        try {
            cryptoManager.createEncrypter(keyRef);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
        }
    }

    @Test
    public final void testEncryptNullInputs()
            throws KmcCryptoManagerException, KmcCryptoException, IOException  {
        // same filename and output in front to ensure the file is created first.
        File encryptedFile = new File(TMP_ENCRYPTED_FILE);
        OutputStream os = new FileOutputStream(encryptedFile);
        InputStream is = new FileInputStream(encryptedFile);
        String keyRef = KEYREF_AES128;

        Encrypter encrypter = cryptoManager.createEncrypter(keyRef);
        try {
            encrypter.encrypt(null, os);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
        try {
            encrypter.encrypt(is, null);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }

        // delete temporary file
        os.close();
        is.close();
        encryptedFile.delete();
    }

    @Test
    public final void testDecryptNullInputs()
            throws KmcCryptoManagerException, KmcCryptoException, IOException  {
        // same filename and output in front to ensure the file is created first.
        File encryptedFile = new File(TMP_ENCRYPTED_FILE);
        OutputStream os = new FileOutputStream(encryptedFile);
        InputStream is = new FileInputStream(encryptedFile);
        String metadata = new String();


        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(null, os, metadata);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
        try {
            decrypter.decrypt(is, null, metadata);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
        try {
            decrypter.decrypt(is, os, null);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }

        // delete temporary file
        os.close();
        is.close();
        encryptedFile.delete();
    }

    @Test
    public final void testDecryptBadMetadata()
            throws KmcCryptoManagerException, KmcCryptoException  {
        String iv = "823BF1B49D2D2BC57FCBF9D129A12669FCB775DB48";
        InputStream is = new ByteArrayInputStream(new byte[]{0});
        OutputStream os = new ByteArrayOutputStream();
        Decrypter decrypter = cryptoManager.createDecrypter();

        String nonExistKey = "metadataType:EncryptionMetadata,"
                + "keyRef:kmc/test/nonExistKeyRef,cipherTransformation:AES/CBC/PKCS5Padding,"
                + "cryptoAlgorithm:AES,keyLength:256,initialVector:" + iv;
        try {
            decrypter.decrypt(is, os, nonExistKey);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }

        String wrongKeyAlg = "metadataType:EncryptionMetadata,"
                + "keyRef:kmc/test/AES256,cipherTransformation:AES/CBC/PKCS5Padding,"
                + "cryptoAlgorithm:XXX,keyLength:256,initialVector:" + iv;
        try {
            decrypter.decrypt(is, os, wrongKeyAlg);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }

        String wrongKeyLength = "metadataType:EncryptionMetadata,"
                + "keyRef:kmc/test/AES256,cipherTransformation:AES/CBC/PKCS5Padding,"
                + "cryptoAlgorithm:AES,keyLength:128,initialVector:" + iv;
        try {
            decrypter.decrypt(is, os, wrongKeyLength);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }

        String wrongPadding = "metadataType:EncryptionMetadata,"
                + "keyRef:kmc/test/AES256,cipherTransformation:AES/CBC/UnknownPadding,"
                + "cryptoAlgorithm:AES,keyLength:256,initialVector:" + iv;
        try {
            decrypter.decrypt(is, os, wrongPadding);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
        }
    }

    // comment out as SunJCE is not supported under FIPS
    //@Test
    public final void testValidProvider() throws KmcCryptoManagerException, KmcCryptoException,
            UnsupportedEncodingException {
        cryptoManager.setAlgorithmProvider("AES", "SunJCE");
        testSymmetricEncryption(KEYREF_AES256);
        cryptoManager.removeAlgorithmProvider("AES");
    }

    @Test
    public final void testValidProviderClass() throws KmcCryptoManagerException, KmcCryptoException,
            UnsupportedEncodingException {
        cryptoManager.setAlgorithmProvider("AES", "SunJCE");
        cryptoManager.setProviderClass("SunJCE", "com.sun.crypto.provider.SunJCE");
        testSymmetricEncryption(KEYREF_AES256);
        cryptoManager.removeAlgorithmProvider("AES");
        cryptoManager.removeProviderClass("SunJCE");
    }

    @Test
    public final void testAlternateProvider() throws KmcCryptoManagerException, KmcCryptoException,
            ClassNotFoundException, InstantiationException, IllegalAccessException,
            UnsupportedEncodingException {
        cryptoManager.setAlgorithmProvider("AES", "BCFIPS");
        cryptoManager.setProviderClass("BCFIPS", "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider");
        testSymmetricEncryption(KEYREF_AES256);
        cryptoManager.removeAlgorithmProvider("AES");
        cryptoManager.removeProviderClass("BCFIPS");
    }

    @Test
    public final void testWrongProvider() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString = "This is the test string.";
        int encryptedSize = (testString.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);

        cryptoManager.setAlgorithmProvider("AES", "SUN");
        //cryptoManager.setProviderClass("SUN", "sun.security.provider.Sun");
        String keyRef = KEYREF_AES128;
        try {
            Encrypter encrypter = cryptoManager.createEncrypter(keyRef);
            encrypter.encrypt(bis, eos);
            fail("Expected KmcCryptoManagerException not received.");
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
        }
        cryptoManager.removeAlgorithmProvider("AES");
        cryptoManager.removeProviderClass("SUN");
    }

    @Test
    public final void testInvalidProvider()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        cryptoManager.setAlgorithmProvider("AES", "SunJCEx");
        String keyRef = KEYREF_AES128;

        try {
            cryptoManager.createEncrypter(keyRef);
            fail("Expected KmcCryptoManagerException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            Throwable cause = e.getCause();
            if (cause.getCause() instanceof KmcCryptoException) {
                // Encrypter using keystore has one more level of cause.
                cause = cause.getCause();
            }
            assertTrue(cause instanceof KmcCryptoException);
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                    ((KmcCryptoException) cause).getErrorCode());
        }
        cryptoManager.removeAlgorithmProvider("AES");
    }

    // comment out as SunJCE is not supported under FIPS
    //@Test
    public final void testSunJCEencryptBCdecrypt()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String testString = "Test using default provider to encrypt and BC provider to decrypt";
        int encryptedSize = (testString.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);

        cryptoManager.setAlgorithmProvider("AES", "SunJCE");
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES128);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();

        cryptoManager.setAlgorithmProvider("AES", "BCFIPS");
        cryptoManager.setProviderClass("BCFIPS", "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider");
        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(testString.length());
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(testString, decryptedString);
        cryptoManager.removeAlgorithmProvider("AES");
        cryptoManager.removeProviderClass("BCFIPS");
    }

}
