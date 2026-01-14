package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.sadb.BaseH2Test;
import org.junit.Test;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SaDeleteTest extends BaseH2Test {

    public static final String TYPE_FMT = "--type=%s";
    public static final String SPI_1 = "--spi=1";
    public static final String SCID_46 = "--scid=46";

    @Test
    public void testDelete() throws KmcException {
        testDelete(FrameType.TC);
        testDelete(FrameType.TM);
        testDelete(FrameType.AOS);
    }

    public void testDelete(FrameType type) throws KmcException {
        CommandLine    cli = getCmd(true, null, null);
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(5, sas.size());
        cli.execute(SPI_1, SCID_46, "-y", String.format(TYPE_FMT, type.name()));
        sas = dao.getSas(type);
        assertEquals(4, sas.size());
        for (ISecAssn sa : sas) {
            assertNotEquals(1, (int) sa.getId().getSpi());
        }
    }

    @Test
    public void testDeleteConfirm() throws KmcException {
        testDeleteConfirm(FrameType.TC);
        testDeleteConfirm(FrameType.AOS);
        testDeleteConfirm(FrameType.TM);
    }

    public void testDeleteConfirm(FrameType type) throws KmcException {
        InputStream old = System.in;
        InputStream is  = new ByteArrayInputStream("y".getBytes(StandardCharsets.UTF_8));
        System.setIn(is);
        CommandLine    cli = getCmd(true, null, null);
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(5, sas.size());
        cli.execute(SPI_1, SCID_46, String.format(TYPE_FMT, type.name()));
        sas = dao.getSas(type);
        assertEquals(4, sas.size());
        for (ISecAssn sa : sas) {
            assertNotEquals(1, (int) sa.getId().getSpi());
        }
        System.setIn(old);
    }

    @Test
    public void testDeleteRefuse() throws KmcException {
        testDeleteRefuse(FrameType.TM);
        testDeleteRefuse(FrameType.AOS);
        testDeleteRefuse(FrameType.TC);
    }

    public void testDeleteRefuse(FrameType type) throws KmcException {
        InputStream old = System.in;
        InputStream is  = new ByteArrayInputStream("n".getBytes(StandardCharsets.UTF_8));
        System.setIn(is);
        CommandLine    cli = getCmd(true, null, null);
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(5, sas.size());
        cli.execute(SPI_1, SCID_46, String.format(TYPE_FMT, type.name()));
        sas = dao.getSas(type);
        assertEquals(5, sas.size());
        System.setIn(old);
    }

    @Test
    public void testDeleteMulti() throws KmcException {
        testDeleteMulti(FrameType.TC);
        testDeleteMulti(FrameType.AOS);
        testDeleteMulti(FrameType.TM);
    }

    public void testDeleteMulti(FrameType type) throws KmcException {
        CommandLine    cli = getCmd(true, null, null);
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(5, sas.size());
        cli.execute(SPI_1, "--spi=2", SCID_46, "-y", String.format(TYPE_FMT, type.name()));
        sas = dao.getSas(type);
        assertEquals(3, sas.size());
        for (ISecAssn sa : sas) {
            assertNotEquals(1, (int) sa.getId().getSpi());
            assertNotEquals(2, (int) sa.getId().getSpi());
        }
    }

    @Test
    public void testDeleteDne() {
        testDeleteDne(FrameType.TM);
        testDeleteDne(FrameType.AOS);
        testDeleteDne(FrameType.TC);
    }

    public void testDeleteDne(FrameType type) {
        CommandLine cli  = getCmd(true, null, null);
        int         exit = cli.execute("--spi=6", "--scid=55", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
    }

    @Test
    public void testDeleteFail() throws KmcException {
        testDeleteFail(FrameType.TC);
        testDeleteFail(FrameType.TM);
        testDeleteFail(FrameType.AOS);
    }

    public void testDeleteFail(FrameType type) throws KmcException {
        List<ISecAssn> sas = dao.getSas(type);
        assertEquals(5, sas.size());

        CommandLine cli  = getCmd(true, null, null);
        int         exit = cli.execute(SPI_1, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute(SCID_46, String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        sas = dao.getSas(type);
        assertEquals(5, sas.size());
    }

    private CommandLine getCmd(boolean overrideOutput, PrintWriter out, PrintWriter err) {
        SaDelete    create = new SaDelete();
        CommandLine cmd    = new CommandLine(create);
        if (overrideOutput) {
            if (out == null) {
                out = new PrintWriter(new StringWriter());
            }
            cmd.setOut(out);
            if (err == null) {
                err = new PrintWriter(new StringWriter());
            }
            cmd.setErr(err);
        }
        return cmd;
    }
}