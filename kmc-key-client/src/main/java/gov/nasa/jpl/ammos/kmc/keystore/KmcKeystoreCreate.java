package gov.nasa.jpl.ammos.kmc.keystore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;

import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;

/**
 * Application to add key to a keystore based on the following parameters:
 * 1) keystore file
 * 2) keystore type
 * 3) keystore password
 * 4) key password
 * 5) key reference
 * 6) key algorithm
 * 7) key material encoded in hex
 *
 *
 */
public final class KmcKeystoreCreate {

    public static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";
    public static final String KEYSTORE_TYPE_BCFKS = "BCFKS";

    /**
     * Application to add or update a key to the specified PKCS12 keystore.
     *
     * @param args keystorePath keystorePassword keyName keyMaterial
     */
    public static void main(final String[] args) {
        if (args.length != 7) {
            System.out.println("Incorrect number of arguments (" + args.length + ").  Expecting 7 arguments:");
            System.out.println("  keystorePath keystoreType keystorePassword keyPassword keyName keyAlgorithm keyMaterial");
            System.exit(1);
        }

        String keystoreFilename = args[0];
        String keystoreType = args[1];
        String keystorePassword = args[2];
        String keyPassword = args[3];
        String keyName = args[4];
        String keyAlgorithm = args[5];
        String keyMaterialHex = args[6];
        byte[] keyMaterial = null;

        if (keystoreFilename == null || keystoreType == null || keystorePassword == null ||
            keyName == null || keyPassword == null || keyAlgorithm == null || keyMaterialHex == null) {
            System.err.println("One of the arguments has null value.  Expecting 7 arguments:");
            System.err.println("  keystorePath keystoreType keystorePassword keyPassword keyName keyAlgorithm keyMaterial");
            System.exit(2);
        }

        // key algorithms are case insensitive
        if (! "AES".equalsIgnoreCase(keyAlgorithm) && ! keyAlgorithm.toUpperCase().startsWith("HMACSHA")) {
            System.err.println("Invalid key algorithm: " + keyAlgorithm + ". Only AES and HmacSHA keys supported");
            System.exit(2);
        }

        try {
            keyMaterial = Hex.decode(keyMaterialHex);
        } catch (DecoderException e) {
            System.err.println("Invalid hex format in key material: " + keyMaterialHex);
            System.exit(2);
        }

        KeyStore keystore = null;
        File keystoreFile = new File(keystoreFilename);
        try {
            if (KEYSTORE_TYPE_BCFKS.equals(keystoreType)) {
                Provider provider = new BouncyCastleFipsProvider();
                Security.addProvider(provider);
                keystore = KeyStore.getInstance(keystoreType, "BCFIPS");
            } else if (KEYSTORE_TYPE_PKCS12.equals(keystoreType)) {
                keystore = KeyStore.getInstance(keystoreType);
            } else {
                String msg = "Unsupported keystore type: " + keystoreType + ", support only PKCS12 and BCFKS keystores";
                System.err.println(msg);
                System.exit(2);
            }
        } catch (Exception e) {
            String msg = "Exception in creating keystore instance: " + e;
            System.err.println(msg);
            System.exit(3);
        }
        if (keystoreFile.exists()) {
            try {
                keystore.load(new FileInputStream(keystoreFile), keystorePassword.toCharArray());
            } catch (Exception e) {
                System.err.println("Failed to load keystore " + keystoreFilename + ", exception: " + e);
                if (e.getMessage().contains("Integrity check failed")) {
                    System.err.println("Failed integrity check could be caused by incorrect keystore password");
                }
                System.exit(1);
            }
        } else {
            try {
                keystore.load(null, null); // a new keystore
                keystore.store(new FileOutputStream(keystoreFile), keystorePassword.toCharArray());
            } catch (Exception e) {
                System.err.println("Failed to create keystore " + keystoreFilename + ", exception: " + e);
                System.exit(1);
            }
        }

        try {
            KeyStore.ProtectionParameter pp = new PasswordProtection(keyPassword.toCharArray());

            SecretKeySpec secretKey = new SecretKeySpec(keyMaterial, keyAlgorithm);
            KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(secretKey);
            keystore.setEntry(keyName, entry, pp);

            keystore.store(new FileOutputStream(keystoreFilename), keystorePassword.toCharArray());
            System.out.println("Added key " + keyName + ", " + keyAlgorithm + ", " + keyMaterialHex);
        } catch (KeyStoreException e) {
            System.err.println("Failed to store key entry to keystore: " + e);
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Failed to write key to keystore: " + e);
            System.exit(2);
        }
    }
}
