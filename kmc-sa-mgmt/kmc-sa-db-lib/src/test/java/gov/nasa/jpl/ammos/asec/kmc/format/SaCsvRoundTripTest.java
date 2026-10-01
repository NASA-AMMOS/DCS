package gov.nasa.jpl.ammos.asec.kmc.format;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnFactory;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * csv import/export roundtrip tests
 */
public class SaCsvRoundTripTest {

    private List<ISecAssn> roundTrip(ISecAssn sa) throws KmcException {
        StringWriter w = new StringWriter();
        try (PrintWriter pw = new PrintWriter(w)) {
            new SaCsvOutput(true).print(pw, List.of(sa));
        }
        String csv = w.toString();
        return new SaCsvInput().parseCsv(new StringReader(csv), FrameType.ALL);
    }

    @Test
    public void testRoundTripBlankArsnAndIv() throws KmcException {
        ISecAssn sa = SecAssnFactory.createSecAssn(new SpiScid(1, (short) 44), FrameType.TC);
        sa.setTfvn((byte) 0);
        sa.setVcid((byte) 0);
        sa.setMapid((byte) 0);
        sa.setArsnLen((short) 0);
        sa.setArsn(new byte[]{});
        sa.setIv((short) 0, new byte[]{});

        List<ISecAssn> imported = roundTrip(sa);
        assertEquals(1, imported.size());
        ISecAssn reimported = imported.get(0);

        assertNotNull("ARSN must never be null after a round trip", reimported.getArsn());
        assertArrayEquals(new byte[]{}, reimported.getArsn());
        assertEquals((short) 0, (short) reimported.getArsnLen());

        assertNotNull("IV must never be null after a round trip", reimported.getIv());
        assertArrayEquals(new byte[]{}, reimported.getIv());
        assertEquals((short) 0, (short) reimported.getIvLen());
    }

    @Test
    public void testRoundTripPreservesEcsLenAndAcsLen() throws KmcException {
        ISecAssn sa = SecAssnFactory.createSecAssn(new SpiScid(2, (short) 44), FrameType.TC);
        sa.setTfvn((byte) 0);
        sa.setVcid((byte) 0);
        sa.setMapid((byte) 0);
        // non-default lengths, to catch the previous hardcoded ecsLen/acsLen = 1 bug
        sa.setEcs((short) 2, new byte[]{0x01, 0x02});
        sa.setAcs((short) 4, new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD});

        List<ISecAssn> imported = roundTrip(sa);
        assertEquals(1, imported.size());
        ISecAssn reimported = imported.get(0);

        assertEquals("ecs_len must survive the CSV round trip", (short) 2, (short) reimported.getEcsLen());
        assertArrayEquals(new byte[]{0x01, 0x02}, reimported.getEcs());
        assertEquals("acs_len must survive the CSV round trip", (short) 4, (short) reimported.getAcsLen());
        assertArrayEquals(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD}, reimported.getAcs());
    }

    @Test
    public void testRoundTripStable() throws KmcException {
        // exporting an already-reimported SA should produce byte-for-byte identical CSV (idempotent round trip)
        ISecAssn sa = SecAssnFactory.createSecAssn(new SpiScid(3, (short) 44), FrameType.AOS);
        sa.setTfvn((byte) 1);
        sa.setVcid((byte) 2);
        sa.setMapid((byte) 0);
        sa.setEcs((short) 2, new byte[]{0x01, 0x02});
        sa.setAcs((short) 4, new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD});
        sa.setArsnLen((short) 0);
        sa.setArsn(new byte[]{});

        StringWriter w1 = new StringWriter();
        try (PrintWriter pw = new PrintWriter(w1)) {
            new SaCsvOutput(true).print(pw, List.of(sa));
        }
        List<ISecAssn> imported = new SaCsvInput().parseCsv(new StringReader(w1.toString()), FrameType.ALL);

        StringWriter w2 = new StringWriter();
        try (PrintWriter pw = new PrintWriter(w2)) {
            new SaCsvOutput(true).print(pw, imported);
        }
        assertEquals(w1.toString(), w2.toString());
    }
}
