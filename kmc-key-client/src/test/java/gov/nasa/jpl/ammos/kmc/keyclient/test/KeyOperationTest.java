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
public class KeyOperationTest {

    private static final String CREATOR = "testuser3300";
    private static final String ENCRYPTION_KEYREF = "key-operation-test";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_LENGTH = 256;

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
        destroyKeys();
    }

    // in case the keys were not destroyed when the tests failed
    @AfterClass
    public static void tearDown() throws KmcKeyClientException {
        destroyKeys();
    }

    private static void destroyKeys() throws KmcKeyClientException {
        if (kmipClient.locateCryptoKey(ENCRYPTION_KEYREF) != null) {
            kmipClient.destroySymmetricKey(ENCRYPTION_KEYREF);
        }
    }

    @Test
    public final void testKeyOperation() throws KmcKeyClientException {
        KmcKey key = kmipClient.createEncryptionKey(CREATOR,
                ENCRYPTION_KEYREF, KEY_ALGORITHM, KEY_LENGTH);
        assertNotNull(key);
        assertEquals(new EnumState(EnumState.PreActive).getKey(), key.getState());
        kmipClient.operateKey(key.getKeyRef(), key.getKeyId(), "Activate", null);
        String state = kmipClient.getKeyState(ENCRYPTION_KEYREF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Active).getKey(), state);
        kmipClient.operateKey(key.getKeyRef(), key.getKeyId(), "Deactivate", "The key no long in use");
        state = kmipClient.getKeyState(ENCRYPTION_KEYREF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Deactivated).getKey(), state);
        kmipClient.operateKey(key.getKeyRef(), key.getKeyId(), "Revoke", "The key has been compromised");
        state = kmipClient.getKeyState(ENCRYPTION_KEYREF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Compromised).getKey(), state);
        kmipClient.operateKey(key.getKeyRef(), key.getKeyId(), "Destroy", null);
        state = kmipClient.getKeyState(ENCRYPTION_KEYREF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Destroyed).getKey(), state);
        assertNull(kmipClient.locateCryptoKey(ENCRYPTION_KEYREF));
    }

    @Test
    public final void testInvalidOperation() throws KmcKeyClientException {
        KmcKey key = kmipClient.createEncryptionKey(CREATOR,
                ENCRYPTION_KEYREF, KEY_ALGORITHM, KEY_LENGTH);
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
        KmcKey key = kmipClient.createEncryptionKey(CREATOR,
                ENCRYPTION_KEYREF, KEY_ALGORITHM, KEY_LENGTH);
        assertNotNull(key);
        assertEquals(new EnumState(EnumState.PreActive).getKey(), key.getState());
        kmipClient.operateKey(key.getKeyRef(), key.getKeyId(), "Revoke", "The key has been compromised");
        String state = kmipClient.getKeyState(ENCRYPTION_KEYREF, key.getKeyId());
        assertEquals(new EnumState(EnumState.Compromised).getKey(), state);
        try {
            kmipClient.operateKey(key.getKeyRef(), key.getKeyId(), "Activate", null);
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertEquals(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, e.getErrorCode());
        }
    }

}
