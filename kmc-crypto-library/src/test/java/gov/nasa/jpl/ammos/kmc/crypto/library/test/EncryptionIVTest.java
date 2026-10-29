package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
 */
public class EncryptionIVTest {
    private static String TEST_STRING = "This is the test string for testing the AES IV parameter";
    private static int AES_ENCRYPT_SIZE;

    private static final int AES_BLOCK_SIZE = 16;  // AES block size in bytes
    private static final int GCM_IV_SIZE = 12;     // IV length for AES-GCM encryption
    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final String KEYNAME_HEAD = "kmc/test/";
    private static final String KEYREF_AES256 = KEYNAME_HEAD + "AES256";

    private static final String ATTRIBUTE_VALUE_DELIMITER = ":";
    private static final String ATTRIBUTE_DELIMITER = ",";
    private static final String INITIAL_VECTOR_ATTR = "initialVector";

    private static Random random;
    private static KmcCryptoManager cryptoManager;
    private static String[] args = null;

    @BeforeAll
    public static void setUp() throws KmcCryptoManagerException {
        cryptoManager = new KmcCryptoManager(null);
        random = new Random();

        AES_ENCRYPT_SIZE = (TEST_STRING.length() / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;
    }

    /* --- AES encryption --- */

    @Test
    public final void testAESwithIV()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // create IV
        byte[] initialVector = new byte[AES_BLOCK_SIZE];
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

    // AES encryption expects IV length same as the AES Block Size, i.e. 16 bytes.
    // Test IV length of GCM_IV_SIZE, i.e. 12 bytes
    @Test
    public final void testAESwrongIVsize()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // create a smaller IV than expected
        byte[] initialVector = new byte[GCM_IV_SIZE];
        random.nextBytes(initialVector);
        String iv = Base64.getUrlEncoder().encodeToString(initialVector);

        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        try {
            encrypter.encrypt(bis, 0, iv, eos);
            fail("testAESwrongIVsize() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertEquals("Input IV has size " + String.valueOf(GCM_IV_SIZE) +
                    " bytes, expected AES IV size = " + String.valueOf(AES_BLOCK_SIZE) , e.getMessage());
        }
    }

    // AES-GCM encryption expects IV length of 12 bytes.
    // Test IV length of AES_BLOCK_SIZE, i.e. 16 bytes
    @Test
    public final void testGCMwrongIVsize()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // create a larger IV than expected
        byte[] initialVector = new byte[AES_BLOCK_SIZE];
        random.nextBytes(initialVector);
        String iv = Base64.getUrlEncoder().encodeToString(initialVector);

        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);

        KmcCryptoManager myCryptoManager = new KmcCryptoManager(args);
        myCryptoManager.setCipherTransformation(GCM_TRANSFORMATION);

        Encrypter encrypter = myCryptoManager.createEncrypter(KEYREF_AES256);
        try {
            encrypter.encrypt(bis, 0, iv, eos);
            fail("testGCMwrongIVsize() Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertEquals("Input IV has size " + String.valueOf(AES_BLOCK_SIZE) +
                    " bytes, expected GCM IV size = " + String.valueOf(GCM_IV_SIZE) , e.getMessage());
        }
    }

    @Test
    public final void testAESwithIVofNotUrlSafeBase64()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // https://datatracker.ietf.org/doc/html/rfc4648#page-6
        // create IV not Base64 URL-Safe (i.e. contains + and /)
        byte[] initialVector = new byte[AES_BLOCK_SIZE];
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
            assertEquals("Input IV has size 0 bytes, expected AES IV size = 16", e.getMessage());
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
            assertEquals("Input IV has size 3 bytes, expected AES IV size = 16", e.getMessage());
        }
    }

    @Test
    public final void testDifferentIVs()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // create IV
        byte[] initialVector = new byte[AES_BLOCK_SIZE];
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
    public final void testSameIVs()
            throws KmcCryptoManagerException, KmcCryptoException, UnsupportedEncodingException {
        // generated IV
        InputStream bis = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES256);
        String metadata = encrypter.encrypt(bis, eos);
        byte[] encryptedData = eos.toByteArray();

        // same IV
        String iv = getIV(metadata);
        InputStream bis2 = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        ByteArrayOutputStream eos2 = new ByteArrayOutputStream(AES_ENCRYPT_SIZE);
        encrypter.encrypt(bis2, 0, iv, eos2);
        byte[] encryptedData2 = eos.toByteArray();
        assertTrue(Arrays.equals(encryptedData, encryptedData2), "Ciphertext not the same using the same IV");

        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(TEST_STRING.length());
        Decrypter decrypter = cryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadata);
        String decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(TEST_STRING, decryptedString);
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
