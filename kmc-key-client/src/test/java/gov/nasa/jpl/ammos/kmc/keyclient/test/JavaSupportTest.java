package gov.nasa.jpl.ammos.kmc.keyclient.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Enumeration;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.junit.Test;

/**
 * Tests for verifying key algorithms and lengths that are supported by Java.
 */
public class JavaSupportTest {

    @Test
    public final void testCreateAESKeys() throws NoSuchAlgorithmException {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        int[] keyLengths = new int[] {128, 192, 256};
        for (int keyLength : keyLengths) {
            gen.init(keyLength);
            SecretKey key = gen.generateKey();
            assertNotNull(key);
            assertEquals("AES", key.getAlgorithm());
            assertEquals(keyLength, (long) key.getEncoded().length * 8);
        }
    }

    @Test
    public final void testInvalidAESKeys() throws NoSuchAlgorithmException {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        int[] keyLengths = new int[] {384, 512};
        for (int keyLength : keyLengths) {
            try {
                gen.init(keyLength);
                fail("Expected InvalidParameterException not received.");
            } catch (Exception e) {
                assertTrue(InvalidParameterException.class.equals(e.getClass()));
                assertTrue(e.getMessage().equals("Wrong keysize: must be equal to 128, 192 or 256"));
            }
        }
    }

    @Test
    public final void testCreateHmacKeys() throws NoSuchAlgorithmException {
        String[] algNames = new String[] {"HmacSHA1", "HmacSHA256", "HmacSHA384", "HmacSHA512"};
        int[] keyLengths = new int[] {128, 192, 256, 384, 512};
        for (String algName : algNames) {
            KeyGenerator gen = KeyGenerator.getInstance(algName);
            for (int keyLength : keyLengths) {
                gen.init(keyLength);
                SecretKey key = gen.generateKey();
                assertNotNull(key);
                assertEquals(algName, key.getAlgorithm());
                assertEquals(keyLength, (long) key.getEncoded().length * 8);
            }
        }
    }

    @Test
    public final void testOtherHmacNames() throws NoSuchAlgorithmException {
        KeyGenerator gen = KeyGenerator.getInstance("HMACSHA256");
        gen.init(128);
        SecretKey key = gen.generateKey();
        assertNotNull(key);
        assertEquals("HmacSHA256", key.getAlgorithm());
        assertEquals(128, key.getEncoded().length * 8);

        gen = KeyGenerator.getInstance("hmacsha256");
        gen.init(512);
        key = gen.generateKey();
        assertNotNull(key);
        assertEquals("HmacSHA256", key.getAlgorithm());
        assertEquals(512, (long) key.getEncoded().length * 8);

        // HmacSHA224 fails Java 7 but ok with Java 8
        String[] invalidHmacs = new String[] {"HMAC_SHA256", "HMAC-SHA256"};
        for (String name : invalidHmacs) {
            try {
                gen = KeyGenerator.getInstance(name);
                fail("Expected NoSuchAlgorithmException not received.");
            } catch (Exception e) {
                assertTrue(NoSuchAlgorithmException.class.equals(e.getClass()));
            }
        }
    }

    @Test(expected = NoSuchAlgorithmException.class)
    public final void testCreateRSAKey() throws NoSuchAlgorithmException {
        KeyGenerator.getInstance("RSA");
    }

    @Test
    public final void testCreateRSA2048KeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        byte[] privateKey = keyGen.genKeyPair().getPublic().getEncoded();
        assertTrue(privateKey.length > 0);
        byte[] publicKey = keyGen.genKeyPair().getPublic().getEncoded();
        assertTrue(publicKey.length > 0);
        assertEquals(privateKey.length, publicKey.length);
        assertEquals(294, privateKey.length);
    }

    @Test
    public final void testCreateRSA4096KeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        byte[] privateKey = keyGen.genKeyPair().getPublic().getEncoded();
        assertTrue(privateKey.length > 0);
        byte[] publicKey = keyGen.genKeyPair().getPublic().getEncoded();
        assertTrue(publicKey.length > 0);
        assertEquals(privateKey.length, publicKey.length);
        assertEquals(550, privateKey.length);
    }

    @Test(expected = NoSuchProviderException.class)
    public final void testGCM() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        gen.init(128);
        SecretKey key = gen.generateKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());

        byte[] ivBytes = new byte[128];
        Random random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        random.nextBytes(ivBytes);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        //GCMParameterSpec s = new GCMParameterSpec(128, ivBytes);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        assertEquals("AES", cipher.getAlgorithm());
    }

    @Test
    public final void printDefaultProviders() {
        try {
            for (Provider provider : Security.getProviders()) {
                System.out.println(provider);
                for (Enumeration<Object> e = provider.keys(); e.hasMoreElements();) {
                    System.out.println("\t" + e.nextElement());
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
