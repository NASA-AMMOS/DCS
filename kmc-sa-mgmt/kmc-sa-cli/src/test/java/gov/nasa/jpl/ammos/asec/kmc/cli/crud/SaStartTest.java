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
 * Tests for starting SAs
 */
public class SaStartTest extends BaseCommandLineTest {

    public static final String TYPE_FMT = "--type=%s";
    public static final String SCID = "--scid";
    public static final String SPI = "--spi";

    @Test
    public void testStart() throws KmcException {
        testStart(FrameType.TC);
        testStart(FrameType.TM);
        testStart(FrameType.AOS);
    }

    public void testStart(FrameType type) throws KmcException {
        CommandLine cli  = getCmd(new SaStart(), true);
        int         exit = cli.execute(String.format(TYPE_FMT, type.name()));
        // no args
        assertNotEquals(0, exit);
        exit = cli.execute("--spi=2", "--scid=46", String.format(TYPE_FMT, type.name()));
        // already started
        assertNotEquals(0, exit);

        // start
        createExtraSas(type);
        exit = cli.execute(SPI, "8", SCID, "46", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        ISecAssn sa1 = dao.getSa(new SpiScid(8, (short) 46), type);
        ISecAssn sa2 = dao.getSa(new SpiScid(9, (short) 46), type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa1.getSaState());
        assertEquals(KmcDao.SA_UNKEYED, (short) sa2.getSaState());
    }

    @Test
    public void testStartAlreadyOperational() throws KmcException {
        testStartAlreadyOperational(FrameType.TC);
        testStartAlreadyOperational(FrameType.TM);
        testStartAlreadyOperational(FrameType.AOS);
    }

    public void testStartAlreadyOperational(FrameType type) throws KmcException {
        // start when already active per GVCID
        createExtraSas(type);
        dao.startSa(new SpiScid(8, (short) 46), false, type);
        CommandLine cli  = getCmd(new SaStart(), true);
        int         exit = cli.execute(SCID, "46", SPI, "9", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
    }

    @Test
    public void testStartForce() throws KmcException {
        testStartForce(FrameType.TC);
        testStartForce(FrameType.TM);
        testStartForce(FrameType.AOS);
    }

    public void testStartForce(FrameType type) throws KmcException {
        createExtraSas(type);
        dao.startSa(new SpiScid(8, (short) 46), false, type);
        CommandLine cli  = getCmd(new SaStart(), true);
        int         exit = cli.execute(SCID, "46", SPI, "9", "--force", String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exit);
    }

    @Test
    public void testStartMultiple() throws KmcException {
        testStartMultiple(FrameType.TC);
        testStartMultiple(FrameType.TM);
        testStartMultiple(FrameType.AOS);
    }

    public void testStartMultiple(FrameType type) throws KmcException {
        createExtraSas(type);
        CommandLine cli  = getCmd(new SaStart(), true);
        int         exit = cli.execute(SCID, "46", SPI, "8", SPI, "10", String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exit);
        ISecAssn sa1 = dao.getSa(new SpiScid(8, (short) 46), type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa1.getSaState());
        ISecAssn sa2 = dao.getSa(new SpiScid(10, (short) 46), type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa2.getSaState());
    }

    @Test
    public void testStartMultipleFail() throws KmcException {
        testStartMultipleFail(FrameType.TC);
        testStartMultipleFail(FrameType.TM);
        testStartMultipleFail(FrameType.AOS);
    }

    public void testStartMultipleFail(FrameType type) throws KmcException {
        createExtraSas(type);
        CommandLine cli  = getCmd(new SaStart(), true);
        int         exit = cli.execute(SCID, "46", SPI, "8", SPI, "9", String.format(TYPE_FMT,
                type.name()));
        assertNotEquals(0, exit);
        ISecAssn sa1 = dao.getSa(new SpiScid(8, (short) 46), type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa1.getSaState());
        ISecAssn sa2 = dao.getSa(new SpiScid(9, (short) 46), type);
        assertEquals(KmcDao.SA_UNKEYED, (short) sa2.getSaState());
    }

    private void createExtraSas(FrameType type) throws KmcException {
        dao.createSa(8, (byte) 0, (short) 46, (byte) 10, (byte) 0, type);
        dao.createSa(9, (byte) 0, (short) 46, (byte) 10, (byte) 0, type);
        dao.createSa(10, (byte) 0, (short) 46, (byte) 11, (byte) 0, type);
    }
}