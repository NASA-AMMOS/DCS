package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for Integrity Check using CMAC algorithm from Bouncy Castle.
 *
 *
 */
public class CmasAlgorithmTest {
    private static final String BCFIPS_PROVIDER = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";

    @BeforeAll
    public static void setUp() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?>  providerClass = Class.forName(BCFIPS_PROVIDER);
        Provider provider = (Provider) providerClass.newInstance();
        Security.addProvider(provider);
    }

    /**
     * Test CMAC with AES-128 key.
     *
     * Verification data from
     * https://github.com/bcgit/bc-java/blob/master/prov/src/test/java/org/bouncycastle/jce/provider/test/CMacTest.java
     */
    @Test
    public final void verifyCmacAES128() throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, ShortBufferException, IllegalStateException {

        byte[] keyBytes128 = Hex.decode("2b7e151628aed2a6abf7158809cf4f3c");
        Mac mac = Mac.getInstance("AESCMAC", "BCFIPS");
        SecretKeySpec key = new SecretKeySpec(keyBytes128, "AES");
        mac.init(key);

        byte[] input0 = Hex.decode("");
        byte[] outputK128M0 = Hex.decode("bb1d6929e95937287fa37d129b756746");
        mac.update(input0, 0, input0.length);
        byte[] out0 = new byte[mac.getMacLength()];
        mac.doFinal(out0, 0);
        assertArrayEquals(outputK128M0, out0);

        byte[] input16 = Hex.decode("6bc1bee22e409f96e93d7e117393172a");
        byte[] outputK128M16 = Hex.decode("070a16b46b4d4144f79bdd9dd04a287c");
        mac.update(input16, 0, input16.length);
        byte[] out16 = new byte[mac.getMacLength()];
        mac.doFinal(out16, 0);
        assertArrayEquals(outputK128M16, out16);
    }

    /**
     * Test CMAC with AES-256 key.
     *
     * Verification data from
     * https://github.com/bcgit/bc-java/blob/master/prov/src/test/java/org/bouncycastle/jce/provider/test/CMacTest.java
     */
    @Test
    public final void verifyCmacAES256() throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, ShortBufferException, IllegalStateException {

        byte[] keyBytes256 = Hex.decode("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4");
        Mac mac = Mac.getInstance("AESCMAC", "BCFIPS");
        SecretKeySpec key = new SecretKeySpec(keyBytes256, "AES");
        mac.init(key);

        byte[] input0 = Hex.decode("");
        byte[] outputK256M0 = Hex.decode("028962f61b7bf89efc6b551f4667d983");
        mac.update(input0, 0, input0.length);
        byte[] out0 = new byte[mac.getMacLength()];
        mac.doFinal(out0, 0);
        assertArrayEquals(outputK256M0, out0);

        byte[] input16 = Hex.decode("6bc1bee22e409f96e93d7e117393172a");
        byte[] outputK256M16 = Hex.decode("28a7023f452e8f82bd4bf28d8c37c35c");
        mac.update(input16, 0, input16.length);
        byte[] out16 = new byte[mac.getMacLength()];
        mac.doFinal(out16, 0);
        assertArrayEquals(outputK256M16, out16);
    }

}
