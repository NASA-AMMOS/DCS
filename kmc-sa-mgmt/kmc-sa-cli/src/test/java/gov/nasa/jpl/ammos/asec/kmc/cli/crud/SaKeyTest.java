package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import org.junit.Test;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Tests for keying an SA
 */
public class SaKeyTest extends BaseCommandLineTest {

    public static final String SCID_46 = "--scid=46";
    public static final String AKID_130 = "--akid=130";
    public static final String EKID_140 = "--ekid=140";
    public static final String SPI_1 = "--spi=1";
    public static final String TYPE_FMT = "--type=%s";
    public static final String ACS_0_X_01 = "--acs=0x01";
    public static final String EKID_130 = "--ekid=130";
    public static final String AKID_140 = "--akid=140";
    public static final String ECS_01 = "--ecs=01";
    public static final String ACS_01 = "--acs=01";
    public static final String Y = "-y";

    @Test
    public void testRekey() throws KmcException {
        testRekey(FrameType.TC);
        testRekey(FrameType.TM);
        testRekey(FrameType.AOS);
    }

    public void testRekey(FrameType type) throws KmcException {
        CommandLine cli = getCmd(new SaKey(), true);
        int exit = cli.execute(SPI_1, SCID_46, AKID_130, ACS_0_X_01, Y, String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exit);
        ISecAssn sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals("130", sa.getAkid());
        assertEquals(1, (short) sa.getAcsLen());
        assertArrayEquals(new byte[]{0x01}, sa.getAcs());

        exit = cli.execute(SPI_1, SCID_46, EKID_140, "--ecs=02", Y, String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals("140", sa.getEkid());
        assertEquals(1, (short) sa.getEcsLen());
        assertArrayEquals(new byte[]{0x02}, sa.getEcs());

        exit = cli.execute("--spi=2", SCID_46, EKID_140, "--ecs=0002", AKID_140, "--acs" + "=0002", Y,
                String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        sa = dao.getSa(new SpiScid(2, (short) 46), type);
        assertEquals("140", sa.getEkid());
        assertEquals(1, (short) sa.getEcsLen());
        assertArrayEquals(new byte[]{0x00, 0x02}, sa.getEcs());
        assertEquals("140", sa.getAkid());
        assertEquals(1, (short) sa.getAcsLen());
        assertArrayEquals(new byte[]{0x00, 0x02}, sa.getAcs());
    }

    @Test
    public void testRekeyConfirm() throws KmcException {
        testRekeyConfirm(FrameType.TC);
        testRekeyConfirm(FrameType.AOS);
        testRekeyConfirm(FrameType.TM);
    }

    public void testRekeyConfirm(FrameType type) throws KmcException {
        InputStream old = System.in;
        InputStream is  = new ByteArrayInputStream("y".getBytes(StandardCharsets.UTF_8));
        System.setIn(is);
        CommandLine cli = getCmd(new SaKey(), true);
        int exit = cli.execute(SPI_1, SCID_46, AKID_130, ACS_0_X_01, String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exit);
        ISecAssn sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertEquals("130", sa.getAkid());
        assertEquals(1, (short) sa.getAcsLen());
        assertArrayEquals(new byte[]{0x01}, sa.getAcs());
        System.setIn(old);
    }

    @Test
    public void testRekeyReject() throws KmcException {
        testRekeyReject(FrameType.TM);
        testRekeyReject(FrameType.AOS);
        testRekeyReject(FrameType.TC);
    }

    public void testRekeyReject(FrameType type) throws KmcException {
        InputStream old = System.in;
        InputStream is  = new ByteArrayInputStream("n".getBytes(StandardCharsets.UTF_8));
        System.setIn(is);
        CommandLine cli = getCmd(new SaKey(), true);
        int exit = cli.execute(SPI_1, SCID_46, AKID_130, ACS_0_X_01, String.format(TYPE_FMT,
                type.name()));
        assertEquals(0, exit);
        ISecAssn sa = dao.getSa(new SpiScid(1, (short) 46), type);
        assertNull(sa.getAkid());
        assertEquals(0, (short) sa.getAcsLen());
        assertArrayEquals(new byte[]{0x00}, sa.getAcs());
        System.setIn(old);
    }

    @Test
    public void testRekeyFail() {
        testRekeyFail(FrameType.TC);
        testRekeyFail(FrameType.AOS);
        testRekeyFail(FrameType.TM);
    }

    public void testRekeyFail(FrameType type) {
        CommandLine cli  = getCmd(new SaKey(), true);
        int         exit = cli.execute(SPI_1, SCID_46, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, EKID_130, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, "--ecslen=1", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, ECS_01, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, EKID_130, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, ECS_01, Y, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, EKID_130, ECS_01, AKID_140, String.format(TYPE_FMT,
                type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, AKID_130, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, ACS_01, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, AKID_130, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, ACS_01, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SPI_1, SCID_46, AKID_140, ACS_01, EKID_140, String.format(TYPE_FMT,
                type.name()));
        assertNotEquals(0, exit);
    }

}