package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.util.Base64;
import java.util.Random;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import gov.nasa.jpl.ammos.kmc.crypto.Decrypter;
import gov.nasa.jpl.ammos.kmc.crypto.Encrypter;
import gov.nasa.jpl.ammos.kmc.crypto.IcvCreator;
import gov.nasa.jpl.ammos.kmc.crypto.IcvVerifier;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;

/**
 * Unit tests for Truncated MAC or Tag.
 *
 *
 */
public class TruncatedMacTest {
    private static final String HMAC_KEYREF = "kmc/test/HmacSHA512";
    private static final String CMAC_KEYREF = "kmc/test/AES256";
    private static final String GMAC_KEYREF = "kmc/test/AES256";
    private static final String DS_KEYREF = "kmc/test/RSA4096";

    private static final String MD_ALGORITHM = "SHA-512";  // Message Digest does not use key
    private static final String DS_ALGORITHM = "SHA512withRSA"; // Digital Signature algorithm

    private static final String ICV_ATTR = "integrityCheckValue:";
    private static final int ICV_ATTR_LENGTH = ICV_ATTR.length();
    private static final String MAC_LENGTH_ATTR = "macLength:";
    private static final int MAC_LENGTH_ATTR_LENGTH = MAC_LENGTH_ATTR.length();

    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // bytes

    private static final String TEST_STRING = "This is the string for testing truncated MAC";
    private static final String AAD_STRING = "123456789";
    private static final int AAD_LENGTH = AAD_STRING.length();
    private static final String AAD_TEST_STRING = AAD_STRING + TEST_STRING;
    private static final int AAD_TEST_LENGTH = AAD_TEST_STRING.length();

    private static String iv;
    private static final Random random = new Random();
    private static InputStream plaintext;
    private static InputStream aadPlaintext;
    private static KmcCryptoManager cryptoManager;
    private static String[] args;

    @BeforeAll
    public static void setUp() throws KmcCryptoManagerException, UnsupportedEncodingException {
        cryptoManager = new KmcCryptoManager(null);

        plaintext = new ByteArrayInputStream(TEST_STRING.getBytes("UTF-8"));
        aadPlaintext = new ByteArrayInputStream(AAD_TEST_STRING.getBytes("UTF-8"));

        // create IV
        byte[] initialVector = new byte[GCM_IV_LENGTH];
        random.nextBytes(initialVector);
        iv = Base64.getUrlEncoder().encodeToString(initialVector);
    }

    @BeforeEach
    public void reset() throws KmcCryptoManagerException, IOException {
        cryptoManager.setMacLength(-1);
        plaintext.reset();
        aadPlaintext.reset();
    }

    // removes the macLength attribute from the metadata
    private static String removeMacLength(final String metadata) {
        int i = metadata.indexOf(MAC_LENGTH_ATTR);
        if (i == -1) {
            // MAC_LENGTH_ATTR does not exist
            return metadata;
        }
        String removed = metadata.substring(0, i);
        int j = metadata.indexOf(",", i + MAC_LENGTH_ATTR_LENGTH);
        if (j != -1) {
            removed = removed + metadata.substring(j+1, metadata.length());
        }
        return removed;
    }

    // returns true if the truncated MAC is the left-most bytes of the full MAC
    private static boolean verifyTruncatedMac(final String truncatedMatadata, final String fullMatadata) {
        int i = truncatedMatadata.indexOf(ICV_ATTR);
        int j = truncatedMatadata.indexOf(",", ICV_ATTR_LENGTH);
        String truncatedMac;
        if (j == -1) {
            truncatedMac = truncatedMatadata.substring(i, truncatedMatadata.length());
        } else {
            truncatedMac = truncatedMatadata.substring(i, j);
        }

        i = fullMatadata.indexOf(ICV_ATTR);
        j = fullMatadata.indexOf(",", ICV_ATTR_LENGTH);
        String fullMac;
        if (j == -1) {
            fullMac = fullMatadata.substring(i, fullMatadata.length());
        } else {
            fullMac = fullMatadata.substring(i, j);
        }
        if (fullMac.length() >= truncatedMac.length()) {
            // remove the base64 padding bytes
            truncatedMac = truncatedMac.substring(0, truncatedMac.length() - 3);
            if (fullMac.startsWith(truncatedMac)) {
                return true;
            } else {
                System.out.println("ERROR: Full MAC = " + fullMac
                    + ", Truncated MAC = " + truncatedMac);
                return false;
            }
        } else {
            System.out.println("ERROR: Full MAC length = " + fullMac.length()
                + ", Truncated MAC length = " + truncatedMac.length());
            return false;
        }
    }

    @Test
    public void testSetAllowedMinMacLength() throws KmcCryptoManagerException {
        String[] args = new String[] {
                "-" + KmcCryptoManager.CFG_ALLOWED_MIN_MAC_LENGTH + "=256"
        };
        KmcCryptoManager myCryptoManager = new KmcCryptoManager(args);
        assertEquals(256, myCryptoManager.getAllowedMinMacLength());

        int minMacLength = 0;  // 0 means allowing all mac length
        myCryptoManager.setAllowedMinMacLength(minMacLength);
        assertEquals(minMacLength, myCryptoManager.getAllowedMinMacLength());

        minMacLength = 1024;
        myCryptoManager.setAllowedMinMacLength(minMacLength);
        assertEquals(minMacLength, myCryptoManager.getAllowedMinMacLength());

        minMacLength = -1;
        myCryptoManager.setAllowedMinMacLength(minMacLength);
        assertEquals(KmcCryptoManager.DEFAULT_ALLOWED_MIN_MAC_LENGTH, myCryptoManager.getAllowedMinMacLength());
    }

    @Test
    public void testBadAllowedMinMacLength() throws KmcCryptoManagerException {
        String[] args = new String[] {
                "-" + KmcCryptoManager.CFG_ALLOWED_MIN_MAC_LENGTH + "=abc"
        };
        try {
            new KmcCryptoManager(args);
            fail("Expected KmcCryptoManagerException not received");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, e.getErrorCode());
            assertTrue(e.getMessage().contains("is not an integer"));
        }

        try {
            cryptoManager.setAllowedMinMacLength(-1024);
            fail("Expected KmcCryptoManagerException not received");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, e.getErrorCode());
            assertTrue(e.getMessage().contains("cannot be negative"));
        }
    }

    @Test
    public void testSetMacLength() throws KmcCryptoManagerException {
        int macLength = 1024;
        cryptoManager.setMacLength(macLength);
        assertEquals(macLength, cryptoManager.getMacLength());

        macLength = -1;
        cryptoManager.setMacLength(macLength);
        assertEquals(macLength, cryptoManager.getMacLength());
    }

    @Test
    public void testBadMacLength() {
        try {
            cryptoManager.setMacLength(4);
            fail("Expected to receive KmcCryptoManagerException");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, e.getErrorCode());
            assertTrue(e.getMessage().contains("less than allowed minimum"));
        }

        try {
            cryptoManager.setMacLength(-256);
            fail("Expected to receive KmcCryptoManagerException");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, e.getErrorCode());
            assertTrue(e.getMessage().contains("is less than allowed minimum"));
        }

        try {
            cryptoManager.setMacLength(255);
            fail("Expected to receive KmcCryptoManagerException");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, e.getErrorCode());
            assertTrue(e.getMessage().contains("is not multiple of 8"));
        }
    }

    @Test
    public final void testTruncatedMac_SHA()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 256;    // full-length MAC of SHA-512 is 512 bits

        cryptoManager.setMessageDigestAlgorithm(MD_ALGORITHM);

        // create ICV of full MAC
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadataFull = creator.createIntegrityCheckValue(plaintext);
        assertFalse(metadataFull.contains("macLength"));

        // create ICV of truncated MAC
        cryptoManager.setMacLength(macLength);
        plaintext.reset();
        creator = cryptoManager.createIcvCreator();
        String metadataTruncated = creator.createIntegrityCheckValue(plaintext);
        assertTrue(metadataTruncated.contains("macLength:" + Integer.valueOf(macLength)));

        assertTrue(verifyTruncatedMac(metadataTruncated, metadataFull));

        // reset the CryptoManager and plaintext
        cryptoManager.setMacLength(-1);
        plaintext.reset();

        // verify the test string with the ICV
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(plaintext, metadataTruncated));
    }

    @Test
    public void testTruncatedMac_HMAC() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 128;

        // create ICV of full MAC
        IcvCreator creator = cryptoManager.createIcvCreator(HMAC_KEYREF);
        String metadataFull = creator.createIntegrityCheckValue(plaintext);
        assertFalse(metadataFull.contains("macLength"));

        // create ICV of truncated MAC
        cryptoManager.setMacLength(macLength);
        plaintext.reset();
        creator = cryptoManager.createIcvCreator(HMAC_KEYREF);
        String metadataTruncated = creator.createIntegrityCheckValue(plaintext);
        assertTrue(metadataTruncated.contains("macLength:" + Integer.valueOf(macLength)));

        assertTrue(verifyTruncatedMac(metadataTruncated, metadataFull));

        // reset the CryptoManager and plaintext
        cryptoManager.setMacLength(-1);
        plaintext.reset();

        // verify the test string with the ICV
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(plaintext, metadataTruncated));
    }

    @Test
    public void testNoMacLength_HMAC() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 256;

        // create ICV of truncated MAC
        cryptoManager.setMacLength(macLength);
        IcvCreator creator = cryptoManager.createIcvCreator(HMAC_KEYREF);
        String metadata = creator.createIntegrityCheckValue(plaintext);
        assertTrue(metadata.contains("macLength:" + Integer.valueOf(macLength)));

        // remove the macLength attribute in the metadata
        metadata = removeMacLength(metadata);
        assertFalse(metadata.contains("macLength"));

        // reset the CryptoManager and plaintext
        cryptoManager.setMacLength(-1);
        plaintext.reset();

        // verify the test string with the ICV
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(plaintext, metadata));
    }

    @Test
    public void testDifferentMacLength_HMAC() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 256;

        // create ICV of truncated MAC
        cryptoManager.setMacLength(macLength);
        IcvCreator creator = cryptoManager.createIcvCreator(HMAC_KEYREF);
        String metadata = creator.createIntegrityCheckValue(plaintext);
        assertTrue(metadata.contains("macLength:" + Integer.valueOf(macLength)));

        // change macLength in metadata to a different value
        metadata = removeMacLength(metadata);
        assertFalse(metadata.contains("macLength"));
        metadata = "macLength:128," + metadata;

        // reset the CryptoManager and plaintext
        cryptoManager.setMacLength(-1);
        plaintext.reset();

        // verify the test string with the ICV
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        try {
            verifier.verifyIntegrityCheckValue(plaintext, metadata);
            fail("Expected KmcCryptoException not received");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertTrue(e.getMessage().contains("different from the length of the ICV"));
        }
    }

    @Test
    public void testTruncatedMac_CMAC()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 64; // AES256 CMAC full MAC length is 128

        // create ICV of full MAC
        IcvCreator creator = cryptoManager.createIcvCreator(CMAC_KEYREF);
        String metadataFull = creator.createIntegrityCheckValue(plaintext);
        assertFalse(metadataFull.contains("macLength"));

        // create ICV of truncated MAC
        cryptoManager.setMacLength(macLength);
        plaintext.reset();
        creator = cryptoManager.createIcvCreator(CMAC_KEYREF);
        String metadataTruncated = creator.createIntegrityCheckValue(plaintext);
        assertTrue(metadataTruncated.contains("macLength:" + String.valueOf(macLength)));

        assertTrue(verifyTruncatedMac(metadataTruncated, metadataFull));

        // reset the CryptoManager and plaintext
        cryptoManager.setMacLength(-1);
        plaintext.reset();

        // verify the test string with the ICV
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(plaintext, metadataTruncated));
    }

    @Test
    public void testLongerMacLength_CMAC()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 256;  // AES256 CMAC full MAC length is 128

        // create ICV of truncated MAC
        cryptoManager.setMacLength(macLength);
        IcvCreator creator = cryptoManager.createIcvCreator(CMAC_KEYREF);
        try {
            creator.createIntegrityCheckValue(plaintext);
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertTrue(e.getMessage().contains("longer than the full MAC length"));
        }
    }

    @Test
    public void testNoMacLength_CMAC() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 128;  // AES256 CMAC full MAC length is 128

        // create ICV of truncated MAC
        cryptoManager.setMacLength(macLength);
        IcvCreator creator = cryptoManager.createIcvCreator(CMAC_KEYREF);
        String metadata = creator.createIntegrityCheckValue(plaintext);
        assertTrue(metadata.contains("macLength:" + Integer.valueOf(macLength)));

        // remove the macLength attribute in the metadata
        metadata = removeMacLength(metadata);
        assertFalse(metadata.contains("macLength"));

        // reset the CryptoManager and plaintext
        cryptoManager.setMacLength(-1);
        plaintext.reset();

        // verify the test string with the ICV and no macLength
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(plaintext, metadata));
    }

    // verify Digital Signature truncated MAC is not supported
    @Test
    public void testTruncatedMac_DS()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 1024; // full MAC length using RSA4096 key is 512 bytes (4096 bits)

        // create DS of truncated length
        cryptoManager.setDigitalSignatureAlgorithm(DS_ALGORITHM);
        cryptoManager.setMacLength(macLength);
        try {
            cryptoManager.createIcvCreator(DS_KEYREF);
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("Digital Signature does not support macLength"));
        }
    }

    @Test
    public void testTruncatedMac_GMAC()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 64; // default is 128 bits
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AAD_TEST_LENGTH);

        KmcCryptoManager myCryptoManager = new KmcCryptoManager(null);
        myCryptoManager.setCipherTransformation(GCM_TRANSFORMATION);

        // encrypt with default tag size
        Encrypter encrypter = myCryptoManager.createEncrypter(GMAC_KEYREF);
        String metadataDefault = encrypter.encrypt(aadPlaintext, AAD_LENGTH, iv, eos);
        byte[] encryptedDataDefault = eos.toByteArray();

        // reset the input
        aadPlaintext.reset();
        eos.reset();

        // encrypt with truncated tag length
        myCryptoManager.setMacLength(macLength);
        encrypter = myCryptoManager.createEncrypter(GMAC_KEYREF);
        String metadataTruncated = encrypter.encrypt(aadPlaintext, AAD_LENGTH, iv, eos);
        byte[] encryptedDataTruncated = eos.toByteArray();

        assertFalse(metadataDefault.contains("macLength:" + Integer.valueOf(macLength)));
        assertTrue(metadataTruncated.contains("macLength:" + Integer.valueOf(macLength)));
        assertTrue(encryptedDataDefault.length > encryptedDataTruncated.length);

        InputStream eis = new ByteArrayInputStream(encryptedDataTruncated);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_TEST_LENGTH);
        Decrypter decrypter = myCryptoManager.createDecrypter();
        decrypter.decrypt(eis, dos, metadataTruncated);
        String decryptedString = new String(dos.toByteArray(), "UTF-8");
        assertEquals(AAD_TEST_STRING, decryptedString);
    }

    @Test
    public void testLongerMac_GMAC()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        cryptoManager.setCipherTransformation(GCM_TRANSFORMATION);
        int macLength = 136; // default is 128 bits and it is also the maximum tag length
        int encryptedSize = TEST_STRING.length();
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);

        KmcCryptoManager myCryptoManager = new KmcCryptoManager(null);
        myCryptoManager.setCipherTransformation(GCM_TRANSFORMATION);

        // encrypt with tag length longer than max
        myCryptoManager.setMacLength(macLength);
        Encrypter encrypter = myCryptoManager.createEncrypter(GMAC_KEYREF);
        try {
            encrypter.encrypt(aadPlaintext, AAD_LENGTH, iv, eos);
            fail("Expected KmcCryptoException not received");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
            assertTrue(e.getMessage().contains("Invalid value for MAC size"));
        }
    }

    // GMAC decrypt requires macLength in metadata if it was used in encrypt
    @Test
    public void testNoMacLength_GMAC() throws KmcCryptoManagerException, KmcCryptoException, IOException {
        int macLength = 64; // default is 128 bits
        ByteArrayOutputStream eos = new ByteArrayOutputStream(AAD_TEST_LENGTH);

        KmcCryptoManager myCryptoManager = new KmcCryptoManager(null);
        myCryptoManager.setCipherTransformation(GCM_TRANSFORMATION);

        // encrypt with truncated tag length
        myCryptoManager.setMacLength(macLength);
        Encrypter encrypter = myCryptoManager.createEncrypter(GMAC_KEYREF);
        String metadata = encrypter.encrypt(aadPlaintext, AAD_LENGTH, iv, eos);
        byte[] encryptedData = eos.toByteArray();

        // confirm metadata contains the macLength
        assertTrue(metadata.contains("macLength:" + Integer.valueOf(macLength)));

        // remove macLength in the metadata
        metadata = removeMacLength(metadata);
        assertFalse(metadata.contains("macLength"));

        // decrypt without the macLength attribute in metadata
        InputStream eis = new ByteArrayInputStream(encryptedData);
        ByteArrayOutputStream dos = new ByteArrayOutputStream(AAD_TEST_LENGTH);
        Decrypter decrypter = myCryptoManager.createDecrypter();
        try {
            decrypter.decrypt(eis, dos, metadata);
            fail("Expected KmcCryptoException not received");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("mac check in GCM failed"));
        }
    }

}
