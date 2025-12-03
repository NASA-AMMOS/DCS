package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;

import javax.crypto.SecretKey;

import org.junit.BeforeClass;
import org.junit.Test;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.library.KeyServiceClient;
import gov.nasa.jpl.ammos.kmc.crypto.library.KeystoreKeyServiceClient;

/**
 * Unit tests for retrieving secret, public, and private keys from keystores
 * from KEYSTORE_TYPE_JCEKS and KEYSTORE_TYPE_JKS.
 *
 *
 */
public class KeystoreKeyServiceClientTest {
    //Make sure the keystore is present for test cases

    private static final String KEYSTORE_PASS = "kmcstorepass";
    private static final String KEYSTORE_KEYPASS = "kmckeypass";

    private static  String SYMMETRIC_KEYSTORE; //Initialize this in setUp()
    private static final String SYMMETRIC_TYPE = KeystoreKeyServiceClient.KEYSTORE_TYPE_JCEKS;
    private static String ASYMMETRIC_KEYSTORE;
    private static final String ASYMMETRIC_TYPE = KeystoreKeyServiceClient.KEYSTORE_TYPE_JKS;

    private static final String SYMMETRIC_KEYREF = "kmc/test/AES128";
    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static final int SYMMETRIC_LENGTH = 128;

    private static final String ASYMMETRIC_KEYREF = "kmc/test/RSA2048";
    private static final String ASYMMETRIC_ALGORITHM = "RSA";


    @BeforeClass
    public static void setUp() throws KmcCryptoManagerException {
        ClassLoader loader = KeystoreKeyServiceClientTest.class.getClassLoader();
        SYMMETRIC_KEYSTORE = loader.getResource("symmetric-keys.jck").getFile();
        ASYMMETRIC_KEYSTORE = loader.getResource("asymmetric-keys.jks").getFile();
    }

    @Test
    public final void testRetrieveSecretKey() throws KmcCryptoException {
        KeystoreKeyServiceClient localKeystore = new KeystoreKeyServiceClient(
                SYMMETRIC_KEYSTORE, KEYSTORE_PASS, SYMMETRIC_TYPE);

        SecretKey key = (SecretKey) localKeystore.getCryptoKey(
                SYMMETRIC_KEYREF, KEYSTORE_KEYPASS, KeyServiceClient.USAGE_MASK_SYMMETRIC_CRYPTO);
        assertNotNull(key);
        assertEquals(SYMMETRIC_ALGORITHM, key.getAlgorithm());
        assertEquals(SYMMETRIC_LENGTH, (long) key.getEncoded().length * 8);
    }

    @Test
    public final void testRetrievePublicKey() throws KmcCryptoException {
        KeystoreKeyServiceClient localKeystore = new KeystoreKeyServiceClient(
                ASYMMETRIC_KEYSTORE, KEYSTORE_PASS, ASYMMETRIC_TYPE);

        PublicKey key = (PublicKey) localKeystore.getCryptoKey(ASYMMETRIC_KEYREF, KEYSTORE_KEYPASS,
                KeyServiceClient.USAGE_MASK_ENCRYPT);
        assertNotNull(key);
        assertEquals(ASYMMETRIC_ALGORITHM, key.getAlgorithm());
    }

    @Test
    public final void testRetrievePrivateKey() throws KmcCryptoException {
        KeystoreKeyServiceClient localKeystore = new KeystoreKeyServiceClient(
                ASYMMETRIC_KEYSTORE, KEYSTORE_PASS, ASYMMETRIC_TYPE);

        PrivateKey key = (PrivateKey) localKeystore.getCryptoKey(ASYMMETRIC_KEYREF, KEYSTORE_KEYPASS,
                KeyServiceClient.USAGE_MASK_DECRYPT);
        assertNotNull(key);
        assertEquals(ASYMMETRIC_ALGORITHM, key.getAlgorithm());
    }

    @Test
    public final void testBadKeystoreLocation() {
        try {
            new KeystoreKeyServiceClient("bad_keystore_location", KEYSTORE_PASS, SYMMETRIC_TYPE);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("No such file or directory"));
            assertTrue(e.getCause().getClass() == FileNotFoundException.class);
        }
    }

    @Test
    public final void testNullKeystorePass() {
        try {
            new KeystoreKeyServiceClient(SYMMETRIC_KEYSTORE, null, SYMMETRIC_TYPE);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("Keystore was tampered with, or password was incorrect"));
            assertTrue(e.getCause().getClass() == IOException.class);
        }
    }

    @Test
    public final void testBadKeystorePass() {
        try {
            new KeystoreKeyServiceClient(SYMMETRIC_KEYSTORE, "bad_keystore_pass", SYMMETRIC_TYPE);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("Keystore was tampered with, or password was incorrect"));
            assertTrue(e.getCause().getClass() == IOException.class);
        }
    }

    @Test
    public final void testBadKeystoreType() {
        try {
            new KeystoreKeyServiceClient(SYMMETRIC_KEYSTORE, KEYSTORE_PASS,
                    KeystoreKeyServiceClient.KEYSTORE_TYPE_JKS);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("Invalid keystore format"));
            assertTrue(e.getCause().getClass() == IOException.class);
        }
        // Comment out as the expected KmcCryptoException is received in Java 8 but not in Java 11.
        // Java 11 can use any keystore type and be able to retrieve the keys from the keystore.
        //try {
        //    new KeystoreKeyServiceClient(KEYSTORE_PATH + ASYMMETRIC_KEYSTORE, KEYSTORE_PASS,
        //            KeystoreKeyServiceClient.KEYSTORE_TYPE_PKCS12);
        //    fail("Expected KmcCryptoException not received.");
        //} catch (KmcCryptoException e) {
        //    assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
        //    assertTrue(e.getMessage().contains("DerInputStream"));
        //    assertTrue(e.getCause().getClass() == IOException.class);
        //}
    }

    @Test
    public final void testBadKeyRef() throws KmcCryptoException {
        KeystoreKeyServiceClient localKeystore = new KeystoreKeyServiceClient(
                SYMMETRIC_KEYSTORE, KEYSTORE_PASS, SYMMETRIC_TYPE);
        String badKeyRef = "badKeyRef";
        try {
            localKeystore.getCryptoKey(
                    badKeyRef, KEYSTORE_KEYPASS, KeyServiceClient.USAGE_MASK_SYMMETRIC_CRYPTO);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("does not exist"));
        }
    }

    @Test
    public final void testBadKeyPass() throws KmcCryptoException {
        KeystoreKeyServiceClient localKeystore = new KeystoreKeyServiceClient(
                SYMMETRIC_KEYSTORE, KEYSTORE_PASS, SYMMETRIC_TYPE);
        try {
            localKeystore.getCryptoKey(
                    SYMMETRIC_KEYREF, "badKeyPass", KeyServiceClient.USAGE_MASK_SYMMETRIC_CRYPTO);
            fail("Expected KmcCryptoException not received.");
        } catch (KmcCryptoException e) {
            assertEquals(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("Given final block not properly padded"));
            assertTrue(e.getCause().getClass() == UnrecoverableKeyException.class);
        }
    }

}
