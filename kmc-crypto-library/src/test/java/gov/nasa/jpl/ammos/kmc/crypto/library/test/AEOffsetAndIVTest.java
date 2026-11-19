package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import gov.nasa.jpl.ammos.kmc.crypto.Decrypter;
import gov.nasa.jpl.ammos.kmc.crypto.Encrypter;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;

/**
 * Unit tests for Encryption Offset and IV parameters.
 *

 *
 *
 */
public class AEOffsetAndIVTest {
    private static String TEST_STRING = "This is the test string for testing Authenticated Encryption offset and IV parameters";
    private static String AAD_STRING = "123456789";
    private static final int AES_BLOCK_SIZE = 16;  // AES block size in bytes
    private static final int GCM_IV_LENGTH = 12;   // bytes
    private static int AAD_SIZE = AAD_STRING.length();
    private static int ENCRYPT_SIZE = TEST_STRING.length();
    private static final int TAG_SIZE = 16;  // Java default authenticated tag size 16 bytes
    private static int AES_ENCRYPT_SIZE;
    private static int AE_OUTPUT_SIZE;

    private static final String CBC_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final String KEYNAME_HEAD = "kmc/test/";
    private static final String KEYREF_AES256 = KEYNAME_HEAD + "AES256";
    private static final String KEYREF_RSA2048 = KEYNAME_HEAD + "RSA2048";

    private static final String ATTRIBUTE_VALUE_DELIMITER = ":";
    private static final String ATTRIBUTE_DELIMITER = ",";
    private static final String INITIAL_VECTOR_ATTR = "initialVector";

    private static Random random;
    private static KmcCryptoManager cryptoManager;

    @BeforeAll
    public static void setUp() throws KmcCryptoManagerException {
        cryptoManager = new KmcCryptoManager(null);

        cryptoManager.setCipherTransformation(GCM_TRANSFORMATION);
        random = new Random();

        AES_ENCRYPT_SIZE = (TEST_STRING.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;
        AE_OUTPUT_SIZE = AAD_STRING.length() + AES_ENCRYPT_SIZE + TAG_SIZE;
    }

    /* --- Authenticated Encryption IV tests --- */

    @Test
    public final void testAESwithIV()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // create IV
        byte[] initialVector = new byte[GCM_IV_LENGTH];
        random.nextBytes(initialVector);
        String iv = Base64.getUrlEncoder().encodeToString(initialVector);

        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, 0, iv, eos);
        byte[] encryptedData = eos.toByteArray();

        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(TEST_STRING.length());
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(TEST_STRING, decryptedString);
    }

    @Test
    public final void testAESwithIVofWrongSize()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // create IV of wrong size
        byte[] initialVector = new byte[AES_BLOCK_SIZE];
        random.nextBytes(initialVector);
        String iv = Base64.getUrlEncoder().encodeToString(initialVector);

        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        try {
            encrypter.encrypt(bis, 0, iv, eos);
            fail("testAESwithIVofWrongSize() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertEquals("Input IV has size " + String.valueOf(AES_BLOCK_SIZE) +
                    " bytes, expected GCM IV size = " + String.valueOf(GCM_IV_LENGTH) , e.getMessage());
        }
    }

    @Test
    public final void testAESwithIVofNotUrlSafeBase64()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // https://datatracker.ietf.org/doc/html/rfc4648#page-6
        // create IV not Base64 URL-Safe (i.e. contains + and /)
        byte[] initialVector = new byte[GCM_IV_LENGTH];
        random.nextBytes(initialVector);
        String iv = new String();
        while (iv.indexOf('+') == 0 && iv.indexOf('/') == 0) {
            iv = Base64.getEncoder().encodeToString(initialVector);
        }

        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        try {
            encrypter.encrypt(bis, 0, iv, eos);
            fail("testAESwithIVofNotUrlSafeBase64() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertEquals("Input IV has size 0 bytes, expected GCM IV size = 12", e.getMessage());
        }
    }

    @Test
    public final void testAESwithIVofNull()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // create IV of value "null"
        String iv = "null";

        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        try {
            encrypter.encrypt(bis, 0, iv, eos);
            fail("testAESwithIVofNull() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertEquals("Input IV has size 3 bytes, expected GCM IV size = 12", e.getMessage());
        }
    }

    @Test
    public final void testDifferentIVs()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // create IV
        byte[] initialVector = new byte[GCM_IV_LENGTH];
        random.nextBytes(initialVector);
        String iv = Base64.getUrlEncoder().encodeToString(initialVector);
        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();

        // different IV
        random.nextBytes(initialVector);
        String iv2 = Base64.getUrlEncoder().encodeToString(initialVector);
        assertFalse(iv2.equals(iv));
        InputStream bis2 = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos2 = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        String metadata2 = encrypter.encrypt(bis2, 0, iv2, eos2);
        byte[] encryptedData2 = eos2.toByteArray();
        assertFalse(Arrays.equals(encryptedData, encryptedData2), "Same ciphertext using different IVs");

        // verify both decrypt correctly
        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(TEST_STRING.length());
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(TEST_STRING, decryptedString);

        InputStream eis2 = new ByteArrayInputStream(encryptedData2);
        ByteArrayOutputStream dos2 = new ByteArrayOutputStream(TEST_STRING.length());
        decrypter.decrypt(eis2, dos2, metadata2);
        String decryptedString2 = new String(dos.toByteArray(), "UTF-8");
        assertEquals(TEST_STRING, decryptedString2);
    }

    @Test
    public final void testGCMreuseIV()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);

        // generated IV
        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        String metadata = encrypter.encrypt(bis, eos);

        // reuse IV
        String iv = getIV(metadata);
        InputStream bis2 = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos2 = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        try {
            encrypter.encrypt(bis2, 0, iv, eos2);
            // comment out because BCFIPS can reuse IV so it does not throw an exception
            //fail("testGCMreuseIV() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof java.security.InvalidAlgorithmParameterException);
            assertTrue(e.getMessage().contains("Cannot reuse iv for GCM encryption") ||   // JCE
                       e.getMessage().contains("cannot reuse nonce for GCM encryption")); // BC
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
    }

    /* --- Authenticated Encryption encryptOffset tests --- */

    @Test
    public final void testAEwithEncryptOffset()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String plaintext = AAD_STRING + TEST_STRING;
        InputStream bis = new ByteArrayInputStream(plaintext.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AE_OUTPUT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, AAD_SIZE, null, eos);
        // verify the metadata
        assertTrue(metadata.contains("encryptOffset:" + Integer.toString(AAD_SIZE)));
        assertTrue(metadata.contains("cipherTransformation:AES/GCM"));
        // verify the cipher text
        byte[] cipherBytes = eos.toByteArray();
        assertEquals(cipherBytes.length, AAD_SIZE + ENCRYPT_SIZE + TAG_SIZE);
        byte[] aadBytes = Arrays.copyOf(cipherBytes, AAD_SIZE);
        String aadString = new String(aadBytes, StandardCharsets.UTF_8);
        assertEquals(AAD_STRING, aadString);

        // decrypt the cipher text
        InputStream eis = new ByteArrayInputStream(cipherBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_SIZE + ENCRYPT_SIZE);
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(plaintext, decryptedString);
    }

    @Test
    public final void testAEwithAADandEmptyPlaintext()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        // create IV
        byte[] initialVector = new byte[GCM_IV_LENGTH];
        random.nextBytes(initialVector);
        String iv = Base64.getUrlEncoder().encodeToString(initialVector);

        String input = AAD_STRING;  // AAD only
        InputStream bis = new ByteArrayInputStream(input.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AE_OUTPUT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, AAD_SIZE, iv, eos);

        // verify the metadata
        assertTrue(metadata.contains("encryptOffset:" + Integer.toString(AAD_SIZE)));
        assertTrue(metadata.contains("cipherTransformation:AES/GCM"));
        // verify the cipher text
        byte[] cipherBytes = eos.toByteArray();
        assertEquals(cipherBytes.length, AAD_SIZE + TAG_SIZE);  // PT_SIZE 0
        byte[] aadBytes = Arrays.copyOf(cipherBytes, AAD_SIZE);
        String aadString = new String(aadBytes, StandardCharsets.UTF_8);
        assertEquals(AAD_STRING, aadString);  // AAD unchanged

        // decrypt the cipher text
        InputStream eis = new ByteArrayInputStream(cipherBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_SIZE);
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(input, decryptedString);  // decryptedString only has AAD
    }

    @Test
    public final void testAEwithLargeInput()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        // create large plaintext with AAD
        int AAD_SIZE = 1233;
        int DATA_SIZE = 1234567;
        byte[] aad = new byte[AAD_SIZE];
        random.nextBytes(aad);
        byte[] data = new byte[DATA_SIZE];
        random.nextBytes(data);
        byte[] plaintext = new byte[AAD_SIZE + DATA_SIZE];
        System.arraycopy(aad, 0, plaintext, 0, AAD_SIZE);
        System.arraycopy(data, 0, plaintext, AAD_SIZE, DATA_SIZE);

        // create IV
        byte[] initialVector = new byte[GCM_IV_LENGTH];
        random.nextBytes(initialVector);
        String iv = Base64.getUrlEncoder().encodeToString(initialVector);

        InputStream bis = new ByteArrayInputStream(plaintext);
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AAD_SIZE + DATA_SIZE + TAG_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, AAD_SIZE, iv, eos);
        // verify the metadata
        assertTrue(metadata.contains("cipherTransformation:AES/GCM"));
        assertTrue(metadata.contains("encryptOffset:" + Integer.toString(AAD_SIZE)));
        assertTrue(metadata.contains(iv));
        // verify the cipher text
        byte[] cipherBytes = eos.toByteArray();
        assertEquals(cipherBytes.length, AAD_SIZE + DATA_SIZE + TAG_SIZE);
        byte[] aadBytes = Arrays.copyOf(cipherBytes, AAD_SIZE);
        assertArrayEquals(aad, aadBytes);

        // decrypt the cipher text
        InputStream eis = new ByteArrayInputStream(cipherBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_SIZE + DATA_SIZE);
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        byte[] decryptedBytes = dos.toByteArray();
        assertEquals(AAD_SIZE + DATA_SIZE, decryptedBytes.length);
        assertArrayEquals(plaintext, decryptedBytes);
    }

    @Test
    public final void testCorruptedAAD()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String plaintext = AAD_STRING + TEST_STRING;
        InputStream bis = new ByteArrayInputStream(plaintext.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AE_OUTPUT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, AAD_SIZE, null, eos);
        byte[] cipherBytes = eos.toByteArray();

        // decrypt with corrupted AAD
        cipherBytes[0]++;
        InputStream eis = new ByteArrayInputStream(cipherBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_SIZE + ENCRYPT_SIZE);
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, metadata);
            fail("testCorruptedAAD() expected exception not received");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("javax.crypto.AEADBadTagException: Tag mismatch") // JCE
                    || e.getMessage().contains("javax.crypto.AEADBadTagException: Error finalising cipher data: mac check in GCM failed")); // BCFIPS
        }
    }

    @Test
    public final void testCorruptedTag()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String plaintext = AAD_STRING + TEST_STRING;
        InputStream bis = new ByteArrayInputStream(plaintext.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AE_OUTPUT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, AAD_SIZE, null, eos);
        byte[] cipherBytes = eos.toByteArray();

        // corrupt the AEAD tag (the tag is in the last 16 bytes of ciphertext)
        cipherBytes[cipherBytes.length - 1]++;

        // decrypt with corrupted AEAD tag
        InputStream eis = new ByteArrayInputStream(cipherBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_SIZE + ENCRYPT_SIZE);
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, metadata);
            fail("testCorruptedTag() expected exception not received");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("javax.crypto.AEADBadTagException: Tag mismatch") // JCE
                    || e.getMessage().contains("javax.crypto.AEADBadTagException: Error finalising cipher data: mac check in GCM failed")); // BCFIPS
        }
    }

    @Test
    public final void testCorruptedCiphertext()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String plaintext = AAD_STRING + TEST_STRING;
        InputStream bis = new ByteArrayInputStream(plaintext.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AE_OUTPUT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, AAD_SIZE, null, eos);
        byte[] cipherBytes = eos.toByteArray();

        // corrupt the first byte of the ciphertext
        cipherBytes[AAD_SIZE + 1]++;

        // decrypt the corrupted ciphertext
        InputStream eis = new ByteArrayInputStream(cipherBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_SIZE + ENCRYPT_SIZE);
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, metadata);
            fail("testCorruptedTag() expected exception not received");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("javax.crypto.AEADBadTagException: Tag mismatch") // JCE
                    || e.getMessage().contains("javax.crypto.AEADBadTagException: Error finalising cipher data: mac check in GCM failed")); // BCFIPS
        }
    }

    @Test
    public final void testCorruptedIV()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // create IV
        byte[] initialVector = new byte[GCM_IV_LENGTH];
        random.nextBytes(initialVector);
        String iv = Base64.getUrlEncoder().encodeToString(initialVector);

        // encrypt with AAD and IV
        String plaintext = AAD_STRING + TEST_STRING;
        InputStream bis = new ByteArrayInputStream(plaintext.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AE_OUTPUT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, AAD_SIZE, iv, eos);
        byte[] cipherBytes = eos.toByteArray();
        assertTrue(metadata.contains(iv));

        // corrupt the IV in the metadata
        initialVector[0]++;
        iv = Base64.getUrlEncoder().encodeToString(initialVector);
        String modifiedMetadata = CryptoTestUtils.modifyMetadataValue(metadata, "initialVector", iv);
        assertTrue(!metadata.equals(modifiedMetadata));

        // decrypt with the corrupted IV
        InputStream eis = new ByteArrayInputStream(cipherBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_SIZE + ENCRYPT_SIZE);
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, modifiedMetadata);
            fail("testCorruptedTag() expected exception not received");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("javax.crypto.AEADBadTagException: Tag mismatch") // JCE
                    || e.getMessage().contains("javax.crypto.AEADBadTagException: Error finalising cipher data: mac check in GCM failed")); // BCFIPS
        }
    }

    @Test
    public final void testLongerAAD()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String plaintext = AAD_STRING + TEST_STRING;
        InputStream bis = new ByteArrayInputStream(plaintext.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AE_OUTPUT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, AAD_SIZE, null, eos);
        byte[] cipherBytes = eos.toByteArray();

        // add one byte to the AAD
        byte[] longerBytes = new byte[cipherBytes.length + 1];
        System.arraycopy(cipherBytes, 0, longerBytes, 1, cipherBytes.length);
        longerBytes[0] = 0x12;  // additional first byte in AAD
        String modifiedMetadata = CryptoTestUtils.modifyMetadataValue(
                metadata, "encryptOffset", String.valueOf(AAD_SIZE + 1));
        assertTrue(!metadata.equals(modifiedMetadata));

        // decrypt the ciphertext with an additional AAD byte
        InputStream eis = new ByteArrayInputStream(longerBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_SIZE + 1 + ENCRYPT_SIZE);
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, modifiedMetadata);
            fail("testCorruptedTag() expected exception not received");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("javax.crypto.AEADBadTagException: Tag mismatch") // JCE
                    || e.getMessage().contains("javax.crypto.AEADBadTagException: Error finalising cipher data: mac check in GCM failed")); // BCFIPS
        }
    }

    @Test
    public final void testLongerCiphertext()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        String plaintext = AAD_STRING + TEST_STRING;
        InputStream bis = new ByteArrayInputStream(plaintext.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AE_OUTPUT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, AAD_SIZE, null, eos);
        byte[] cipherBytes = eos.toByteArray();

        // add one byte to the ciphertext
        byte[] longerBytes = new byte[cipherBytes.length + 1];
        System.arraycopy(cipherBytes, 0, longerBytes, 0, AAD_SIZE);
        longerBytes[AAD_SIZE] = 0x12;  // additional first byte in ciphertext
        System.arraycopy(cipherBytes, AAD_SIZE, longerBytes, AAD_SIZE + 1, cipherBytes.length - AAD_SIZE);

        // decrypt the ciphertext with an additional AAD byte
        InputStream eis = new ByteArrayInputStream(longerBytes);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_SIZE + 1 + ENCRYPT_SIZE);
        Decrypter decrypter = cryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, metadata);
            fail("testCorruptedTag() expected exception not received");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("javax.crypto.AEADBadTagException: Tag mismatch") // JCE
                    || e.getMessage().contains("javax.crypto.AEADBadTagException: Error finalising cipher data: mac check in GCM failed")); // BCFIPS
        }
    }

    @Test
    public final void testInvalidEncryptOffsetSize()
            throws KmcCryptoManagerException, IOException {
        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        // encryptOffset value negative
        try {
            encrypter.encrypt(bis, -10, null, eos);
            fail("testInvalidEncryptOffsetSize() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertTrue(e.getMessage().contains("encryptOffset less than 0 or exceeds maximum size"));
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
        bis.reset();
        // encryptOffset value exceeds limit
        try {
            encrypter.encrypt(bis, KmcCryptoManager.MAX_CRYPTO_SIZE + 1, null, eos);
            fail("testInvalidEncryptOffsetSize() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertTrue(e.getMessage().contains("encryptOffset less than 0 or exceeds maximum size"));
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
        bis.reset();
        // encryptOffset value exceeds the the plaintext size
        try {
            encrypter.encrypt(bis, TEST_STRING.length() + 1, null, eos);
            fail("testInvalidEncryptOffsetSize() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertTrue(e.getMessage().contains("less than the encryptOffset"));
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
    }

    @Test
    public final void testInvalidAlgorithmUsingEncryptOffset()
            throws KmcCryptoManagerException, IOException {
        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);

        cryptoManager.setCipherTransformation(CBC_TRANSFORMATION);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        try {
            encrypter.encrypt(bis, 5, null, eos);
            fail("testInvalidAlgorithmUsingEncryptOffset() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertTrue(e.getMessage().contains("Non-zero encryptOffset can only be used for AES-GCM encryption"));
        } finally {
            cryptoManager.setCipherTransformation(GCM_TRANSFORMATION);
            bis.reset();
        }

        encrypter = cryptoManager.createEncrypter(KEYREF_RSA2048);
        try {
            encrypter.encrypt(bis, 5, null, eos);
            fail("testInvalidAlgorithmUsingEncryptOffset() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertTrue(e.getMessage().contains("Non-zero encryptOffset can only be used for AES-GCM encryption"));
        }
    }

    private String getIV(final String metadata) {
        int i = metadata.indexOf(INITIAL_VECTOR_ATTR);
        i = metadata.indexOf(ATTRIBUTE_VALUE_DELIMITER, i + 1);
        int j = metadata.indexOf(ATTRIBUTE_DELIMITER, i + 1);
        if (i == -1 || j == -1) {
            return null;
        } else {
            return metadata.substring(i + 1, j);
        }
    }
}
