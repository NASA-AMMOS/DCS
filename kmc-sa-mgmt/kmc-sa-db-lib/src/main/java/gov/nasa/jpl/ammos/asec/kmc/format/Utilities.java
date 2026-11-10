package gov.nasa.jpl.ammos.asec.kmc.format;

import org.apache.commons.codec.binary.Hex;

/**
 * Utilities
 */
public class Utilities {
    private Utilities() {
    }

    /**
     * Hex encode a byte array. Used in velocity templates.
     *
     * @param o byte array
     * @return hex string
     */
    public static String getHex(byte[] o) {
        if (o == null) {
            return "";
        }
        return "0x" + Hex.encodeHexString(o);
    }

    /**
     * Sanitizes a value for logging to prevent log injection.
     * Replaces control characters (including CR, LF, TAB, ESC, NUL), NEL and the Unicode line/paragraph
     * separators with underscore.
     *
     * @param o object to sanitize
     * @return sanitized string
     */
    public static String sanitizeForLog(Object o) {
        if (o == null) {
            return "null";
        }
        return String.valueOf(o).replaceAll("[\\p{Cntrl}\r\n\t\u0085\u2028\u2029]", "_");
    }
}
