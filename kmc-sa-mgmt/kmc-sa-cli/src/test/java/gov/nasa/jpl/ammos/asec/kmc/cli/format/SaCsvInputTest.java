package gov.nasa.jpl.ammos.asec.kmc.cli.format;

import static org.junit.jupiter.api.Assertions.*;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.format.SaCsvInput;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

/**
 * CSV input tests
 */
public class SaCsvInputTest {

    public static final String BULK_SA_FILE = "kmc-all-SAs.csv";

    @Test
    public void testParse() throws IOException, KmcException {
        SaCsvInput input = new SaCsvInput();

        InputStream inputStream = SaCsvInputTest.class.getClassLoader().getResourceAsStream(BULK_SA_FILE);
        if (inputStream == null) {
            fail("Test resource file '" + BULK_SA_FILE + "' not found");
        }

        try (Reader reader = new InputStreamReader(inputStream)) {
            List<ISecAssn> sas = input.parseCsv(reader, FrameType.TC);
            assertEquals(81, sas.size());
        }
    }

    @Test
    public void testParseHex() throws DecoderException {
        SaCsvInput i   = new SaCsvInput();
        String     hex = "0x00";
        byte[]     val = i.parseHex(hex);
        assertArrayEquals(new byte[]{0x00}, val);

        hex = "X'00'";
        val = i.parseHex(hex);
        assertArrayEquals(new byte[]{0x00}, val);

        hex = "00";
        val = i.parseHex(hex);
        assertArrayEquals(new byte[]{0x00}, val);

        hex = "";
        val = i.parseHex(hex);
        assertArrayEquals(new byte[]{}, val);

        hex = null;
        val = i.parseHex(hex);
        assertArrayEquals(new byte[]{}, val);
    }

    @Test
    public void testParseHexFail() {
        assertThrows(DecoderException.class, () -> {
            SaCsvInput i = new SaCsvInput();
            String hex = "0xG0";
            byte[] val = i.parseHex(hex);
        });
    }

    @Test
    public void testParseHexFail1() {
        assertThrows(DecoderException.class, () -> {
            SaCsvInput i = new SaCsvInput();
            String hex = "0";
            byte[] val = i.parseHex(hex);
        });
    }

}