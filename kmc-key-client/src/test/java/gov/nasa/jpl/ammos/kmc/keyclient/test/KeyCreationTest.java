package gov.nasa.jpl.ammos.kmc.keyclient.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import gov.nasa.jpl.ammos.kmc.keyclient.KmcKey;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientException;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientException.KmcKeyOpsErrorCode;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManager;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManagerException;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKmipKeyClient;

/**
 * Unit tests for KMClient.
 *
 */
public class KeyCreationTest {

    private static final String KEYREF_HEAD = "kmc/unittest/";
    private static final String CREATOR = "testuser3300";

    private static final String AES_ALGORITHM = "AES";
    private static final String[] AES_KEY_REFS = {KEYREF_HEAD + "aes128", KEYREF_HEAD + "aes256"};
    private static final int[] AES_KEY_LENGTHS = {128, 256};

    private static final String TRIPLE_DES_ALGORITHM = "DESede";
    private static final String[] TRIPLE_DES_KEY_REFS = {KEYREF_HEAD + "3des168"};
    private static final int[] TRIPLE_DES_KEY_LENGTHS = {168};

    private static final String HMAC_ALGORITHM = "HMAC_SHA";
    private static final String[] HMAC_KEY_REFS = {KEYREF_HEAD + "HmacSha1", KEYREF_HEAD + "HmacSha256",
        KEYREF_HEAD + "HmacSha384", KEYREF_HEAD + "HmacSha512"};
    private static final int[] HMAC_KEY_LENGTHS = {128, 256, 384, 512};

    private static KmcKmipKeyClient kmipClient;

    @BeforeClass
    public static void setUp() throws KmcKeyClientManagerException, KmcKeyClientException {
        KmcKeyClientManager manager = new KmcKeyClientManager(null);
        kmipClient = (KmcKmipKeyClient) manager.getKmcKmipKeyClient();
    }

    // in case keys were not destroyed when the tests failed
    @AfterClass
    public static void destroyKeys() throws KmcKeyClientException {
        for (int i = 0; i < AES_KEY_REFS.length; i++) {
            if (kmipClient.locateCryptoKey(AES_KEY_REFS[i]) != null) {
                kmipClient.destroySymmetricKey(AES_KEY_REFS[i]);
            }
        }
        for (int i = 0; i < TRIPLE_DES_KEY_REFS.length; i++) {
            if (kmipClient.locateCryptoKey(TRIPLE_DES_KEY_REFS[i]) != null) {
                kmipClient.destroySymmetricKey(TRIPLE_DES_KEY_REFS[i]);
            }
        }
        for (int i = 0; i < HMAC_KEY_REFS.length; i++) {
            if (kmipClient.locateCryptoKey(HMAC_KEY_REFS[i]) != null) {
                kmipClient.destroyAsymmetricKeyPair(HMAC_KEY_REFS[i]);
            }
        }
    }

    @Test
    public final void testCreateAESkeys() throws KmcKeyClientException {
        for (int i = 0; i < AES_KEY_REFS.length; i++) {
            testCreateSymmetricKey(AES_KEY_REFS[i], "AES", AES_KEY_LENGTHS[i]);
        }
    }

    @Test
    public final void testCreateTripleDESkeys() throws KmcKeyClientException {
        for (int i = 0; i < TRIPLE_DES_KEY_REFS.length; i++) {
            testCreateSymmetricKey(TRIPLE_DES_KEY_REFS[i], TRIPLE_DES_ALGORITHM, TRIPLE_DES_KEY_LENGTHS[i]);
        }
    }

    private void testCreateSymmetricKey(final String keyRef, final String keyAlgorithm, final int keyLength)
            throws KmcKeyClientException {
        String keyId = kmipClient.locateCryptoKey(keyRef);
        if (keyId != null) {
            kmipClient.destroyCryptographicKey(keyId);
        }
        KmcKey key = kmipClient.createEncryptionKey(CREATOR, keyRef, keyAlgorithm, keyLength);
        assertNotNull(key);
        assertEquals(keyAlgorithm, key.getKeyAlgorithm());
        assertEquals(keyLength, key.getKeyLength());
        assertEquals(KmcKey.USAGE_MASK_ENCRYPTION, key.getUsageMask());
        kmipClient.destroyCryptographicKey(key.getKeyId());
        assertNull(kmipClient.locateCryptoKey(keyRef));
    }

    @Test
    public final void testCreateIcvKeys() throws KmcKeyClientException {
        testCreateIcvKey(HMAC_KEY_REFS[0], HMAC_ALGORITHM + "1", HMAC_KEY_LENGTHS[0]);
        for (int i = 1; i < HMAC_KEY_REFS.length; i++) {
            testCreateIcvKey(HMAC_KEY_REFS[i],
                    HMAC_ALGORITHM + String.valueOf(HMAC_KEY_LENGTHS[i]), HMAC_KEY_LENGTHS[i]);
        }
    }

    private void testCreateIcvKey(final String keyRef, final String keyAlgorithm, final int keyLength)
            throws KmcKeyClientException {
        String keyId = kmipClient.locateCryptoKey(keyRef);
        if (keyId != null) {
            kmipClient.destroyCryptographicKey(keyId);
        }
        KmcKey kmcKey = kmipClient.createIntegrityCheckKey(CREATOR, keyRef, keyAlgorithm, keyLength);
        assertNotNull(kmcKey);
        assertEquals(keyAlgorithm, kmcKey.getKeyAlgorithm());
        assertEquals(keyLength, kmcKey.getKeyLength());
        assertEquals(KmcKey.USAGE_MASK_ICV, kmcKey.getUsageMask());
        kmipClient.destroySymmetricKey(keyRef);
        assertNull(kmipClient.locateCryptoKey(keyRef));
    }

    /* Anomalies */

    @Test
    public final void testCreateSameKey() throws KmcKeyClientException {
        KmcKey key1 = kmipClient.createEncryptionKey(CREATOR, AES_KEY_REFS[0], AES_ALGORITHM, AES_KEY_LENGTHS[0]);
        KmcKey key2 = kmipClient.createEncryptionKey(CREATOR, AES_KEY_REFS[0], AES_ALGORITHM, AES_KEY_LENGTHS[0]);
        // the second key is not created.
        assertNull(key2);
        // the first key still can be retrieved.
        String keyId = kmipClient.locateCryptoKey(AES_KEY_REFS[0]);
        assertEquals(key1.getKeyId(), keyId);
        kmipClient.destroyCryptographicKey(key1.getKeyId());
    }

    @Test
    public final void testEncryptionBadAlgorithm() throws KmcKeyClientException {
        try {
            kmipClient.createEncryptionKey(CREATOR, AES_KEY_REFS[0], "ASE", AES_KEY_LENGTHS[0]);
            fail("Expected KeyOpsServiceException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("Invalid algorithm"));
        }
    }

    @Test
    public final void testIcvBadAlgorithm() throws KmcKeyClientException {
        try {
            kmipClient.createIntegrityCheckKey(CREATOR, "testHmacBadAlgorithm", "HMAC_SHA257", 256);
            fail("Expected KeyOpsServiceException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_ALGORITHM_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("Invalid algorithm"));
        }
    }

    @Test
    public final void testAESbadKeyLength() throws KmcKeyClientException {
        try {
            kmipClient.createEncryptionKey(CREATOR, "testAESbadKeyLength", AES_ALGORITHM, 512);
            fail("Expected KeyOpsServiceException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("failed"));
        }
    }

    @Test
    public final void testTripleDESbadKeyLength() throws KmcKeyClientException {
        try {
            kmipClient.createEncryptionKey(CREATOR, "testTripleDESbadKeyLength", TRIPLE_DES_ALGORITHM, 256);
            fail("Expected KeyOpsServiceException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("failed"));
        }
    }

    @Test
    public final void testHmacBadKeyLength() throws KmcKeyClientException {
        // the HMAC algorithm determines the key length
        KmcKey key = kmipClient.createIntegrityCheckKey(CREATOR, "testHmacBadKeyLength", "HMAC_SHA256", 257);
        assertNotNull(key);
        kmipClient.destroyCryptographicKey(key.getKeyId());
    }

    @Test
    public final void testRetrieveNonExistKey() throws KmcKeyClientException {
        String nonExistKeyRef = "nonExistKeyRef";

        assertNull(kmipClient.locateCryptoKey(nonExistKeyRef));
        try {
            kmipClient.getKey(nonExistKeyRef);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertTrue(e.getMessage().contains("does not exist"));
        }
        try {
            kmipClient.getSymmetricKey(nonExistKeyRef);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertTrue(e.getMessage().contains("does not exist"));
        }
        try {
            kmipClient.getPublicKey(nonExistKeyRef);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertTrue(e.getMessage().contains("does not exist"));
        }
        try {
            kmipClient.getPrivateKey(nonExistKeyRef);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertTrue(e.getMessage().contains("does not exist"));
        }
    }

    @Test
    public final void testDestroyNonExistKey() throws KmcKeyClientException {
        String nonExistKeyId = "nonExistKeyId";
        String nonExistKeyRef = "nonExistKeyRef";
        try {
            kmipClient.destroyCryptographicKey(nonExistKeyId);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertTrue(e.getMessage().contains("Failed"));
        }
        try {
            kmipClient.destroySymmetricKey(nonExistKeyRef);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertTrue(e.getMessage().contains("does not exist"));
        }
        try {
            kmipClient.destroyAsymmetricKeyPair(nonExistKeyRef);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertTrue(e.getMessage().contains("does not exist"));
        }
    }

}
