package gov.nasa.jpl.ammos.kmc.keyclient;

import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientVersion;

/**
 * This Java program prints the KMC Key Management API version information and can be run by the command:
 * <code>java -jar kmc-keymgt.jar</code>.
 *
 *
 */
public final class KmcKeyClientVersion {

    /**
     * This private constructor is used to silence the checkstyle warning:
     * Utility classes should not have a public or default constructor.
     */
    private KmcKeyClientVersion() {
        // not called
     }

    /**
     * The main program prints the KMC Key Management API version information.
     * @param args command-line arguments are not used.
     */
    public static void main(final String[] args) {
        System.out.println("AMMOS Application Security (ASEC)");
        System.out.println("Key Management & Cryptography (KMC)");
        System.out.println("KMC Key Management API");
        try {
            URL res = KmcKeyClientVersion.class.getResource(
                KmcKeyClientVersion.class.getSimpleName() + ".class");
            JarURLConnection conn = (JarURLConnection) res.openConnection();
            Manifest mf = conn.getManifest();
            Attributes attrs = mf.getMainAttributes();
            System.out.println("Version: " + attrs.getValue("Version"));
            System.out.println("Built-Time: " + attrs.getValue("Built-Time"));
            String copyright = attrs.getValue("Copyright");
            if (copyright != null) {
                System.out.println(copyright);
            }
        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }
    }

}
