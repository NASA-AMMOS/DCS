package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ServiceType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import org.junit.Test;
import picocli.CommandLine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for updating an SA
 */
public class SaUpdateTest extends BaseCommandLineTest {

    public static final String BULK_SA_FILE   = "kmc-all-SAs.csv";
    public static final String BULK_SA_FILE_2 = "kmc-all-SAs-type.csv";
    public static final String H2_SA_UPDATES  = "test-sas-h2.csv";
    public static final String TYPE_FMT = "--type=%s";
    public static final String FILE_FMT = "--file=%s";
    public static final String SPI_5 = "--spi=5";
    public static final String SCID_46 = "--scid=46";
    public static final String SPI_1 = "--spi=1";
    public static final String ECS_0_X_01 = "--ecs=0x01";

    @Test
    public void testUpdateBulkNotExist() throws KmcException {
        testUpdateBulkNotExist(FrameType.TC);
        testUpdateBulkNotExist(FrameType.TM);
        testUpdateBulkNotExist(FrameType.AOS);
    }

    public void testUpdateBulkNotExist(FrameType type) throws KmcException {
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(5, sas.size());
        CommandLine cli = getCmd(new SaUpdate(), true);
        int exit = cli.execute(String.format(FILE_FMT, getClass().getClassLoader().getResource(
                BULK_SA_FILE).getFile()), String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sas = dao.getSas(type);
        assertEquals(5, sas.size());
    }

    @Test
    public void testUpdateBulkNotExistType() throws KmcException, SQLException {
        testUpdateBulkNotExistType(FrameType.TC, 5);
        afterTest();
        beforeTest();
        testUpdateBulkNotExistType(FrameType.TM, 5);
        afterTest();
        beforeTest();
        testUpdateBulkNotExistType(FrameType.AOS, 5);
        afterTest();
        beforeTest();
        testUpdateBulkNotExistType(FrameType.ALL, 15);
        afterTest();
        beforeTest();
    }

    public void testUpdateBulkNotExistType(FrameType type, int expect) throws KmcException {
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(type.name(), expect, sas.size());
        CommandLine cli = getCmd(new SaUpdate(), true);
        int exit = cli.execute(String.format(FILE_FMT, getClass().getClassLoader().getResource(
                BULK_SA_FILE_2).getFile()), String.format(TYPE_FMT, type.name()));
        assertEquals(type.name(), 0, exit);
        sas = dao.getSas(type);
        assertEquals(type.name(), expect, sas.size());
    }

    @Test
    public void testUpdateBulk() throws KmcException {
        testUpdateBulk(FrameType.TC);
        testUpdateBulk(FrameType.TM);
        testUpdateBulk(FrameType.AOS);
    }

    public void testUpdateBulk(FrameType type) throws KmcException {
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(5, sas.size());
        assertEquals(1, (int) sas.get(0).getSpi());
        CommandLine cli = getCmd(new SaUpdate(), true);
        int exit = cli.execute(String.format(FILE_FMT, getClass().getClassLoader().getResource(
                H2_SA_UPDATES).getFile()), String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sas = new ArrayList<>(dao.getSas(type));
        assertEquals(5, sas.size());
        sas.sort(Comparator.comparing(ISecAssn::getSpi));
        assertEquals(ServiceType.ENCRYPTION, sas.get(0).getServiceType());
        assertEquals("140", sas.get(0).getEkid());
        assertEquals(ServiceType.AUTHENTICATION, sas.get(1).getServiceType());
        assertEquals("140", sas.get(1).getEkid());
        assertEquals(ServiceType.ENCRYPTION, sas.get(2).getServiceType());
        assertEquals("140", sas.get(2).getEkid());
        assertEquals(ServiceType.AUTHENTICATED_ENCRYPTION, sas.get(3).getServiceType());
        assertEquals("140", sas.get(3).getEkid());
        assertEquals(ServiceType.AUTHENTICATED_ENCRYPTION, sas.get(4).getServiceType());
        assertEquals("140", sas.get(4).getEkid());
    }

    @Test
    public void testNoArgsFail() {
        CommandLine cli  = getCmd(new SaUpdate(), true);
        int         exit = cli.execute();
        assertNotEquals(0, exit);
    }

    @Test
    public void testResetArsnFail() throws KmcException {
        testResetArsnFail(FrameType.TC);
        testResetArsnFail(FrameType.TM);
        testResetArsnFail(FrameType.AOS);
    }

    public void testResetArsnFail(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(5, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(4, (short) sa.getArsnLen());
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x01}, sa.getArsn());

        int exit = cli.execute(SPI_5, SCID_46, "--arsn=0x01", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_5, SCID_46, "--arsnlen=6", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
    }

    @Test
    public void testResetArsn() throws KmcException {
        testResetArsn(FrameType.TC);
        testResetArsn(FrameType.TM);
        testResetArsn(FrameType.AOS);
    }

    public void testResetArsn(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(5, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(4, (short) sa.getArsnLen());
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x01}, sa.getArsn());
        int exit = cli.execute(SPI_5, SCID_46, "--arsn=0x0000000002", "--arsnlen=5", String.format(TYPE_FMT
                , type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals(5, (short) sa.getArsnLen());
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00, 0x02}, sa.getArsn());
    }

    @Test
    public void testResetIv() throws KmcException {
        testResetIv(FrameType.TC);
        testResetIv(FrameType.TM);
        testResetIv(FrameType.AOS);
    }

    public void testResetIv(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(0, (short) sa.getIvLen());
        assertNull(sa.getIv());
        int exit = cli.execute(SPI_1, SCID_46, "--iv=0x00112233465566778899aabbccddeeff", "--ivlen=16",
                String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals(16, (short) sa.getIvLen());
        assertArrayEquals(new byte[]{0x00,
                0x11,
                0x22,
                0x33,
                0x46,
                0x55,
                0x66,
                0x77,
                (byte) 0x88,
                (byte) 0x99,
                (byte) 0xaa,
                (byte) 0xbb,
                (byte) 0xcc,
                (byte) 0xdd,
                (byte) 0xee,
                (byte) 0xff}, sa.getIv());
    }

    @Test
    public void testResetIvFail() throws KmcException {
        testResetIvFail(FrameType.TC);
        testResetIvFail(FrameType.TM);
        testResetIvFail(FrameType.AOS);
    }

    public void testResetIvFail(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(0, (short) sa.getIvLen());
        assertNull(sa.getIv());

        int exit = cli.execute(SPI_1, SCID_46, ECS_0_X_01, "--iv=0x00112233465566778899aabb", "--ivlen=12",
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, ECS_0_X_01, "--iv=0x01", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, "--ivlen=8", ECS_0_X_01, "--ekid=130", String.format(TYPE_FMT
                , type.name()));
        assertNotEquals(0, exit);
    }

    @Test
    public void testResetShivf() throws KmcException {
        testResetShivf(FrameType.TC);
        testResetShivf(FrameType.TM);
        testResetShivf(FrameType.AOS);
    }

    public void testResetShivf(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(12, (short) sa.getShivfLen());
        int exit = cli.execute(SPI_1, SCID_46, "--shivflen=20", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals(20, (short) sa.getShivfLen());
    }

    @Test
    public void testResetShplf() throws KmcException {
        testResetShplf(FrameType.TC);
        testResetShplf(FrameType.TM);
        testResetShplf(FrameType.AOS);
    }

    public void testResetShplf(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(0, (short) sa.getShplfLen());
        int exit = cli.execute(SPI_1, SCID_46, "--shplflen=20", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals(20, (short) sa.getShplfLen());
    }

    @Test
    public void testResetShsnf() throws KmcException {
        testResetShsnf(FrameType.TC);
        testResetShsnf(FrameType.TM);
        testResetShsnf(FrameType.AOS);
    }

    public void testResetShsnf(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(0, (short) sa.getShsnfLen());
        int exit = cli.execute(SPI_1, SCID_46, "--shsnflen=20", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals(20, (short) sa.getShsnfLen());
    }

    @Test
    public void testResetStmacf() throws KmcException {
        testResetStmacf(FrameType.TC);
        testResetStmacf(FrameType.TM);
        testResetStmacf(FrameType.AOS);
    }

    public void testResetStmacf(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(16, (short) sa.getStmacfLen());
        int exit = cli.execute(SPI_1, SCID_46, "--stmacflen=20", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals(20, (short) sa.getStmacfLen());
    }

    @Test
    public void testResetEkidFail() throws KmcException {
        testResetEkidFail(FrameType.TC);
        testResetEkidFail(FrameType.TM);
        testResetEkidFail(FrameType.AOS);
    }

    public void testResetEkidFail(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals("130", sa.getEkid());
        int exitCode = cmd.execute(SPI_1, SCID_46, "--ekid=140", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(SCID_46, SPI_1, ECS_0_X_01, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(SCID_46, SPI_1, "--ekid=130", "--ecs=1", String.format(TYPE_FMT,
                type.name()));
        assertNotEquals(0, exitCode);
    }

    @Test
    public void testResetEkid() throws KmcException {
        testResetEkid(FrameType.TC);
        testResetEkid(FrameType.TM);
        testResetEkid(FrameType.AOS);
    }

    public void testResetEkid(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals("130", sa.getEkid());
        assertArrayEquals(new byte[]{0x01}, sa.getEcs());
        int exit = cli.execute(SPI_1, SCID_46, "--ekid=140", "--ecs=0x02", "--ivlen=16", String.format(
                TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals("140", sa.getEkid());
        assertArrayEquals(new byte[]{0x02}, sa.getEcs());
    }

    @Test
    public void testResetAkidFail() throws KmcException {
        testResetAkidFail(FrameType.TC);
        testResetAkidFail(FrameType.TM);
        testResetAkidFail(FrameType.AOS);
    }

    public void testResetAkidFail(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertNull(sa.getAkid());
        assertArrayEquals(new byte[]{0x00}, sa.getAcs());
        int exitCode = cmd.execute(SPI_1, SCID_46, "--akid=140", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(SCID_46, SPI_1, "--acs=0x01", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);

        exitCode = cmd.execute(SCID_46, SPI_1, "--akid=130", "--acs=1", String.format(TYPE_FMT,
                type.name()));
        assertNotEquals(0, exitCode);
    }

    @Test
    public void testResetAkid() throws KmcException {
        testResetAkid(FrameType.TC);
        testResetAkid(FrameType.TM);
        testResetAkid(FrameType.AOS);
    }

    public void testResetAkid(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertNull(sa.getAkid());
        assertArrayEquals(new byte[]{0x00}, sa.getAcs());
        int exit = cli.execute(SPI_1, SCID_46, "--akid=140", "--acs=0x02", String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals("140", sa.getAkid());
        assertArrayEquals(new byte[]{0x02}, sa.getAcs());
    }

    @Test
    public void testResetAbm() throws KmcException {
        testResetAbm(FrameType.TC);
        testResetAbm(FrameType.TM);
        testResetAbm(FrameType.AOS);
    }

    public void testResetAbm(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(19, (int) sa.getAbmLen());
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
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00}, sa.getAbm());
        int exit = cli.execute(SPI_1, SCID_46, "--abm=0x1111111111111111111111111111111111111111",
                "--abmlen" + "=20", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(id, type);
        assertEquals(20, (int) sa.getAbmLen());
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
    }

    @Test
    public void testResetAbmFail() throws KmcException {
        testResetAbmFail(FrameType.TC);
        testResetAbmFail(FrameType.TM);
        testResetAbmFail(FrameType.AOS);
    }

    public void testResetAbmFail(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaUpdate(), true);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertNull(sa.getAkid());
        assertArrayEquals(new byte[]{0x00}, sa.getAcs());
        int exit = cli.execute(SPI_1, SCID_46, "--abm=0x1111111111111111111111111111111111111111",
                "--abmlen" + "=21", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, "--abm=0x11111111111111111111111111111111111111", "--abmlen=20",
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, "--abm=111111111111111111111111111111111111111111", "--abmlen=20",
                String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
    }

    @Test
    public void testResetArsnw() throws KmcException {
        testResetArsnw(FrameType.TC);
        testResetArsnw(FrameType.TM);
        testResetArsnw(FrameType.AOS);
    }

    public void testResetArsnw(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaUpdate(), true, null, null);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(5, (short) sa.getArsnw());
        int exitCode = cmd.execute(SCID_46, SPI_1, "--arsnw=10", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(10, (short) sa.getArsnw());
    }

    @Test
    public void testResetSt() throws KmcException {
        testResetSt(FrameType.TC);
        testResetSt(FrameType.TM);
        testResetSt(FrameType.AOS);
    }

    public void testResetSt(FrameType type) throws KmcException {
        CommandLine cmd = getCmd(new SaUpdate(), true, null, null);
        SpiScid     id  = new SpiScid(1, (short) 46);
        ISecAssn    sa  = dao.getSa(id, type);
        assertEquals(ServiceType.AUTHENTICATED_ENCRYPTION, sa.getServiceType());
        int exitCode = cmd.execute(SCID_46, SPI_1, "--st=1", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(ServiceType.ENCRYPTION, sa.getServiceType());

        exitCode = cmd.execute(SCID_46, SPI_1, "--st=2", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(ServiceType.AUTHENTICATION, sa.getServiceType());

        exitCode = cmd.execute(SCID_46, SPI_1, "--st=0", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(ServiceType.PLAINTEXT, sa.getServiceType());

        exitCode = cmd.execute(SCID_46, SPI_1, "--st=3", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(ServiceType.AUTHENTICATED_ENCRYPTION, sa.getServiceType());

        exitCode = cmd.execute(SCID_46, SPI_1, "--st=ENCRYPTION", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(ServiceType.ENCRYPTION, sa.getServiceType());

        exitCode = cmd.execute(SCID_46, SPI_1, "--st=PLAINTEXT", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(ServiceType.PLAINTEXT, sa.getServiceType());

        exitCode = cmd.execute(SCID_46, SPI_1, "--st=AUTHENTICATION", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(ServiceType.AUTHENTICATION, sa.getServiceType());

        exitCode = cmd.execute(SCID_46, SPI_1, "--st=AUTHENTICATED_ENCRYPTION", String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exitCode);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals(ServiceType.AUTHENTICATED_ENCRYPTION, sa.getServiceType());
    }

    @Test
    public void testResetStFail() {
        testResetStFail(FrameType.TC);
        testResetStFail(FrameType.TM);
        testResetStFail(FrameType.AOS);
    }

    public void testResetStFail(FrameType type) {
        CommandLine cmd      = getCmd(new SaUpdate(), true, null, null);
        int         exitCode = cmd.execute(SCID_46, SPI_1, "--st=4", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
        exitCode = cmd.execute(SCID_46, SPI_1, "--st=-1", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
        exitCode = cmd.execute(SCID_46, SPI_1, "--st=unknown", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exitCode);
    }

}