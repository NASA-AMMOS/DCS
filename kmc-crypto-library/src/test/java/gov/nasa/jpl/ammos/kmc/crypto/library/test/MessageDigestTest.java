package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Random;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import gov.nasa.jpl.ammos.kmc.crypto.IcvCreator;
import gov.nasa.jpl.ammos.kmc.crypto.IcvVerifier;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;

/**
 * Unit tests for Integrity Check using Message Digest algorithms.
 *
 *
 */
public class MessageDigestTest {

    // Java accepts algorithm names with - or no -, upper or lower case
    private static final String HASH_ALGORITHM = "SHA-";
    private static int[] lengths = new int[] {1, 256, 384, 512};
    private static final String DEFAULT_ALGORITHM = "SHA-256";

    private static KmcCryptoManager cryptoManager;

    @BeforeAll
    public static void setUp() throws KmcCryptoManagerException {
        cryptoManager = new KmcCryptoManager(null);
    }

    @Test
    public final void testDefaultAlgorithm()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String algorithm = cryptoManager.getMessageDigestAlgorithm();
        String testString = "This is the test string for the default Message Digest Algorithm";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));

        // create ICV for the test string
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadata = creator.createIntegrityCheckValue(bis);

        // confirm default algorithm is used
        assertTrue(metadata.contains(algorithm));

        // verify the test string with the ICV
        bis.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(bis, metadata));
    }

    @Test
    public final void testIntegrityCheckOfStream()
            throws KmcCryptoManagerException, KmcCryptoException {
        for (int length : lengths) {
            testIntegrityCheckOfStream(length);
        }
    }

    @Test
    public final void testIntegrityCheckOfBytes()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        for (int length : lengths) {
            testIntegrityCheckOfBytes(length);
        }
    }

    @Test
    public final void testIntegrityCheckOfString()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        for (int length : lengths) {
            testIntegrityCheckOfString(length);
        }
    }

    private void testIntegrityCheckOfStream(final int length)
            throws KmcCryptoManagerException, KmcCryptoException {
        String algorithm = HASH_ALGORITHM + String.valueOf(length);
        cryptoManager.setMessageDigestAlgorithm(algorithm);

        byte[] data = new byte[1000000];
        Random random = new Random();
        random.nextBytes(data);
        InputStream is = new ByteArrayInputStream(data);

        // create ICV for the test string
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadata = creator.createIntegrityCheckValue(is);

        // verify the test string with the ICV
        is = new ByteArrayInputStream(data);
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(is, metadata));
    }

    private void testIntegrityCheckOfBytes(final int length)
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String algorithm = HASH_ALGORITHM + String.valueOf(length);
        cryptoManager.setMessageDigestAlgorithm(algorithm);

        byte[] data = new byte[100];
        Random random = new Random();
        random.nextBytes(data);
        InputStream bis = new ByteArrayInputStream(data);

        // create ICV for the test string
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadata = creator.createIntegrityCheckValue(bis);
        // verify the test string with the ICV
        bis.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(bis, metadata));
    }

    private void testIntegrityCheckOfString(final int length)
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String algorithm = HASH_ALGORITHM + String.valueOf(length);
        cryptoManager.setMessageDigestAlgorithm(algorithm);

        String testString = "This is the test string for " + algorithm;
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));

        // create ICV for the test string
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadata = creator.createIntegrityCheckValue(bis);
        // verify the test string with the ICV
        bis.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(bis, metadata));
    }

    @Test
    public final void testRepeatedUse()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString1 = "String 1 for testing repeated use of the IcvCreator and IcvVerifier.";
        InputStream bis1 = new ByteArrayInputStream(testString1.getBytes("UTF-8"));
        String testString2 = "String 2 for testing repeated use of the IcvCreator and IcvVerifier.";
        InputStream bis2 = new ByteArrayInputStream(testString2.getBytes("UTF-8"));

        // create ICV for the test string
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadata1 = creator.createIntegrityCheckValue(bis1);
        // verify the test string with the ICV
        bis1.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(bis1, metadata1));

        String metadata2 = creator.createIntegrityCheckValue(bis2);
        bis2.reset();
        assertFalse(verifier.verifyIntegrityCheckValue(bis2, metadata1));
        bis2.reset();
        assertTrue(verifier.verifyIntegrityCheckValue(bis2, metadata2));

        bis1.reset();
        String metadata3 = creator.createIntegrityCheckValue(bis1);
        assertEquals(metadata1, metadata3);
    }

    @Test
    public final void testDifferentData()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString = "This is the test string for verifying a different input data.";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));

        // create ICV of the test string
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadata = creator.createIntegrityCheckValue(bis);
        // verify the test string with the ICV
        bis.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(bis, metadata));

        // verify a different string
        String badData = testString.replace('i', 'j');
        InputStream bis2 = new ByteArrayInputStream(badData.getBytes("UTF-8"));
        assertFalse(verifier.verifyIntegrityCheckValue(bis2, metadata));
    }

    @Test
    public final void testDifferentICV()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString = "This is the test string for testing corrupted metadata.";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));

        // create ICV of the test string
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadata = creator.createIntegrityCheckValue(bis);
        // verify the test string with the ICV
        bis.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(bis, metadata));

        // corrupt the ICV by changing its first byte to 'x'
        int i = metadata.indexOf("integrityCheckValue");
        i = metadata.indexOf(":", i + 1) + 1;
        String badMetadata = metadata.substring(0, i);
        char c = metadata.charAt(i);
        if (c == '0') {
            badMetadata = badMetadata + '1' + metadata.substring(i + 1);
        } else {
            badMetadata = badMetadata + '0' + metadata.substring(i + 1);
        }
        bis.reset();
        assertFalse(verifier.verifyIntegrityCheckValue(bis, badMetadata));
    }

    @Test
    public final void testInvalidMetadata()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString = "This is the test string for testing invalid metadata.";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));

        // create ICV of the test string
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadata = creator.createIntegrityCheckValue(bis);
        // verify the test string with the ICV
        bis.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(bis, metadata));

        // make the metadata invalid by changing ':' into '='
        String invalidMetadata = metadata.replace(':', '=');
        bis.reset();
        try {
            verifier.verifyIntegrityCheckValue(bis, invalidMetadata);
            fail("Expected CryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, e.getErrorCode());
        }

        // make the ICV invalid by changing its first byte to '?'
        int i = metadata.indexOf("integrityCheckValue");
        i = metadata.indexOf(":", i + 1) + 1;
        String invaliadICV = metadata.substring(0, i);
        invaliadICV = invaliadICV + '?' + metadata.substring(i + 1);
        bis.reset();
        try {
            verifier.verifyIntegrityCheckValue(bis, invaliadICV);
            fail("Expected CryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, e.getErrorCode());
            assertEquals(IllegalArgumentException.class, e.getCause().getClass());
        }
    }

    @Test
    public final void testNullInput()
            throws KmcCryptoManagerException, KmcCryptoException {
        IcvCreator creator = cryptoManager.createIcvCreator();

        try {
            creator.createIntegrityCheckValue((InputStream) null);
            fail("Expected CryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
    }

    @Test
    public final void testValidProvider()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        cryptoManager.setMessageDigestAlgorithm(DEFAULT_ALGORITHM);
        cryptoManager.setAlgorithmProvider(DEFAULT_ALGORITHM, "SUN");
        cryptoManager.setProviderClass("SUN", "sun.security.provider.Sun");

        byte[] data = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        InputStream is = new ByteArrayInputStream(data);

        // create signature for the test bytes
        IcvCreator creator = cryptoManager.createIcvCreator();
        String metadata = creator.createIntegrityCheckValue(is);

        // verify the test bytes with the signature
        is.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(is, metadata));

        cryptoManager.removeAlgorithmProvider(DEFAULT_ALGORITHM);
        cryptoManager.removeProviderClass("SUN");
    }

    @Test
    public final void testWrongProvider() throws KmcCryptoManagerException {
        cryptoManager.setMessageDigestAlgorithm(DEFAULT_ALGORITHM);
        cryptoManager.setAlgorithmProvider(DEFAULT_ALGORITHM, "SunJCE");
        cryptoManager.setProviderClass("SunJCE", "com.sun.crypto.provider.SunJCE");
        try {
            cryptoManager.createIcvCreator();
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getCause().getCause() instanceof KmcCryptoException);
            KmcCryptoException cause = (KmcCryptoException) e.getCause().getCause();
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, cause.getErrorCode());
            assertTrue(cause.getCause() instanceof java.security.NoSuchAlgorithmException);
        }
        cryptoManager.removeAlgorithmProvider(DEFAULT_ALGORITHM);
        cryptoManager.removeProviderClass("SunJCE");
    }

    @Test
    public final void testInvalidProvider() throws KmcCryptoManagerException {
        cryptoManager.setMessageDigestAlgorithm(DEFAULT_ALGORITHM);
        cryptoManager.setAlgorithmProvider(DEFAULT_ALGORITHM, "SunJCEx");
        try {
            cryptoManager.createIcvCreator();
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getCause().getCause() instanceof KmcCryptoException);
            KmcCryptoException cause = (KmcCryptoException) e.getCause().getCause();
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, cause.getErrorCode());
        }
        cryptoManager.removeAlgorithmProvider(DEFAULT_ALGORITHM);
    }

}
