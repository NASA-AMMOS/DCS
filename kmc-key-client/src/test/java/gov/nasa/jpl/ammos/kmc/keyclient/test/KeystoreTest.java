package gov.nasa.jpl.ammos.kmc.keyclient.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.UnrecoverableKeyException;

import javax.crypto.SecretKey;

import org.junit.BeforeClass;
import org.junit.Test;

import gov.nasa.jpl.ammos.kmc.keyclient.KmcKey;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClient;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientException;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientException.KmcKeyOpsErrorCode;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManager;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManagerException;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManagerException.KmcKeyOpsManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeystoreKeyClient;

/**
 * Unit tests for Keystores.
 *
 */
public class KeystoreTest {

    /**
     * 1) Keytool does not allow creating a keystore without storePass.
     * 2) Keytool does not allow creating a key without keyPass.  The default keyPass is the storePass.
     * 3) If a keystore is created with storePass "changeit", then it can be open with a null password.
     *    The key should be retrieved with "changeit" keyPass if it uses the storePass.
     * 4) Each key in the keystore can have a different keyPass.
     *
     * The symmetric-keys.jck was created using this command:
     * keytool -genseckey -keystore symmetric-keys.jck -storetype jceks -storepass kmcstorepass \
     *         -keyalg AES -keysize 256 -alias AES256key1 -keypass AES256keypass
     * keytool -genseckey -keystore symmetric-keys.jck -storetype jceks -storepass kmcstorepass \
     *         -keyalg AES -keysize 128 -alias AES128key1 (keyPass is the default storePass)
     */
    private static final String KEYSTORE_DIR = "/opt/ammos/kmc/test/input/";
    private static final String KEYSTORE_NAME = "keystore-test.jck";
    private static final String KEYSTORE_TYPE = KmcKeystoreKeyClient.KEYSTORE_TYPE_JCEKS;
    private static final String KEYSTORE_PASS = "kmcstorepass";
    private static final String AES256_KEYREF = "AES256key1";
    private static final String AES256_KEYPASS = "AES256keypass";
    private static final String AES128_KEYREF = "AES128key1";
    private static final String AES128_KEYPASS = null;    // i.e. same as storePass

    private static final String KEYSTORE_DIFFERENT_KEYPASSES = "keystore-different-keypass.jck";
    private static final String KEYSTORE_DEFAULT_STOREPASS = "keystore-changeit.jck";

    private static KmcKeyClientManager manager;
    private static KmcKeyClient keystoreService;
    private static String keystorePath;

    @BeforeClass
    public static void setUp() throws KmcKeyClientManagerException {
        manager = new KmcKeyClientManager(null);
        keystorePath = KEYSTORE_DIR;
    }

    @Test
    public final void testGetSymmetricKeyFilePath() throws KmcKeyClientManagerException, KmcKeyClientException {
        keystoreService = manager.getKmcKeystoreKeyClient(keystorePath + KEYSTORE_NAME, KEYSTORE_PASS, KEYSTORE_TYPE);

        KmcKey kmcKey = keystoreService.getSymmetricKey(AES256_KEYREF, AES256_KEYPASS);
        SecretKey key = (SecretKey) kmcKey.getJavaKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals("RAW", key.getFormat());
        assertEquals(256, (long) key.getEncoded().length * 8);

        kmcKey = keystoreService.getSymmetricKey(AES128_KEYREF, AES128_KEYPASS);
        key = (SecretKey) kmcKey.getJavaKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals("RAW", key.getFormat());
        assertEquals(128, (long) key.getEncoded().length * 8);
    }

    @Test
    public final void testBadKeystoreLocation() {
        try {
            manager.getKmcKeystoreKeyClient("bad_keystore_location", KEYSTORE_PASS, KEYSTORE_TYPE);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcKeyClientManagerException e) {
            assertEquals(KmcKeyOpsManagerErrorCode.NULL_VALUE, e.getErrorCode());
            assertTrue(e.getMessage().contains("No such file or directory"));
            assertTrue(e.getCause().getClass() == FileNotFoundException.class);
        }
    }

    @Test
    public final void testBadKeystorePass() {
        try {
            manager.getKmcKeystoreKeyClient(keystorePath + KEYSTORE_NAME,
                    "bad_keystore_pass", KEYSTORE_TYPE);
            fail("Expected KmcKeyClientManagerException not received.");
        } catch (KmcKeyClientManagerException e) {
            assertEquals(KmcKeyOpsManagerErrorCode.NULL_VALUE, e.getErrorCode());
            assertTrue(e.getMessage().contains("Keystore was tampered with, or password was incorrect"));
            assertTrue(e.getCause().getClass() == IOException.class);
        }
    }

    @Test
    public final void testBadKeystoreType() {
        try {
            manager.getKmcKeystoreKeyClient(keystorePath + KEYSTORE_NAME,
                    KEYSTORE_PASS, KmcKeystoreKeyClient.KEYSTORE_TYPE_JKS);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcKeyClientManagerException e) {
            assertEquals(KmcKeyOpsManagerErrorCode.NULL_VALUE, e.getErrorCode());
            assertTrue(e.getMessage().contains("Invalid keystore format"));
            assertTrue(e.getCause().getClass() == IOException.class);
        }
    }

    @Test
    public final void testBadKeyRef() throws KmcKeyClientManagerException {
        KmcKeyClient localKeystore = manager.getKmcKeystoreKeyClient(
                keystorePath + KEYSTORE_NAME, KEYSTORE_PASS, KEYSTORE_TYPE);
        String badKeyRef = "badKeyRef";
        try {
            localKeystore.getSymmetricKey(badKeyRef, AES256_KEYPASS);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains(badKeyRef + " not found"));
        }
    }

    @Test
    public final void testBadKeyPass() throws KmcKeyClientManagerException {
        KmcKeyClient localKeystore = manager.getKmcKeystoreKeyClient(
                keystorePath + KEYSTORE_NAME, KEYSTORE_PASS, KEYSTORE_TYPE);
        try {
            localKeystore.getSymmetricKey(AES256_KEYREF, "AES257keypass");
            fail("Expected KmcCryptoException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("Given final block not properly padded"));
            assertTrue(e.getCause().getClass() == UnrecoverableKeyException.class);
        }
    }

    @Test
    public final void testDifferentKeyPasses() throws KmcKeyClientManagerException, KmcKeyClientException {
        keystoreService = manager.getKmcKeystoreKeyClient(KEYSTORE_DIFFERENT_KEYPASSES, KEYSTORE_PASS,
                KmcKeystoreKeyClient.KEYSTORE_TYPE_JCEKS);
        KmcKey kmcKey = keystoreService.getSymmetricKey("AES128key1", "AES128keypass");
        SecretKey key = (SecretKey) kmcKey.getJavaKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals("RAW", key.getFormat());
        assertEquals(128, (long) key.getEncoded().length * 8);
        kmcKey = keystoreService.getSymmetricKey("AES256key1", "AES256keypass");
        key = (SecretKey) kmcKey.getJavaKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals("RAW", key.getFormat());
        assertEquals(256, (long) key.getEncoded().length * 8);
    }

    @Test
    public final void testChangeitPassword() throws KmcKeyClientManagerException, KmcKeyClientException {
        keystoreService = manager.getKmcKeystoreKeyClient(KEYSTORE_DEFAULT_STOREPASS,
                null, KmcKeystoreKeyClient.KEYSTORE_TYPE_JCEKS);
        KmcKey kmcKey = keystoreService.getSymmetricKey("AES128key1", null);
        SecretKey key = (SecretKey) kmcKey.getJavaKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals("RAW", key.getFormat());
        assertEquals(128, (long) key.getEncoded().length * 8);
    }

    @Test
    public final void testGetSymmetricKeyClasspath() throws KmcKeyClientManagerException, KmcKeyClientException {
        keystoreService = manager.getKmcKeystoreKeyClient(KEYSTORE_DIFFERENT_KEYPASSES,
                KEYSTORE_PASS, KmcKeystoreKeyClient.KEYSTORE_TYPE_JCEKS);
        KmcKey kmcKey = keystoreService.getSymmetricKey(AES256_KEYREF, AES256_KEYPASS);
        SecretKey key = (SecretKey) kmcKey.getJavaKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals("RAW", key.getFormat());
        assertEquals(256, (long) key.getEncoded().length * 8);
    }


}
