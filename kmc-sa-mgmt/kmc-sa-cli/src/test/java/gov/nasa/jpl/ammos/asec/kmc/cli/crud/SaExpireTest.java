package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SaExpireTest extends BaseCommandLineTest {

    public static final String TYPE_FMT = "--type=%s";
    public static final String SCID_46 = "--scid=46";
    public static final String SPI_1 = "--spi=1";
    private CommandLine cli;

    @Before
    public void setupTest() {
        cli = getCmd(new SaExpire(), true);
    }

    @Test
    public void testExpireNoConfirm() throws KmcException {
        testExpireNoConfirm(FrameType.AOS);
        testExpireNoConfirm(FrameType.TC);
        testExpireNoConfirm(FrameType.TM);
    }

    public void testExpireNoConfirm(FrameType type) throws KmcException {
        int exit = cli.execute(SPI_1, SCID_46, "-y", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        ISecAssn sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(1, (short) sa.getSaState());
    }

    @Test
    public void testExpireConfirm() throws KmcException {
        testExpireConfirm(FrameType.AOS);
        testExpireConfirm(FrameType.TC);
        testExpireConfirm(FrameType.TM);
    }

    public void testExpireConfirm(FrameType type) throws KmcException {
        InputStream old = System.in;
        InputStream in  = new ByteArrayInputStream("y".getBytes(StandardCharsets.UTF_8));
        System.setIn(in);
        int exit = cli.execute(SPI_1, SCID_46, String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        ISecAssn sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(1, (short) sa.getSaState());
        System.setIn(old);
    }

    @Test
    public void testExpireRefuse() throws KmcException {
        testExpireRefuse(FrameType.TC);
        testExpireRefuse(FrameType.AOS);
        testExpireRefuse(FrameType.TM);

    }

    public void testExpireRefuse(FrameType type) throws KmcException {
        InputStream old = System.in;
        InputStream in  = new ByteArrayInputStream("n".getBytes(StandardCharsets.UTF_8));
        System.setIn(in);
        int exit = cli.execute(SPI_1, SCID_46, String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        ISecAssn sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(3, (short) sa.getSaState());
        System.setIn(old);
    }

    @Test
    public void testExpireFail() {
        testExpireFail(FrameType.TM);
        testExpireFail(FrameType.AOS);
        testExpireFail(FrameType.TC);
    }

    public void testExpireFail(FrameType type) {
        int exit = cli.execute("--spi=8", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
        exit = cli.execute(SCID_46, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
        exit = cli.execute("--spi=8", SCID_46, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
    }
}