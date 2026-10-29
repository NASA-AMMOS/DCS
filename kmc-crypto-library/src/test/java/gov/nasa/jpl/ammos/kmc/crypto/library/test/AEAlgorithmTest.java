package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Authenticated Encryption algorithms
 * using the Galois/Counter Mode (GCM) mode of operation.
 *
 *
 */
public class AEAlgorithmTest {
    private static final String BCFIPS_PROVIDER = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";
    private static final int AES_KEY_LENGTH = 256;  // bits
    private static final int GCM_IV_LENGTH = 12;    // bytes
    private static final int GCM_TAG_LENGTH = 16;   // bytes

    @BeforeAll
    public static void setUp() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> providerClass = Class.forName(BCFIPS_PROVIDER);
        Provider provider = (Provider) providerClass.newInstance();
        Security.addProvider(provider);
    }


    /**
     * Verify encryption and decryption using AES with GCM mode of operation
     * using data of Test Case 15 from McGrew/Viega GCM Spec.
     */
    @Test
    public final void testGCM() throws Exception {
        byte[] aK = Hex.decode(
                      "feffe9928665731c6d6a8f9467308308"
                    + "feffe9928665731c6d6a8f9467308308");
        byte[] aP = Hex.decode(
                      "d9313225f88406e5a55909c5aff5269a"
                    + "86a7a9531534f7da2e4c303d8a318a72"
                    + "1c3c0c95956809532fcf0e2449a6b525"
                    + "b16aedf5aa0de657ba637b391aafd255");
        byte[] aN = Hex.decode("cafebabefacedbaddecaf888");
        String aT = "b094dac5d93471bdec1a502270e3cc6c";
        byte[] aC = Hex.decode(
                      "522dc1f099567d07f47f37a32a84427d"
                    + "643a8cdcbfe5c0c97598a2bd2555d1aa"
                    + "8cb08e48590dbb3da7b08b1056828838"
                    + "c5f61e6393ba7a0abcc9f662898015ad" + aT);

        Key key;
        Cipher eCipher;
        Cipher dCipher;

        key = new SecretKeySpec(aK, "AES");

        eCipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        eCipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(aN));
        byte[] enc = eCipher.doFinal(aP);
        assertArrayEquals(enc, aC, "ciphertext doesn't match.");

        dCipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        dCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(aN));
        byte[] dec = dCipher.doFinal(aC);
        assertArrayEquals(dec, aP, "plaintext doesn't match.");

        try {
            eCipher = Cipher.getInstance("AES/GCM/PKCS5Padding", "BCFIPS");
            fail("bad padding missed in GCM");
        } catch (NoSuchPaddingException e) {
            //assertEquals("Only NoPadding can be used with AEAD modes.", e.getMessage());
        }
    }

    /**
     * Verify authenticated encryption with GCM with additional authenticated data (AAD).
     */
    @Test
    public final void testGcmAEAD() throws NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchProviderException {

        String inputString = "A string for GCM AEAD testing.";
        String aadString = "This is the non-encrypted part";
        byte[] input = inputString.getBytes();
        byte[] aad = aadString.getBytes();

        //SecureRandom random = SecureRandom.getInstanceStrong();
        SecureRandom random = new SecureRandom();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_LENGTH, random);
        SecretKey key = keyGen.generateKey();

        final byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

        // encrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        cipher.updateAAD(aad);
        byte[] cipherText = cipher.doFinal(input);

        // decrypt
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        cipher.updateAAD(aad);
        byte[] plainText = cipher.doFinal(cipherText);
        assertArrayEquals(input, plainText);

        // corrupt the ciphertext to cause exception
        cipherText[0]++;
        cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        cipher.updateAAD(aad);
        try {
            cipher.doFinal(cipherText);
            fail("Expected AEADBadTagException not received.");
        } catch (AEADBadTagException e) {
            String msg = e.getMessage();
            // JCE error message || BouncyCastle error message
            // Java 17 is "Tag mismatch!" and Java 21 is "Tag mismatch"
            System.err.println(msg);
            assertTrue(msg.startsWith("Tag mismatch") || msg.contains("mac check in GCM failed"));
        }
        cipherText[0]--;

        // corrupt the tag to cause exception
        cipherText[cipherText.length - 1]++;
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        cipher.updateAAD(aad);
        try {
            cipher.doFinal(cipherText);
            fail("Expected AEADBadTagException not received.");
        } catch (AEADBadTagException e) {
            String msg = e.getMessage();
            // JCE error message || BouncyCastle error message
            assertTrue(msg.startsWith("Tag mismatch") || msg.contains("mac check in GCM failed"));
        }
    }

    /**
     * Verify authenticated encryption with verifiable data.
     *
     * Verification data from
     * https://github.com/bcgit/bc-java/blob/master/prov/src/test/java/org/bouncycastle/jce/provider/test/AEADTest.java
     */
    @Test
    public final void testGcmAEAD2() throws Exception {
        // EAX test vector from EAXTest
        byte[] aK2 = Hex.decode("91945D3F4DCBEE0BF45EF52255F095A4");
        byte[] aN2 = Hex.decode("BECAF043B0A23D843194BA972C66DEBD");
        byte[] aA2 = Hex.decode("FA3BFD4806EB53FA");
        byte[] aP2 = Hex.decode("F7FB");
        byte[] aC2 = Hex.decode("19DD5C4C9331049D0BDAB0277408F67967E5");
        // C2 with only 64bit MAC (default for EAX)
        byte[] aC2short = Hex.decode("19DD5C4C9331049D0BDA");

        byte[] aKGCM = Hex.decode("00000000000000000000000000000000");
        byte[] aNGCM = Hex.decode("000000000000000000000000");
        byte[] aCGCM = Hex.decode("58e2fccefa7e3061367f1d57a4e7455a");

        checkCipherWithAD(aK2, aN2, aA2, aP2, aC2short);
        testGCMParameterSpec(aK2, aN2, aA2, aP2, aC2);
        testGCMGeneric(aKGCM, aNGCM, new byte[0], new byte[0], aCGCM);
        testGCMParameterSpecWithMultipleUpdates(aK2, aN2, aA2, aP2, aC2);

        testTampering(true);
    }

    private void testTampering(final boolean aeadAvailable)
            throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        Cipher eax = Cipher.getInstance("AES/EAX/NoPadding", "BCFIPS");
        final SecretKeySpec key = new SecretKeySpec(new byte[eax.getBlockSize()], eax.getAlgorithm());
        final IvParameterSpec iv = new IvParameterSpec(new byte[eax.getBlockSize()]);

        eax.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] ciphertext = eax.doFinal(new byte[100]);
        ciphertext[0] = (byte) (ciphertext[0] + 1); // Tamper

        try {
            eax.init(Cipher.DECRYPT_MODE, key, iv);
            eax.doFinal(ciphertext);
            fail("Tampered ciphertext should be invalid");
        } catch (BadPaddingException e) {
            if (aeadAvailable) {
                if (!e.getClass().getName().equals("javax.crypto.AEADBadTagException")) {
                    fail("Tampered AEAD ciphertext should fail with AEADBadTagException when available.");
                }
            }
        }
    }

    private void checkCipherWithAD(final byte[] aK, final byte[] aN, final byte[] aA, final byte[] aP, final byte[] aC)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException {
        Cipher eax = Cipher.getInstance("AES/EAX/NoPadding", "BCFIPS");
        SecretKeySpec key = new SecretKeySpec(aK, "AES");
        IvParameterSpec iv = new IvParameterSpec(aN);
        eax.init(Cipher.ENCRYPT_MODE, key, iv);

        eax.updateAAD(aA);
        byte[] c = eax.doFinal(aP);
        assertArrayEquals(aC, c, "JCE encrypt with additional data failed.");

        eax.init(Cipher.DECRYPT_MODE, key, iv);
        eax.updateAAD(aA);
        byte[] p = eax.doFinal(aC);
        assertArrayEquals(aP, p, "JCE decrypt with additional data failed.");
    }

    private void testGCMParameterSpec(final byte[] aK, final byte[] aN, final byte[] aA, final byte[] aP, final byte[] aC)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, IOException {
        Cipher eax = Cipher.getInstance("AES/EAX/NoPadding", "BCFIPS");
        SecretKeySpec key = new SecretKeySpec(aK, "AES");

        // GCMParameterSpec mapped to AEADParameters and overrides default MAC size
        GCMParameterSpec spec = new GCMParameterSpec(128, aN);
        eax.init(Cipher.ENCRYPT_MODE, key, spec);

        eax.updateAAD(aA);
        byte[] c = eax.doFinal(aP);
        assertArrayEquals(aC, c, "JCE encrypt with additional data and GCMParameterSpec failed.");

        eax.init(Cipher.DECRYPT_MODE, key, spec);
        eax.updateAAD(aA);
        byte[] p = eax.doFinal(aC);
        assertArrayEquals(aP, p, "JCE decrypt with additional data and GCMParameterSpec failed.");
    }

    private void testGCMParameterSpecWithMultipleUpdates(
            final byte[] aK, final byte[] aN, final byte[] aA, final byte[] aP, final byte[] aC)
            throws Exception {
        Cipher eax = Cipher.getInstance("AES/EAX/NoPadding", "BCFIPS");
        SecretKeySpec key = new SecretKeySpec(aK, "AES");
        SecureRandom random = new SecureRandom();

        // GCMParameterSpec mapped to AEADParameters and overrides default MAC size
        GCMParameterSpec spec = new GCMParameterSpec(128, aN);

        for (int i = 900; i != 1024; i++) {
            byte[] message = new byte[i];

            random.nextBytes(message);

            eax.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] out = new byte[eax.getOutputSize(i)];

            int offSet = 0;

            int count;
            for (count = 0; count < i / 21; count++) {
                offSet += eax.update(message, count * 21, 21, out, offSet);
            }

            offSet += eax.doFinal(message, count * 21, i - (count * 21), out, offSet);

            byte[] dec = new byte[i];
            int len = offSet;

            eax.init(Cipher.DECRYPT_MODE, key, spec);

            offSet = 0;
            for (count = 0; count < len / 10; count++) {
                offSet += eax.update(out, count * 10, 10, dec, offSet);
            }

            offSet += eax.doFinal(out, count * 10, len - (count * 10), dec, offSet);

            if (!Arrays.equals(message, dec) || offSet != message.length) {
                fail("message mismatch");
            }
        }
    }

    private void testGCMGeneric(
            final byte[] aK, final byte[] aN, final byte[] aA, final byte[] aP, final byte[] aC)
                    throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                    IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException,
                    NoSuchProviderException, IOException, InvalidParameterSpecException {
        Cipher eax = Cipher.getInstance("AES/GCM/NoPadding", "BCFIPS");
        SecretKeySpec key = new SecretKeySpec(aK, "AES");

        // GCMParameterSpec mapped to AEADParameters and overrides default MAC size
        GCMParameterSpec spec = new GCMParameterSpec(128, aN);
        eax.init(Cipher.ENCRYPT_MODE, key, spec);

        eax.updateAAD(aA);
        byte[] c = eax.doFinal(aP);
        assertArrayEquals(aC, c, "JCE encrypt with additional data and GCMParameterSpec failed.");

        eax = Cipher.getInstance("GCM", "BCFIPS");
        eax.init(Cipher.DECRYPT_MODE, key, spec);
        eax.updateAAD(aA);
        byte[] p = eax.doFinal(aC);
        assertArrayEquals(aP, p, "JCE decrypt with additional data and GCMParameterSpec failed.");
    }

}
