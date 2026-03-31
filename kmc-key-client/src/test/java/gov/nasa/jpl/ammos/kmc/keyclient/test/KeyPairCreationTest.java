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
 * Unit tests for Asymmetric Key Pair.
 *
 */
public class KeyPairCreationTest {
    private static final String CREATOR = "testuser3300";
    private static final String KEYREF_HEAD = "kmc/unittest/";
    private static final String[] RSA_KEY_REFS = {KEYREF_HEAD + "RSA2048",
                            KEYREF_HEAD + "RSA3072", KEYREF_HEAD + "RSA4096"};
    private static final String RSA_KEY_ALGORITHM = "RSA";
    private static final int[] RSA_KEY_LENGTHS = {2048, 3072, 4096};

    private static KmcKmipKeyClient kmipClient;

    @BeforeClass
    public static void setUp() throws KmcKeyClientManagerException, KmcKeyClientException {
        KmcKeyClientManager manager = new KmcKeyClientManager(null);
        kmipClient = (KmcKmipKeyClient) manager.getKmcKmipKeyClient();
    }

    // in case keys were not destroyed when the tests failed
    @AfterClass
    public static void destroyKeys() throws KmcKeyClientException {
        for (int i = 0; i < RSA_KEY_REFS.length; i++) {
            String keyRef = RSA_KEY_REFS[i];
            if (kmipClient.locateCryptoKey(keyRef) != null) {
                kmipClient.destroyAsymmetricKeyPair(keyRef);
            }
        }
    }

    @Test
    public final void testCreateKeyPairs() throws KmcKeyClientException {
        for (int i = 0; i < RSA_KEY_REFS.length; i++) {
            createKeyPair(RSA_KEY_REFS[i], RSA_KEY_ALGORITHM, RSA_KEY_LENGTHS[i]);
        }
    }

    private void createKeyPair(final String keyRef, final String algorithm, final int keyLength)
            throws KmcKeyClientException {
        String publicKeyId = kmipClient.locateCryptoKey(keyRef);
        if (publicKeyId != null) {
            kmipClient.destroyAsymmetricKeyPair(keyRef);
        }
        // create key pair
        KmcKey kmcKey = kmipClient.createAsymmetricKeyPair(CREATOR,
                keyRef, algorithm, keyLength);
        assertNotNull(kmcKey);
        // verify public key
        assertNotNull(kmipClient.locateCryptoKey(keyRef));
        KmcKey publicKey = kmipClient.getPublicKey(keyRef);
        String keyAlgorithm = publicKey.getKeyAlgorithm();
        assertEquals(algorithm, keyAlgorithm);
        assertEquals(keyLength, publicKey.getKeyLength());
        // verify private key
        KmcKey privateKey = kmipClient.getPrivateKey(keyRef);
        assertEquals(algorithm, privateKey.getKeyAlgorithm());
        assertEquals(keyLength, privateKey.getKeyLength());
        // delete key pair
        kmipClient.destroyAsymmetricKeyPair(keyRef);
        assertNull(kmipClient.locateCryptoKey(keyRef));
    }

    @Test
    public final void testKeyPairBadParameters() throws KmcKeyClientException {
        final String badKeyAlgorithm = "AES";
        final int badKeyLength = 256;
        try {
            kmipClient.createAsymmetricKeyPair(CREATOR,
                    RSA_KEY_REFS[0], badKeyAlgorithm, RSA_KEY_LENGTHS[0]);
            fail("Expected KeyOpsServiceException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("failed"));
        }
        try {
            kmipClient.createAsymmetricKeyPair(CREATOR,
                    RSA_KEY_REFS[0], RSA_KEY_ALGORITHM, badKeyLength);
            fail("Expected KeyOpsServiceException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("failed"));
        }
    }

    @Test
    public final void testNonExistKey() throws KmcKeyClientException {
        assertNull(kmipClient.locateCryptoKey("nonExistentKeyRef"));
        try {
            kmipClient.getPublicKey("nonExistentKeyRef");
            fail("Expected KeyOpsServiceException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("does not exist"));
        }
        try {
            kmipClient.getPrivateKey("nonExistentKeyRef");
            fail("Expected KeyOpsServiceException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("does not exist"));
        }
        try {
            kmipClient.destroyAsymmetricKeyPair("nonExistentKeyRef");
            fail("Expected KeyOpsServiceException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("does not exist"));
        }
    }
}
