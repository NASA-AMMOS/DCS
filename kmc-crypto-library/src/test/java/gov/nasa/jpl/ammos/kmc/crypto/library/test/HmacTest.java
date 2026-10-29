package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
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
import gov.nasa.jpl.ammos.kmc.crypto.library.KeystoreKeyServiceClient;

/**
 * Unit tests for Integrity Check using HMAC algorithms.
 *
 *
 */
public class HmacTest {
    // key length (128 and 256) and HMAC output size (1, 256, 384, 512) are unrelated
    private static final String KEYNAME_HEAD = "kmc/test/";
    private static final String[] KEYREFS = new String[] {
        "HmacSHA1", "HmacSHA256", "HmacSHA384", "HmacSHA512"
    };

    private static final ClassLoader loader = HmacTest.class.getClassLoader();
    private static final String KEYSTORE_FILE = "symmetric-keys.jck";
    private static final String KEYSTORE_FILE_PATH = loader.getResource(KEYSTORE_FILE).getFile();
    private static final String KEYSTORE_PASS = "kmcstorepass";
    private static final String KEYSTORE_KEYPASS = "kmckeypass";

    private static final String DEFAULT_ALGORITHM = KEYREFS[1];
    private static final String DEFAULT_KEYREF = KEYNAME_HEAD + KEYREFS[1];
    // WRONG_ALGORITHM_KEYREF cannot be tested because all algorithms can be used for ICV
    //private static final String WRONG_ALGORITHM_KEYREF = KEYNAME_HEAD + "AES128";

    private static KmcCryptoManager cryptoManager;

    @BeforeAll
    public static void setUp() throws KmcCryptoManagerException {
        cryptoManager = new KmcCryptoManager(null);
    }

    @Test
    public final void testIntegrityCheckStringHmacSHA256()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        testIntegrityCheckOfString(KEYNAME_HEAD + KEYREFS[1]);
    }

    @Test
    public final void testIntegrityCheckStringAll()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        for (int i = 0; i < KEYREFS.length; i++) {
            testIntegrityCheckOfString(KEYNAME_HEAD + KEYREFS[i]);
        }
    }

    @Test
    public final void testIntegrityCheckBytesAll()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        for (int i = 0; i < KEYREFS.length; i++) {
            testIntegrityCheckOfBytes(KEYNAME_HEAD + KEYREFS[i]);
        }
    }

    @Test
    public final void testIntegrityCheckStreamAll()
            throws KmcCryptoManagerException, KmcCryptoException {
        for (int i = 0; i < KEYREFS.length; i++) {
            testIntegrityCheckOfStream(KEYNAME_HEAD + KEYREFS[i]);
        }
    }

    private void testIntegrityCheckOfString(final String keyRef)
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString = "This is the string for testing HMAC";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));

        // create ICV for the test string
        IcvCreator creator = cryptoManager.createIcvCreator(keyRef);
        String metadata = creator.createIntegrityCheckValue(bis);

        // verify the test string with the ICV
        bis.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(bis, metadata));
    }

    private void testIntegrityCheckOfBytes(final String keyRef)
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        byte[] data = new byte[100];
        Random random = new Random();
        random.nextBytes(data);
        InputStream bis = new ByteArrayInputStream(data);

        // create ICV for the test string
        IcvCreator creator = cryptoManager.createIcvCreator(keyRef);
        String metadata = creator.createIntegrityCheckValue(bis);

        // verify the test string with the ICV
        bis.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(bis, metadata));
    }

    private void testIntegrityCheckOfStream(final String keyRef)
            throws KmcCryptoManagerException, KmcCryptoException {
        byte[] data = new byte[1000000];
        Random random = new Random();
        random.nextBytes(data);
        InputStream is = new ByteArrayInputStream(data);

        // create ICV for the test string
        IcvCreator creator = cryptoManager.createIcvCreator(keyRef);
        String metadata = creator.createIntegrityCheckValue(is);

        // verify the test string with the ICV
        is = new ByteArrayInputStream(data);
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(is, metadata));
    }

    @Test
    public final void testRepeatedUse()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString1 = "String 1 for testing repeated use of the IcvCreator and IcvVerifier.";
        String testString2 = "String 2 for testing repeated use of the IcvCreator and IcvVerifier.";
        InputStream bis1 = new ByteArrayInputStream(testString1.getBytes("UTF-8"));
        InputStream bis2 = new ByteArrayInputStream(testString2.getBytes("UTF-8"));

        // repeated use of the same creator and verifier
        IcvCreator creator = cryptoManager.createIcvCreator(DEFAULT_KEYREF);
        IcvVerifier verifier = cryptoManager.createIcvVerifier();

        String metadata1 = creator.createIntegrityCheckValue(bis1);
        bis1.reset();
        assertTrue(verifier.verifyIntegrityCheckValue(bis1, metadata1));

        String metadata2 = creator.createIntegrityCheckValue(bis2);
        bis2.reset();
        assertTrue(verifier.verifyIntegrityCheckValue(bis2, metadata2));
        bis2.reset();
        assertFalse(verifier.verifyIntegrityCheckValue(bis2, metadata1));

        // same ICV for same input string.
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
        IcvCreator creator = cryptoManager.createIcvCreator(DEFAULT_KEYREF);
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
        IcvCreator creator = cryptoManager.createIcvCreator(DEFAULT_KEYREF);
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
    public final void testZeroByte() throws KmcCryptoManagerException {
        byte[] inBytes = new byte[0];
        InputStream is = new ByteArrayInputStream(inBytes);
        IcvCreator creator = cryptoManager.createIcvCreator(DEFAULT_KEYREF);
        try {
            creator.createIntegrityCheckValue(is);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
    }

    @Test
    public final void testInvalidMetadata()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString = "This is the test string for testing invalid metadata.";
        InputStream bis = new ByteArrayInputStream(testString.getBytes("UTF-8"));

        // create ICV of the test string
        IcvCreator creator = cryptoManager.createIcvCreator(DEFAULT_KEYREF);
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
            fail("Excepted CryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, e.getErrorCode());
        }
    }

    @Test
    public final void testBadMetadata()
            throws KmcCryptoManagerException, KmcCryptoException, FileNotFoundException  {
        String icv = "823BF1B49D2D2BC57FCBF9D129A12669FCB775DB48";
        InputStream is = new ByteArrayInputStream(new byte[]{0});
        IcvVerifier verifier = cryptoManager.createIcvVerifier();

        String nonExistKey = "metadataType:IntegrityCheckMetadata,"
                + "keyRef:kmc/test/nonExistKey,cryptoAlgorithm:HmacSHA256,keyLength:256,"
                + "integrityCheckValue:" + icv;
        try {
            verifier.verifyIntegrityCheckValue(is, nonExistKey);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }

        String missingAlg = "metadataType:IntegrityCheckMetadata,"
                + "keyRef:kmc/test/HmacSHA256,keyLength:256,"
                + "integrityCheckValue:" + icv;
        try {
            verifier.verifyIntegrityCheckValue(is, missingAlg);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_METADATA_ERROR, e.getErrorCode());
        }

        String wrongKeyAlg = "metadataType:IntegrityCheckMetadata,"
                + "keyRef:kmc/test/HmacSHA256,cryptoAlgorithm:HmacSHA244,keyLength:256,"
                + "integrityCheckValue:" + icv;
        try {
            verifier.verifyIntegrityCheckValue(is, wrongKeyAlg);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
        }
    }

    // comment out because FIPS VMs cannot load JCEKS keystore
    //@Test
    public final void testKeyfromKeystore()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        String testString1 = "This is the test string for local key.";
        InputStream bis1 = new ByteArrayInputStream(testString1.getBytes("UTF-8"));

        // create ICV for the test string
        IcvCreator creator = cryptoManager.createIcvCreator(
                KEYSTORE_FILE_PATH, KEYSTORE_PASS,
                KeystoreKeyServiceClient.KEYSTORE_TYPE_JCEKS, DEFAULT_KEYREF, KEYSTORE_KEYPASS);
        String metadata1 = creator.createIntegrityCheckValue(bis1);
        // verify the test string with the ICV
        bis1.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier(
                KEYSTORE_FILE_PATH, KEYSTORE_PASS,
                KeystoreKeyServiceClient.KEYSTORE_TYPE_JCEKS, KEYSTORE_KEYPASS);
        assertTrue(verifier.verifyIntegrityCheckValue(bis1, metadata1));

        // use the IcvCreator and IcvVerifier again
        String testString2 = "This is another string for local key.";
        InputStream bis2 = new ByteArrayInputStream(testString2.getBytes("UTF-8"));
        String metadata2 = creator.createIntegrityCheckValue(bis2);
        bis2.reset();
        assertTrue(verifier.verifyIntegrityCheckValue(bis2, metadata2));
        bis1.reset();
        assertFalse(verifier.verifyIntegrityCheckValue(bis1, metadata2));
        bis2.reset();
        assertFalse(verifier.verifyIntegrityCheckValue(bis2, metadata1));
    }

    /*
     * WRONG_ALGORITHM_KEYREF cannot be tested because all algorithms can be used for ICV
    @Test
    public final void testWrongKeyAlgorithm() throws KmcCryptoManagerException, KmcCryptoException  {
        try {
            cryptoManager.createIcvCreator(WRONG_ALGORITHM_KEYREF);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
        }
        try {
            cryptoManager.createIcvCreator(
                    keystorePath + "symmetric-keys.jck", KEYSTORE_PASS,
                    KeystoreKeyServiceClient.KEYSTORE_TYPE_JCEKS, WRONG_ALGORITHM_KEYREF, KEYSTORE_KEYPASS);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            Throwable cause = e.getCause().getCause();  // first cause is InvocationTargetException
            assertTrue(cause instanceof KmcCryptoException);
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, ((KmcCryptoException) cause).getErrorCode());
        }
    }
    */

    @Test
    public final void testNullKey() throws KmcCryptoManagerException  {
        try {
            cryptoManager.createIcvCreator(null);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.NULL_VALUE, e.getErrorCode());
        }
    }

    @Test
    public final void testNonExistKey() throws KmcCryptoManagerException, KmcCryptoException  {
        try {
            cryptoManager.createIcvCreator("kmc/test/nonExistKey");
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        }
    }

    @Test
    public final void testNullInputStream() throws KmcCryptoManagerException, KmcCryptoException  {
        IcvCreator creator = cryptoManager.createIcvCreator(DEFAULT_KEYREF);
        try {
            creator.createIntegrityCheckValue((InputStream) null);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.INVALID_INPUT_VALUE, e.getErrorCode());
        }
    }

    @Test
    public final void testValidProvider()
            throws KmcCryptoManagerException, KmcCryptoException, IOException {
        cryptoManager.setAlgorithmProvider(DEFAULT_ALGORITHM, "SunJCE");
        cryptoManager.setProviderClass("SunJCE", "com.sun.crypto.provider.SunJCE");

        byte[] data = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        InputStream is = new ByteArrayInputStream(data);

        // create ICV for the test bytes
        IcvCreator creator = cryptoManager.createIcvCreator(DEFAULT_KEYREF);
        String metadata = creator.createIntegrityCheckValue(is);

        // verify the test bytes with the ICV
        is.reset();
        IcvVerifier verifier = cryptoManager.createIcvVerifier();
        assertTrue(verifier.verifyIntegrityCheckValue(is, metadata));

        cryptoManager.removeAlgorithmProvider(DEFAULT_ALGORITHM);
        cryptoManager.removeProviderClass("SunJCE");
    }

    @Test
    public final void testWrongProvider() throws KmcCryptoManagerException {
        cryptoManager.setAlgorithmProvider(DEFAULT_ALGORITHM, "SUN");
        cryptoManager.setProviderClass("SUN", "sun.security.provider.Sun");
        try {
            cryptoManager.createIcvCreator(DEFAULT_KEYREF);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            Throwable cause2 = e.getCause();
            if (cause2.getCause() instanceof KmcCryptoException) {
                // IcvCreator using keystore has one more level of cause.
                cause2 = cause2.getCause();
            }
            assertTrue(cause2 instanceof KmcCryptoException);
            KmcCryptoException cause = (KmcCryptoException) cause2;
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, cause.getErrorCode());
            assertTrue(cause.getCause() instanceof java.security.NoSuchAlgorithmException);
        } finally {
            cryptoManager.removeAlgorithmProvider(DEFAULT_ALGORITHM);
            cryptoManager.removeProviderClass("SUN");
        }
    }

    @Test
    public final void testInvalidProvider() throws KmcCryptoManagerException {
        cryptoManager.setAlgorithmProvider(DEFAULT_ALGORITHM, "SunJCEx");
        try {
            cryptoManager.createIcvCreator(DEFAULT_KEYREF);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoManagerException e) {
            assertEquals(KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR, e.getErrorCode());
            Throwable cause2 = e.getCause();
            if (cause2.getCause() instanceof KmcCryptoException) {
                // IcvCreator using keystore has one more level of cause.
                cause2 = cause2.getCause();
            }
            assertTrue(cause2 instanceof KmcCryptoException);
            KmcCryptoException cause = (KmcCryptoException) cause2;
            assertEquals(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR, cause.getErrorCode());
        } finally {
            cryptoManager.removeAlgorithmProvider(DEFAULT_ALGORITHM);
        }
    }

}
