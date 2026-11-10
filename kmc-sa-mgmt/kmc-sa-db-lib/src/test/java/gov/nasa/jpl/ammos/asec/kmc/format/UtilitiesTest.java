package gov.nasa.jpl.ammos.asec.kmc.format;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilitiesTest {

    @Test
    public void testSanitizeForLogNull() {
        assertEquals("null", Utilities.sanitizeForLog(null));
    }

    @Test
    public void testSanitizeForLogCarriageReturn() {
        assertEquals("test_value", Utilities.sanitizeForLog("test\rvalue"));
    }

    @Test
    public void testSanitizeForLogLineFeed() {
        assertEquals("test_value", Utilities.sanitizeForLog("test\nvalue"));
    }

    @Test
    public void testSanitizeForLogTab() {
        assertEquals("test_value", Utilities.sanitizeForLog("test\tvalue"));
    }

    @Test
    public void testSanitizeForLogMultiple() {
        assertEquals("test_value_here", Utilities.sanitizeForLog("test\rvalue\nhere"));
    }

    @Test
    public void testSanitizeForLogClean() {
        assertEquals("test value", Utilities.sanitizeForLog("test value"));
    }

    @Test
    public void testSanitizeForLogOtherControlCharacters() {
        assertEquals("a_b_c", Utilities.sanitizeForLog("a\u001bb\u0000c"));
    }

    @Test
    public void testSanitizeForLogUnicodeLineSeparators() {
        assertEquals("a_b_c_d", Utilities.sanitizeForLog("a\u0085b\u2028c\u2029d"));
    }
}
