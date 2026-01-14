package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.sadb.KmcDao;
import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests for stopping SAs
 */
public class SaStopTest extends BaseCommandLineTest {

    public static final String TYPE_FMT = "--type=%s";
    public static final String SPI = "--spi";
    public static final String SCID_40 = "--scid=40";
    public static final String SPI_1 = "--spi=1";

    @Test
    public void testStop() throws KmcException {
        testStop(FrameType.TC);
        testStop(FrameType.TM);
        testStop(FrameType.AOS);
    }

    public void testStop(FrameType type) throws KmcException {
        SpiScid     id   = new SpiScid(1, (short) 46);
        CommandLine cli  = getCmd(new SaStop(), true);
        int         exit = cli.execute(String.format(TYPE_FMT, type.name()));
        // no args
        assertNotEquals(0, exit);
        ISecAssn sa = dao.getSa(id, type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa.getSaState());
        exit = cli.execute("--scid", "46", SPI, "1", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals(KmcDao.SA_KEYED, (short) sa.getSaState());
    }

    @Test
    public void testStopMultiple() throws KmcException {
        testStopMultiple(FrameType.TC);
        testStopMultiple(FrameType.TM);
        testStopMultiple(FrameType.AOS);
    }

    public void testStopMultiple(FrameType type) throws KmcException {
        SpiScid  id1 = new SpiScid(1, (short) 46);
        ISecAssn sa1 = dao.getSa(id1, type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa1.getSaState());
        SpiScid  id2 = new SpiScid(2, (short) 46);
        ISecAssn sa2 = dao.getSa(id2, type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa2.getSaState());

        CommandLine cli  = getCmd(new SaStop(), true);
        int         exit = cli.execute("--scid", "46", SPI, "1", SPI, "2", String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exit);
        sa1 = dao.getSa(id1, type);
        assertEquals(KmcDao.SA_KEYED, (short) sa1.getSaState());
        sa2 = dao.getSa(id2, type);
        assertEquals(KmcDao.SA_KEYED, (short) sa2.getSaState());
    }

    @Test
    public void testStopDne() {
        testStopDne(FrameType.TC);
        testStopDne(FrameType.TM);
        testStopDne(FrameType.AOS);
    }

    public void testStopDne(FrameType type) {
        CommandLine cli  = getCmd(new SaStop(), true);
        int         exit = cli.execute(SCID_40, SPI_1, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
    }

    @Test
    public void testAlreadyStopped() throws KmcException {
        testAlreadyStopped(FrameType.TC);
        testAlreadyStopped(FrameType.TM);
        testAlreadyStopped(FrameType.AOS);
    }

    public void testAlreadyStopped(FrameType type) throws KmcException {
        SpiScid  id1 = new SpiScid(1, (short) 46);
        ISecAssn sa1 = dao.getSa(id1, type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa1.getSaState());
        dao.stopSa(id1, type);
        sa1 = dao.getSa(id1, type);
        assertEquals(KmcDao.SA_KEYED, (short) sa1.getSaState());
        CommandLine cli  = getCmd(new SaStop(), true);
        int         exit = cli.execute(SCID_40, SPI_1, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
    }

}