package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ServiceType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.sadb.KmcDao;
import org.junit.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for creating an SA
 */
public class SaCreateTest extends BaseCommandLineTest {

    public static final String BULK_SA_FILE   = "kmc-all-SAs.csv";
    public static final String BULK_SA_FILE_2 = "kmc-all-SAs-type.csv";
    public static final String TYPE_FMT = "--type=%s";
    public static final String FILE_FMT = "--file=%s";
    public static final String TFVN_0 = "--tfvn 0";
    public static final String MAPID_0 = "--mapid=0";
    public static final String TFVN_EQ_0 = "--tfvn=0";
    public static final String SCID_46 = "--scid=46";
    public static final String VCID_0 = "--vcid=0";
    public static final String SPI_7 = "--spi=7";
    public static final String EKID_140 = "--ekid=140";
    public static final String SPI_8 = "--spi=8";
    public static final String ST_AUTHENTICATED_ENCRYPTION = "--st=AUTHENTICATED_ENCRYPTION";
    public static final String ECS_0_X_02 = "--ecs=0x02";
    public static final String IVLEN_16 = "--ivlen=16";
    public static final String EKID_130 = "--ekid=130";
    public static final String SPI_EQ_6 = "--spi=6";
    public static final String ECS_0_X_01 = "--ecs=0x01";
    public static final String IVLEN_12 = "--ivlen=12";
    public static final String ABMLEN_20 = "--abmlen=20";

    @Test
    public void testCreateSasBulkFail() {
        createSasBulkFail(FrameType.TC);
        createSasBulkFail(FrameType.TM);
        createSasBulkFail(FrameType.AOS);
    }

    public void createSasBulkFail(FrameType type) {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(String.format(FILE_FMT, BULK_SA_FILE), String.format(TYPE_FMT,
                type.name()));
        assertNotEquals(0, exitCode);
    }

    @Test
    public void testCreateSasBulkFailDupe() throws KmcException {
        createSasBulkFailDupe(FrameType.TC);
        createSasBulkFailDupe(FrameType.TM);
        createSasBulkFailDupe(FrameType.AOS);
    }

    public void createSasBulkFailDupe(FrameType type) throws KmcException {
        StringWriter w   = new StringWriter();
        PrintWriter  err = new PrintWriter(w);
        StringWriter o   = new StringWriter();
        PrintWriter  out = new PrintWriter(o);
        dao.createSa(20, (byte) 0, (short) 44, (byte) 20, (byte) 0, type);
        assertEquals(6, dao.getSas(type).size());
        CommandLine cmd = getCmd(new SaCreate(), true, out, err);
        int exitCode = cmd.execute(String.format(FILE_FMT,
                getClass().getClassLoader().getResource(BULK_SA_FILE).getFile()), String.format(TYPE_FMT,
                type.name()));

        assertEquals(0, exitCode);
        assertTrue("Incorrect error message: " + w, w.toString().contains("SA create failed: an SA with " + "the " +
                "SPI/SCID combination 20/44 already exists"));
    }

    @Test
    public void testCreateSasBulk() throws KmcException {
        createSasBulk(FrameType.TC);
        createSasBulk(FrameType.AOS);
        createSasBulk(FrameType.TM);
    }

    public void createSasBulk(FrameType type) throws KmcException {
        StringWriter w   = new StringWriter();
        PrintWriter  err = new PrintWriter(w);
        StringWriter o   = new StringWriter();
        PrintWriter  out = new PrintWriter(o);
        CommandLine  cmd = getCmd(new SaCreate(), true, out, err);
        int exitCode = cmd.execute(String.format(FILE_FMT,
                getClass().getClassLoader().getResource(BULK_SA_FILE).getFile()), String.format(TYPE_FMT,
                type.name()));
        assertEquals(w.toString(), 0, exitCode);
        List<ISecAssn> sas = dao.getSas(type);
        int expectedSize = 0;
        switch (type) {
            case AOS:
                expectedSize = 85;
                break;
            case TM:
                expectedSize = 84;
                break;
            case TC:
            default:
                expectedSize = 86;
                break;
        }
        assertEquals(type.name() + " " + w, expectedSize, sas.size());
    }

    @Test
    public void testCreateSasBulkByType() throws KmcException, SQLException {
        afterTest();
        testCreateSasBulkType(FrameType.TC, 32);
        afterTest();
        testCreateSasBulkType(FrameType.TM, 32);
        afterTest();
        testCreateSasBulkType(FrameType.AOS, 17);
        afterTest();
        testCreateSasBulkType(FrameType.ALL, 81);
        List<ISecAssn> sas = dao.getSas(FrameType.ALL);
        List<ISecAssn> tc =
                sas.stream().filter(sa -> sa.getType() == FrameType.TC).toList();
        assertEquals("bulk ALL tc records", 32, tc.size());
        List<ISecAssn> tm = sas.stream().filter(sa -> sa.getType() == FrameType.TM).toList();
        assertEquals("bulk ALL tm records", 32, tm.size());
        List<ISecAssn> aos = sas.stream().filter(sa -> sa.getType() == FrameType.AOS).toList();
        assertEquals("bulk ALL aos records", 17, aos.size());
        afterTest();
    }

    public void testCreateSasBulkType(FrameType type, int expected) throws KmcException {
        StringWriter w   = new StringWriter();
        PrintWriter  err = new PrintWriter(w);
        StringWriter o   = new StringWriter();
        PrintWriter  out = new PrintWriter(o);
        CommandLine  cmd = getCmd(new SaCreate(), true, out, err);
        int exitCode = cmd.execute(String.format(FILE_FMT,
                getClass().getClassLoader().getResource(BULK_SA_FILE_2).getFile()), String.format(TYPE_FMT,
                type.name()));
        assertEquals(w.toString(), 0, exitCode);
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(w.toString(), expected, sas.size());
    }

    @Test
    public void testCreateSaFail() {
        createSaFail(FrameType.AOS);
        createSaFail(FrameType.TM);
        createSaFail(FrameType.TC);
    }

    public void createSaFail(FrameType type) {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);

        int exitCode = cmd.execute(TFVN_0, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_0, "--scid 44", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_0, "--scid 44", "--vcid 0", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
    }

    @Test
    public void testCreateSa() throws KmcException {
        createSa(FrameType.AOS);
        createSa(FrameType.TC);
        createSa(FrameType.TM);
    }

    public void createSa(FrameType type) throws KmcException {

        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exitCode);

        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertNotNull(sa);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_7, String.format(TYPE_FMT
                , type.name()));
        assertEquals(0, exitCode);

        sa = dao.getSa(new SpiScid(7, (short) 46), type);
        assertNotNull(sa);
        assertEquals(KmcDao.SA_UNKEYED, (short) sa.getSaState());
    }

    @Test
    public void testCreateSaDupeFail() throws KmcException {
        createSaDupeFail(FrameType.AOS);
        createSaDupeFail(FrameType.TM);
        createSaDupeFail(FrameType.TC);
    }

    public void createSaDupeFail(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_8, EKID_140, ECS_0_X_02, IVLEN_16,
                ST_AUTHENTICATED_ENCRYPTION, String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);

        ISecAssn sa = dao.getSa(new SpiScid(8, (short) 46), type);
        assertNotNull(sa);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_8, EKID_140, ECS_0_X_02
                , IVLEN_16, ST_AUTHENTICATED_ENCRYPTION, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

    }

    @Test
    public void testCreateSaShivf() throws KmcException {
        createSaShivf(FrameType.AOS);
        createSaShivf(FrameType.TC);
        createSaShivf(FrameType.TM);
    }

    public void createSaShivf(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, "--shivflen=20", String.format(
                TYPE_FMT, type.name()));
        assertEquals(0, exitCode);

        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertNotNull(sa);
        assertEquals(20, (short) sa.getShivfLen());
    }

    @Test
    public void testCreateSaShplf() throws KmcException {
        createSaShplf(FrameType.AOS);
        createSaShplf(FrameType.TC);
        createSaShplf(FrameType.TM);
    }

    public void createSaShplf(FrameType type) throws KmcException {

        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, "--shplflen=20", String.format(
                TYPE_FMT, type.name()));
        assertEquals(0, exitCode);

        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertNotNull(sa);
        assertEquals(20, (short) sa.getShplfLen());
    }

    @Test
    public void testCreateSaShsnf() throws KmcException {
        createSaShsnf(FrameType.AOS);
        createSaShsnf(FrameType.TC);
        createSaShsnf(FrameType.TM);
    }

    public void createSaShsnf(FrameType type) throws KmcException {

        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, "--shsnflen=20", String.format(
                TYPE_FMT, type.name()));
        assertEquals(0, exitCode);

        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertNotNull(sa);
        assertEquals(20, (short) sa.getShsnfLen());
    }

    @Test
    public void testCreateSaStmacf() throws KmcException {
        createSaStmacf(FrameType.AOS);
        createSaStmacf(FrameType.TC);
        createSaStmacf(FrameType.TM);
    }

    public void createSaStmacf(FrameType type) throws KmcException {

        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, "--stmacflen=20", String.format(
                TYPE_FMT, type.name()));
        assertEquals(0, exitCode);

        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertNotNull(sa);
        assertEquals(20, (short) sa.getStmacfLen());
    }

    @Test
    public void testCreateSaEkidFail() {
        createSaEkidFail(FrameType.TC);
        createSaEkidFail(FrameType.TM);
        createSaEkidFail(FrameType.AOS);
    }

    public void createSaEkidFail(FrameType type) {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);

        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, EKID_130,
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, ECS_0_X_01,
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, EKID_130, "--ecs=1",
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
    }

    @Test
    public void testCreateSaEkid() throws KmcException {
        createSaEkid(FrameType.TC);
        createSaEkid(FrameType.AOS);
        createSaEkid(FrameType.TM);
    }

    public void createSaEkid(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);

        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, EKID_130,
                ECS_0_X_01, "--st=1", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertNotNull(sa);
        assertEquals(6, (int) sa.getId().getSpi());
        assertEquals(46, (short) sa.getId().getScid());
        assertEquals("130", sa.getEkid());
        assertArrayEquals(new byte[]{0x01}, sa.getEcs());
        assertEquals(1, (short) sa.getEcsLen());
        assertEquals(KmcDao.SA_KEYED, (short) sa.getSaState());
    }

    @Test
    public void testCreateSaAkidFail() {
        createSaAkidFail(FrameType.TC);
        createSaAkidFail(FrameType.AOS);
        createSaAkidFail(FrameType.TM);
    }

    public void createSaAkidFail(FrameType type) {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--akid=130",
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--acs=1", String.format(
                TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--akid=130", "--acs=1",
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
    }

    @Test
    public void testCreateSaAkid() throws KmcException {
        createSaAkid(FrameType.TC);
        createSaAkid(FrameType.TM);
        createSaAkid(FrameType.AOS);
    }

    public void createSaAkid(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--akid=130", "--acs=0x01", "--st=2", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);

        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertNotNull(sa);
        assertEquals(6, (int) sa.getId().getSpi());
        assertEquals(46, (short) sa.getId().getScid());
        assertEquals("130", sa.getAkid());
        assertArrayEquals(new byte[]{0x01}, sa.getAcs());
        assertEquals(1, (short) sa.getAcsLen());
        assertEquals(KmcDao.SA_KEYED, (short) sa.getSaState());
        assertEquals(1, (short) sa.getAst());
    }

    @Test
    public void testCreateSaIv() throws KmcException {
        createSaIv(FrameType.TC);
        createSaIv(FrameType.TM);
        createSaIv(FrameType.AOS);
    }

    public void createSaIv(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--iv" +
                "=0x000000000000000000000001", IVLEN_12, ST_AUTHENTICATED_ENCRYPTION, String.format("--type" +
                "=%s", type.name()));
        assertEquals(0, exitCode);
        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertArrayEquals(new byte[]{0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x01}, sa.getIv());
        assertEquals(12, (short) sa.getIvLen());
        exitCode = cmd.execute(TFVN_EQ_0, "--scid=45", VCID_0, MAPID_0, SPI_8, IVLEN_12, "--st" +
                "=AUTHENTICATED_ENCRYPTION", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_8, EKID_140, ECS_0_X_02
                , IVLEN_16, ST_AUTHENTICATED_ENCRYPTION, String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, "--scid=47", VCID_0, MAPID_0, SPI_8, EKID_140, ECS_0_X_01
                , IVLEN_12, "--st=3", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        exitCode = cmd.execute(TFVN_EQ_0, "--scid=48", VCID_0, MAPID_0, SPI_8, EKID_140, ECS_0_X_02
                , IVLEN_16, ST_AUTHENTICATED_ENCRYPTION, String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
    }

    @Test

    public void testCreateSaIvFail() throws KmcException {
        createSaIvFail(FrameType.TC);
        createSaIvFail(FrameType.TM);
        createSaIvFail(FrameType.AOS);
    }

    public void createSaIvFail(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--iv=0x000000000000000000000001", "--ivlen=11", ST_AUTHENTICATED_ENCRYPTION, EKID_130,
                ECS_0_X_01, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--iv=0x000000000000000000000001", "--ivlen=13", ST_AUTHENTICATED_ENCRYPTION, EKID_130,
                ECS_0_X_01, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--iv=0x0000000000000000000001", IVLEN_12, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--iv=0x00000000000000000000001", IVLEN_12, ST_AUTHENTICATED_ENCRYPTION, EKID_130, ECS_0_X_01, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
        //Test if the word null in IV settings are accepted
        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--iv=null",
                IVLEN_12, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
        //Test only incorrect IV len for algo
        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, ST_AUTHENTICATED_ENCRYPTION,
                EKID_130, ECS_0_X_01, IVLEN_16, String.format(TYPE_FMT,
                type.name()));

        assertNotEquals(0, exitCode);
        List<ISecAssn> sas = dao.getSas(FrameType.TC);
        assertEquals(5, sas.size());
    }

    @Test
    public void testCreateSaIvNull() throws KmcException {
        createSaIvNull(FrameType.TC);
        createSaIvNull(FrameType.TM);
        createSaIvNull(FrameType.AOS);
    }

    public void createSaIvNull(FrameType type) throws KmcException {
        StringWriter w   = new StringWriter();
        PrintWriter  err = new PrintWriter(w);
        StringWriter o   = new StringWriter();
        PrintWriter  out = new PrintWriter(o);
        CommandLine  cmd = getCmd(new SaCreate(), true, out, err);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, IVLEN_12, "--ecs" +
                "=0x01", EKID_130, String.format(TYPE_FMT, type.name()));
        assertEquals("Incorrect error message: " + w.toString(), 0, exitCode);
        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertEquals(12, (short) sa.getIvLen());
    }

    @Test
    public void testCreateSaAbm() throws KmcException {
        createSaAbm(FrameType.TC);
        createSaAbm(FrameType.AOS);
        createSaAbm(FrameType.TM);
    }

    public void createSaAbm(FrameType type) throws KmcException {
        StringWriter w   = new StringWriter();
        PrintWriter  err = new PrintWriter(w);
        StringWriter o   = new StringWriter();
        PrintWriter  out = new PrintWriter(o);
        CommandLine  cmd = getCmd(new SaCreate(), true, out, err);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--abm" +
                "=0x1111111111111111111111111111111111111111", ABMLEN_20, "--type=" + type.name());

        assertEquals("got " + w.toString(), 0, exitCode);
        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), FrameType.TC);
        assertArrayEquals(new byte[]{0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11,
                0x11}, sa.getAbm());
        assertEquals(20, (int) sa.getAbmLen());
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(6, sas.size());
    }

    @Test
    public void testCreateSaAbmFail() throws KmcException {
        createSaAbmFail(FrameType.TC);
        createSaAbmFail(FrameType.TM);
        createSaAbmFail(FrameType.AOS);
    }

    public void createSaAbmFail(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--abm" +
                "=0x1111111111111111111111111111111111111111", "--abmlen=21", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--abm" +
                "=0x11111111111111111111111111111111111111", ABMLEN_20, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--abm" +
                "=0x111111111111111111111111111111111111111111", ABMLEN_20, String.format(TYPE_FMT,
                type.name()));
        assertNotEquals(0, exitCode);
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(5, sas.size());
    }

    @Test
    public void testCreateSaArsn() throws KmcException {
        createSaArsn(FrameType.TC);
        createSaArsn(FrameType.TM);
        createSaArsn(FrameType.AOS);
    }

    public void createSaArsn(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--arsn=0x04",
                "--arsnlen=1", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertArrayEquals(new byte[]{0x00,
                0x00,
                (byte) 0xFC,
                0x00,
                0x00,
                (byte) 0xFF,
                (byte) 0xFF,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00}, sa.getAbm());
        assertEquals(19, (int) sa.getAbmLen());
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(6, sas.size());
    }

    @Test
    public void testCreateSaArsnFail() {
        createSaArsnFail(FrameType.TC);
        createSaArsnFail(FrameType.TM);
        createSaArsnFail(FrameType.AOS);
    }

    public void createSaArsnFail(FrameType type) {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--arsn=0x04",
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
    }

    @Test
    public void testCreateSaArsnw() throws KmcException {
        createSaArsnw(FrameType.TC);
        createSaArsnw(FrameType.TM);
        createSaArsnw(FrameType.AOS);
    }

    public void createSaArsnw(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--arsnw=5",
                String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertEquals(5, (short) sa.getArsnw());
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(6, sas.size());
    }

    @Test
    public void testCreateSaEkidAkid() throws KmcException {
        createSaEkidAkid(FrameType.TC);
        createSaEkidAkid(FrameType.TM);
        createSaEkidAkid(FrameType.AOS);
    }

    public void createSaEkidAkid(FrameType type) throws KmcException {
        StringWriter w   = new StringWriter();
        PrintWriter  err = new PrintWriter(w);
        StringWriter o   = new StringWriter();
        PrintWriter  out = new PrintWriter(o);
        CommandLine  cmd = getCmd(new SaCreate(), true, out, err);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_7, EKID_130, ECS_0_X_01,
                "--akid=130", "--acs=0x01", "--st=3", String.format(TYPE_FMT, type.name()));
        assertEquals("got " + w.toString(), 0, exitCode);
        ISecAssn sa = dao.getSa(new SpiScid(7, (short) 46), type);
        assertNotNull(sa);
        assertEquals(7, (int) sa.getSpi());
        assertEquals(0, (short) sa.getTfvn());
        assertEquals(1, (short) sa.getAst());
        assertEquals(1, (short) sa.getEst());
        assertEquals(0, (byte) sa.getVcid());
        assertEquals(0, (short) sa.getMapid());
        assertArrayEquals(new byte[]{0x01}, sa.getEcs());
        assertArrayEquals(new byte[]{0x01}, sa.getAcs());
        assertEquals("130", sa.getEkid());
        assertEquals("130", sa.getAkid());
    }

    @Test
    public void testCreateSaStFail() throws KmcException {
        createSaStFail(FrameType.TC);
        createSaStFail(FrameType.TM);
        createSaStFail(FrameType.AOS);
    }

    public void createSaStFail(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--st=-1",
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_7, "--st=4", String.format(
                TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_7, EKID_130, ECS_0_X_01
                , "--st=4", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_7, EKID_130, ECS_0_X_01
                , "--st=-1", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_7, "--akid=130", "--acs=0x01"
                , "--st=4", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_7, "--akid=130", "--acs=0x01"
                , "--st=-1", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_7, "--akid=130", "--acs=0x01"
                , "--st=HI", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(5, sas.size());
    }

    @Test
    public void testCreateSaSt() throws KmcException {
        createSaSt(FrameType.TC);
        createSaSt(FrameType.TM);
        createSaSt(FrameType.AOS);
    }

    public void createSaSt(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaCreate(), true, null, null);
        int exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_EQ_6, "--st=ENCRYPTION"
                , String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        ISecAssn sa = dao.getSa(new SpiScid(6, (short) 46), type);
        assertEquals(ServiceType.ENCRYPTION, sa.getServiceType());
        assertEquals(1, (short) sa.getEst());
        assertEquals(0, (short) sa.getAst());

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_7, "--st=1",
                String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(7, (short) 46), type);
        assertEquals(ServiceType.ENCRYPTION, sa.getServiceType());
        assertEquals(1, (short) sa.getEst());
        assertEquals(0, (short) sa.getAst());

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, SPI_8, "--st=AUTHENTICATION"
                , String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(8, (short) 46), type);
        assertEquals(ServiceType.AUTHENTICATION, sa.getServiceType());
        assertEquals(0, (short) sa.getEst());
        assertEquals(1, (short) sa.getAst());

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, "--spi=9", "--st=2",
                String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(9, (short) 46), type);
        assertEquals(ServiceType.AUTHENTICATION, sa.getServiceType());
        assertEquals(0, (short) sa.getEst());
        assertEquals(1, (short) sa.getAst());

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, "--spi=10", ST_AUTHENTICATED_ENCRYPTION,
                String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(10, (short) 46), type);
        assertEquals(ServiceType.AUTHENTICATED_ENCRYPTION, sa.getServiceType());
        assertEquals(1, (short) sa.getEst());
        assertEquals(1, (short) sa.getAst());

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, "--spi=11", "--st=3",
                String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(11, (short) 46), type);
        assertEquals(ServiceType.AUTHENTICATED_ENCRYPTION, sa.getServiceType());
        assertEquals(1, (short) sa.getEst());
        assertEquals(1, (short) sa.getAst());

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, "--spi=12", "--st=PLAINTEXT",
                String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(12, (short) 46), type);
        assertEquals(ServiceType.PLAINTEXT, sa.getServiceType());
        assertEquals(0, (short) sa.getEst());
        assertEquals(0, (short) sa.getAst());

        exitCode = cmd.execute(TFVN_EQ_0, SCID_46, VCID_0, MAPID_0, "--spi=13", "--st=0",
                String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(13, (short) 46), type);
        assertEquals(ServiceType.PLAINTEXT, sa.getServiceType());
        assertEquals(0, (short) sa.getEst());
        assertEquals(0, (short) sa.getAst());
    }

}