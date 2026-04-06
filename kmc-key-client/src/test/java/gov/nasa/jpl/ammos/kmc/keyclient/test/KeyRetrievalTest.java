package gov.nasa.jpl.ammos.kmc.keyclient.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import gov.nasa.jpl.ammos.kmc.keyclient.KmcKey;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientException;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManager;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManagerException;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKmipKeyClient;

/**
 * Unit tests for retrieving keys from a KMIP server.
 *
 */
public class KeyRetrievalTest {

    private static final String ENCRYPTION_KEYREF = "kmc/test/AES256";

    private static KmcKeyClientManager manager;
    private static KmcKmipKeyClient kmipClient;

    @BeforeClass
    public static void setUp() throws KmcKeyClientManagerException, KmcKeyClientException {
        manager = new KmcKeyClientManager(null);
        kmipClient = (KmcKmipKeyClient) manager.getKmcKmipKeyClient();
    }

    @Test
    public final void testLocateAllKeyIds() throws KmcKeyClientException {
        long startTime = System.currentTimeMillis();
        List<String> keyIds = kmipClient.locateAllKeyIds();
        long time = System.currentTimeMillis() - startTime;
        assertTrue(keyIds.size() >= 0);
        System.out.println("Time to locate " + keyIds.size() + " keys = " + time + " ms.");
        for (String id : keyIds) {
            System.out.println("keyId = " + id);
        }
    }

    @Test
    public final void testGetKmcKey() throws KmcKeyClientException {
        String keyId = kmipClient.locateCryptoKey(ENCRYPTION_KEYREF);
        assertNotNull(keyId);
        long startTime = System.currentTimeMillis();
        KmcKey key = kmipClient.getKeyById(keyId);
        long time = System.currentTimeMillis() - startTime;
        System.out.println("Time to retrieve attributes of one key = " + time + " ms.");
        assertEquals(keyId, key.getKeyId());
        System.out.println("key = " + key);
    }

    @Test
    public final void testGetAllKeys() throws KmcKeyClientException {
        long startTime = System.currentTimeMillis();
        List<KmcKey> keys = kmipClient.getAllKeys();
        long time = System.currentTimeMillis() - startTime;
        assertTrue(keys.size() >= 0);
        System.out.println("Time to retrieve attributes of " + keys.size() + " keys = " + time + " ms.");
        for (KmcKey key : keys) {
            System.out.println("key = " + key);
        }
    }

    /* Anomalies */

    @Test
    public final void testLocateNonExistKey() throws KmcKeyClientException {
        assertNull(kmipClient.locateCryptoKey("nonExistKeyRef"));
    }

    @Test
    public final void testGetNonExistKeyId() {
        try {
            kmipClient.getKeyById("nonExistKeyId");
            fail("Expected KmcKeyClientException not received.");
        } catch (KmcKeyClientException e) {
            assertTrue(e.getMessage().contains("Failed"));
        }
    }

}
