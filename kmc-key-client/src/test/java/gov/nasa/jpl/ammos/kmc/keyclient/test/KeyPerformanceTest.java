package gov.nasa.jpl.ammos.kmc.keyclient.test;

import static org.junit.Assert.assertNotNull;

import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClient;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKey;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientException;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManager;
import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManagerException;

/**
 * Measuring the performance of creating and retrieving symmetric and asymmetric keys.
 *
 *
 */
public class KeyPerformanceTest {

    private static final String CREATOR = "testuser3300";

    private static final int NUMBER_OF_KEYS_TO_CREATE = 3;   // 3x total keys (symmetric and key pair)
    private static final int NUMBER_OF_KEYS_TO_RETRIEVE = 2; // for averaging key retrieval time

    private static final String SYMMETRIC_KEY_ALGORITHM = "AES";
    private static final int SYMMETRIC_KEY_LENGTH = 256;
    private static final String SYMMETRIC_KEYNAME_HEAD = "kmc_test_perf_AES_";

    private static final String ASYMMETRIC_KEY_ALGORITHM = "RSA";
    private static final int ASYMMETRIC_KEY_LENGTH = 2048;
    private static final String ASYMMETRIC_KEYNAME_HEAD = "kmc_test_perf_RSA_";

    private static KmcKeyClientManager manager;
    private static KmcKeyClient kmipClient;
    private static Random random;

    private static final Logger logger = LoggerFactory.getLogger(KeyPerformanceTest.class);

    @BeforeClass
    public static final void setUp() throws KmcKeyClientManagerException, KmcKeyClientException  {
        manager = new KmcKeyClientManager(null);
        kmipClient =  manager.getKmcKmipKeyClient();
        random = new Random();
    }

    @AfterClass
    public static final void destroyKeys() throws KmcKeyClientException {
    }

    @Test
    public final void measureSymmetricKeyCreationAndRetrieval() throws KmcKeyClientException {
        String keyName;
        int keyCreationTime;
        int keyRetrievalTime;

        System.out.println("Number of Symmetric Keys\tKey Creation Time\tKey Retrieval Time");
        for (int i = 1; i <= NUMBER_OF_KEYS_TO_CREATE; i++) {
            keyName = SYMMETRIC_KEYNAME_HEAD + String.valueOf(i);
            keyCreationTime = createSymmetricKey(keyName);
            keyRetrievalTime = retrieveSymmetricKeys(i);
            System.out.format("%d\t%d\t%d\n", i, keyCreationTime, keyRetrievalTime);
        }
        // destroy keys
        for (int i = 1; i <= NUMBER_OF_KEYS_TO_CREATE; i++) {
            keyName = SYMMETRIC_KEYNAME_HEAD + String.valueOf(i);
            kmipClient.destroySymmetricKey(keyName);
        }
    }

    @Test
    public final void measureAsymmetricKeyCreationAndRetrieval() throws KmcKeyClientException {
        String keyName;
        int keyCreationTime;
        int keyRetrievalTime;

        System.out.println("Number of Key Pairs\tKey Creation Time\tKey Retrieval Time");
        for (int i = 1; i <= NUMBER_OF_KEYS_TO_CREATE; i++) {
            keyName = ASYMMETRIC_KEYNAME_HEAD + String.valueOf(i);
            keyCreationTime = createKeyPair(keyName);
            keyRetrievalTime = retrieveAsymmetricKeys(i);
            System.out.format("%d\t%d\t%d\n", i, keyCreationTime, keyRetrievalTime);
        }
        // destroy keys
        for (int i = 1; i <= NUMBER_OF_KEYS_TO_CREATE; i++) {
            keyName = ASYMMETRIC_KEYNAME_HEAD + String.valueOf(i);
            kmipClient.destroyAsymmetricKeyPair(keyName);
        }
    }

    private int createSymmetricKey(final String keyName) throws KmcKeyClientException {
        long startTime = System.currentTimeMillis();
        kmipClient.createEncryptionKey(CREATOR, keyName, SYMMETRIC_KEY_ALGORITHM, SYMMETRIC_KEY_LENGTH);
        long time = System.currentTimeMillis() - startTime;
        logger.info("Creation key " + keyName + " = " + time);
        return (int) time;
    }

    private int createKeyPair(final String keyName) throws KmcKeyClientException {
        long startTime = System.currentTimeMillis();
        kmipClient.createAsymmetricKeyPair(CREATOR, keyName, ASYMMETRIC_KEY_ALGORITHM, ASYMMETRIC_KEY_LENGTH);
        long time = System.currentTimeMillis() - startTime;
        logger.info("Creation key pair: " + keyName + " = " + time);
        return (int) time;
    }

    private int retrieveSymmetricKeys(final int iterations) throws KmcKeyClientException {
        long startTime;
        long time;
        long sum = 0;
        KmcKey symmetricKey;
        String keyName;

        for (int i = 1; i <= NUMBER_OF_KEYS_TO_RETRIEVE; i++) {
            int id = random.nextInt(iterations) + 1;
            keyName = SYMMETRIC_KEYNAME_HEAD + String.valueOf(id);
            startTime = System.currentTimeMillis();
            symmetricKey = kmipClient.getSymmetricKey(keyName);
            time = System.currentTimeMillis() - startTime;
            sum += time;
            logger.info("Get symmetric key " + keyName + " = " + time);
            assertNotNull(symmetricKey);
            //logger.info("Running sum " + sum);
        }
        int average = (int) sum / NUMBER_OF_KEYS_TO_RETRIEVE;
        logger.info("sum = " + sum + ", numberOfKeys = " + NUMBER_OF_KEYS_TO_RETRIEVE + ", average = " + average);
        return average;
    }

    private int retrieveAsymmetricKeys(final int iterations) throws KmcKeyClientException {
        long startTime;
        long time;
        long sum = 0;
        KmcKey publicKey;
        //PrivateKey privateKey;
        String keyName;

        for (int i = 1; i <= NUMBER_OF_KEYS_TO_RETRIEVE; i++) {
            int id = random.nextInt(iterations) + 1;
            keyName = ASYMMETRIC_KEYNAME_HEAD + String.valueOf(id);
            startTime = System.currentTimeMillis();
            publicKey = kmipClient.getPublicKey(keyName);
            time = System.currentTimeMillis() - startTime;
            sum += time;
            logger.info("Get public key " + keyName + " = " + time);
            assertNotNull(publicKey);
            /*
            startTime = System.currentTimeMillis();
            privateKey = kmClient.getPrivateKey(keyName);
            time = System.currentTimeMillis() - startTime;
            sum += time;
            logger.info("Get private key " + keyName + " = " + time);
            assertNotNull(privateKey);
            */
            //logger.info("Running sum " + sum);
        }
        int average = (int) sum / NUMBER_OF_KEYS_TO_RETRIEVE;
        logger.info("sum = " + sum + ", numberOfKeys = " + NUMBER_OF_KEYS_TO_RETRIEVE + ", average = " + average);
        return average;
    }

}
