package gov.nasa.jpl.ammos.kmc.crypto.client.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import gov.nasa.jpl.ammos.kmc.crypto.Decrypter;
import gov.nasa.jpl.ammos.kmc.crypto.Encrypter;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.client.CryptoServiceClient;

import java.security.Security;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
 

/**
 * Unit tests for Authenticated Encryption using the client to access the KMC Crypto Service.
 *
 */
public class AEClientTest {
    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_BLOCK_SIZE = 16;  // AES block size in bytes

    private static final String KEYNAME_HEAD = "kmc/test/";
    private static final String KEYREF_AES128 = KEYNAME_HEAD + "AES128";
    private static final String KEYREF_AES256 = KEYNAME_HEAD + "AES256";

    private static final String TMP_ENCRYPTED_FILE = "/tmp/encrypted-data";
    private static final String TMP_DECRYPTED_FILE = "/tmp/decrypted-data";

    private static KmcCryptoManager cryptoManager;

    @BeforeClass
    public static void setUp() throws KmcCryptoManagerException {
        cryptoManager = new KmcCryptoManager(null);
        //cryptoManager.setUseCryptoService("true");
        cryptoManager.setCipherTransformation(GCM_TRANSFORMATION);
    }


    @Test
    public final void testAuthenticatedEncryptionAES128()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        testAuthenticatedEncryption(KEYREF_AES128);
    }

    @Test
    public final void testAuthenticatedEncryptionAES256()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        testAuthenticatedEncryption(KEYREF_AES256);
    }

    private void testAuthenticatedEncryption(final String keyRef)
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String testString = "Test string for authenticated encryption using keyRef: " + keyRef;
        int encryptedSize = (testString.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;

        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);
        Encrypter encrypter = cryptoManager.createEncrypter(keyRef);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();
        assertTrue(encryptedData.length > 0);

        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(testString.length());
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(testString, decryptedString);
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

        // delete the temporary files
        eis.close();
        eos.close();
        dos.close();
        encryptedFile.delete();
        decryptedFile.delete();
    }

    @Test
    public final void testEncryptBytes() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String keyRef = KEYREF_AES128;
        int dataSize = 1000000;
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

    // comment out the test becuase it is taking too long to send so many bytes
    //@Test
    public final void testExceedMaxBytes() throws KmcCryptoManagerException {
        int dataSize = CryptoServiceClient.MAX_CRYPTO_SERVICE_BYTES + 1;
        byte[] testData = CryptoTestUtils.createTestData(dataSize);
        InputStream is = new ByteArrayInputStream(testData);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES128);
        try {
            encrypter.encrypt(is, os);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertTrue(e.getMessage().contains("exceeds maximum size of " + CryptoServiceClient.MAX_CRYPTO_SERVICE_BYTES));
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
    public final void testNonExistKey() {
        try {
            cryptoManager.createEncrypter("kmc/test/nonExistKey");
            fail("Expected KmcCryptoManagerException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }
    }

    //@Test
    public final void testWrongKeyAlg() throws KmcCryptoManagerException, IOException  {
        String keyRef = "kmc/test/HmacSHA256";
        String testString = "This is a test string for testing wrong key algorithm.";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        File encryptedFile = new File(TMP_ENCRYPTED_FILE);
        OutputStream eos = new FileOutputStream(encryptedFile);
        Encrypter encrypter = cryptoManager.createEncrypter(keyRef);
        try {
            encrypter.encrypt(bis, eos);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
        }

        // delete the temporary file
        bis.close();
        eos.close();
        encryptedFile.delete();
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

        // delete the temporary file
        is.close();
        os.close();
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

        // delete the temporary file
        is.close();
        os.close();
        encryptedFile.delete();
    }

    @Test
    public final void testDecryptBadMetadata()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString = "This is a test string for testing wrong key algorithm.";
        InputStream is = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        OutputStream os = new ByteArrayOutputStream();
        Decrypter decrypter = cryptoManager.createDecrypter();

        String nonExistKey = "metadataType:EncryptionMetadata,"
                + "keyRef:kmc/test/nonExistKeyRef,cipherTransformation:AES/GCM/NoPadding,"
                + "initialVector:of3ypQAN8aF9C63ltxMTOg==,cryptoAlgorithm:AES,keyLength:256";
        try {
            decrypter.decrypt(is, os, nonExistKey);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }

        is.reset();
        String wrongKeyAlg = "metadataType:EncryptionMetadata,"
                + "keyRef:kmc/test/AES256,cipherTransformation:AES/GCM/NoPadding,"
                + "initialVector:of3ypQAN8aF9C63ltxMTOg==,cryptoAlgorithm:XXX,keyLength:256";
        try {
            decrypter.decrypt(is, os, wrongKeyAlg);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }

        is.reset();
        String wrongKeyLength = "metadataType:EncryptionMetadata,"
                + "keyRef:kmc/test/AES256,cipherTransformation:AES/GCM/NoPadding,"
                + "initialVector:of3ypQAN8aF9C63ltxMTOg==,cryptoAlgorithm:AES,keyLength:128";
        try {
            decrypter.decrypt(is, os, wrongKeyLength);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }

        is.reset();
        String wrongPadding = "metadataType:EncryptionMetadata,"
                + "keyRef:kmc/test/AES256,cipherTransformation:AES/GCM/UnknownPadding,"
                + "initialVector:of3ypQAN8aF9C63ltxMTOg==,cryptoAlgorithm:AES,keyLength:256";
        try {
            decrypter.decrypt(is, os, wrongPadding);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
        }
    }

    @Test
    public final void testDecryptInvalidCiphertext()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String ciphertext = "669c";
        String metadata = "metadataType:EncryptionMetadata,"
                + "keyRef:kmc/test/AES256,cipherTransformation:AES/GCM/NoPadding,"
                + "cryptoAlgorithm:AES,keyLength:256,initialVector:AAAAAAAAAAAAAAAB";

        InputStream is = new ByteArrayInputStream(ciphertext.getBytes("UTF-8"));
        OutputStream os = new ByteArrayOutputStream();
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(is, os, metadata);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("javax.crypto.AEADBadTagException: Error finalising cipher data: data too short"));
        }
    }

    /**
     * Encrypt data using Crypto Library and Decrypt the ciphertext with Crypto Service
     */
    // comment out because library and service may use different keystores
    //@Test
    public final void testLibraryEncryptServiceDecrypt()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String testString = "Test using local library to encrypt and remote service to decrypt";
        int encryptedSize = (testString.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);

        // library encrypt
        cryptoManager.setUseCryptoService("false");
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES128);
        String metadata = encrypter.encrypt(bis, eos);
        assertTrue(metadata.contains("AES/GCM"));
        byte[] encryptedData = eos.toByteArray();
        assertTrue(encryptedData.length > 0);

        // library decrypt
        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(testString.length());
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(testString, decryptedString);

        // service decrypt
        cryptoManager.setUseCryptoService("true");
        eis = new ByteArrayInputStream(encryptedData);
        dos = new ByteArrayOutputStream(testString.length());
        decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(testString, decryptedString);
    }

    /**
     * Encrypt data with Crypto Service and Decrypt the ciphertext with Crypto Library
     */
    // comment out because library and service may use different keystores
    //@Test
    public final void testServiceEncryptLibraryDecrypt()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String testString = "Test using remote service to encrypt and local library to decrypt";
        int encryptedSize = (testString.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);

        // service encrypt
        cryptoManager.setUseCryptoService("true");
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES128);
        String metadata = encrypter.encrypt(bis, eos);
        assertTrue(metadata.contains("AES/GCM"));
        byte[] encryptedData = eos.toByteArray();
        assertTrue(encryptedData.length > 0);

        // service decrypt
        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(testString.length());
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(testString, decryptedString);

        // library decrypt
        cryptoManager.setUseCryptoService("false");
        eis = new ByteArrayInputStream(encryptedData);
        dos = new ByteArrayOutputStream(testString.length());
        decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(testString, decryptedString);
        cryptoManager.setUseCryptoService("true");
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
