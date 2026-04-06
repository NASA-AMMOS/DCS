package gov.nasa.jpl.ammos.kmc.keyclient.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.ntb.inf.kmip.kmipenum.EnumState;
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
public class KeyPairOperationTest {

    private static final String CREATOR = "testuser3300";
    private static final String KEYPAIR_REF = "keypair-operation-test";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_LENGTH = 2048;

    private static KmcKmipKeyClient kmipClient;

    // create kmipClient before the tests.
    @BeforeClass
    public static void setUp() throws KmcKeyClientManagerException, KmcKeyClientException {
        KmcKeyClientManager manager = new KmcKeyClientManager(null);
        kmipClient = (KmcKmipKeyClient) manager.getKmcKmipKeyClient();
    }

    // destroy the keys before each test.
    @Before
    public final void cleanUp() throws KmcKeyClientException {
        if (kmipClient.locateCryptoKey(KEYPAIR_REF) != null) {
            kmipClient.destroyAsymmetricKeyPair(KEYPAIR_REF);
        }
    }

    // in case the keys were not destroyed when the tests failed.
    @AfterClass
    public static void tearDown() throws KmcKeyClientException {
        if (kmipClient.locateCryptoKey(KEYPAIR_REF) != null) {
            kmipClient.destroyAsymmetricKeyPair(KEYPAIR_REF);
        }
    }

    @Test
    public final void testKeyPairOperation() throws KmcKeyClientException {
        KmcKey key = kmipClient.createAsymmetricKeyPair(CREATOR,
                KEYPAIR_REF, KEY_ALGORITHM, KEY_LENGTH);
        assertNotNull(key);
        assertEquals(new EnumState(EnumState.PreActive).getKey(), key.getState());
        kmipClient.operateKeyPair(key.getKeyRef(), key.getKeyId(), "Activate", null);
        String state = kmipClient.getKeyPairState(KEYPAIR_REF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Active).getKey(), state);
        kmipClient.operateKeyPair(key.getKeyRef(), key.getKeyId(), "Deactivate", "The key no long in use");
        state = kmipClient.getKeyPairState(KEYPAIR_REF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Deactivated).getKey(), state);
        kmipClient.operateKeyPair(key.getKeyRef(), key.getKeyId(), "Revoke", "The key has been compromised");
        state = kmipClient.getKeyPairState(KEYPAIR_REF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Compromised).getKey(), state);
        kmipClient.operateKeyPair(key.getKeyRef(), key.getKeyId(), "Destroy", null);
        // after the key is destroyed, can't locate private key from KMIP service.
        state = kmipClient.getKeyState(KEYPAIR_REF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Destroyed).getKey(), state);
        assertNull(kmipClient.locateCryptoKey(KEYPAIR_REF));
    }

    @Test
    public final void testInvalidOperation() throws KmcKeyClientException {
        KmcKey key = kmipClient.createAsymmetricKeyPair(CREATOR,
                KEYPAIR_REF, KEY_ALGORITHM, KEY_LENGTH);
        assertNotNull(key);
        assertEquals(new EnumState(EnumState.PreActive).getKey(), key.getState());
        try {
            kmipClient.operateKey(key.getKeyRef(), key.getKeyId(), "Active", null);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
        }
    }

    @Test
    public final void testDisallowedOperation() throws KmcKeyClientException {
        KmcKey key = kmipClient.createAsymmetricKeyPair(CREATOR,
                KEYPAIR_REF, KEY_ALGORITHM, KEY_LENGTH);
        assertNotNull(key);
        assertEquals(new EnumState(EnumState.PreActive).getKey(), key.getState());
        kmipClient.operateKeyPair(key.getKeyRef(), key.getKeyId(), "Revoke", "The key has been compromised");
        String state = kmipClient.getKeyPairState(KEYPAIR_REF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Compromised).getKey(), state);
        try {
            kmipClient.operateKeyPair(key.getKeyRef(), key.getKeyId(), "Activate", null);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
        }
    }

}
