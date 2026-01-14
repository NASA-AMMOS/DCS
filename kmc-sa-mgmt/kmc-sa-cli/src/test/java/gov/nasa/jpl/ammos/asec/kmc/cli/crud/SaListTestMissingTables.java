package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import org.junit.BeforeClass;
import org.junit.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests for listing SAs
 */
public class SaListTestMissingTables extends BaseCommandLineTest {

    public static final String TYPE_FMT = "--type=%s";

    @Override
    public void beforeTest() {
        setupTc();
    }

    @Override
    public void afterTest() throws SQLException {
        truncateTc();
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dropAos();
        dropTm();
    }

    @Test
    public void listFilterSpi() {
        listFilterSpi(FrameType.TM);
        listFilterSpi(FrameType.AOS);
        listFilterSpi(FrameType.TC);
    }

    public void listFilterSpi(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute("--spi=1", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        if (type.equals(FrameType.TC)) {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","ekid","est","akid","ast","type"
                         "1","46","0","0","0","3","130","1","","1","%s"
                         """.formatted(type.name()), w.toString());
        } else {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","ekid","est","akid","ast","type"
                         """, w.toString());
        }
    }

    @Test
    public void listFilterScid() {
        listFilterScid(FrameType.TC);
        listFilterScid(FrameType.TM);
        listFilterScid(FrameType.AOS);
    }

    public void listFilterScid(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute("--scid=1", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        assertEquals("\"spi\",\"scid\",\"vcid\",\"tfvn\",\"mapid\",\"sa_state\",\"ekid\",\"est\",\"akid\",\"ast\"," +
                "\"type\"\n", w.toString());
    }

    @Test
    public void listFilterSpiScid() {
        listFilterSpiScid(FrameType.TC);
        listFilterSpiScid(FrameType.TM);
        listFilterSpiScid(FrameType.AOS);
    }

    public void listFilterSpiScid(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute("--spi=2", "--scid=46", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        if (type.equals(FrameType.TC)) {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","ekid","est","akid","ast","type"
                         "2","46","1","0","0","3","130","1","","1","%s"
                         """.formatted(type.name()), w.toString());
        } else {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","ekid","est","akid","ast","type"
                         """, w.toString());
        }
    }

    @Test
    public void testActive() throws KmcException {
        testActive(FrameType.TC);
        testActive(FrameType.TM);
        testActive(FrameType.AOS);
    }

    public void testActive(FrameType type) throws KmcException {
        StringWriter w   = new StringWriter();
        PrintWriter  out = new PrintWriter(w);
        dao.createSa(6, (byte) 0, (short) 44, (byte) 0, (byte) 0, type);
        CommandLine cli  = getCmd(new SaList(), true, out, null);
        int         exit = cli.execute("--active", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        if (type.equals(FrameType.TC)) {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","ekid","est","akid","ast","type"
                         "1","46","0","0","0","3","130","1","","1","%s"
                         "2","46","1","0","0","3","130","1","","1","%s"
                         "3","46","2","0","0","3","130","1","","1","%s"
                         "4","46","3","0","0","3","130","0","","1","%s"
                         "5","46","7","0","0","3","","0","130","1","%s"
                         """.formatted(type.name(), type.name(), type.name(), type.name(), type.name()), w.toString());
        } else {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","ekid","est","akid","ast","type"
                         """, w.toString());
        }
    }

    @Test
    public void testInactive() throws KmcException {
        testInactive(FrameType.TC);
        testInactive(FrameType.TM);
        testInactive(FrameType.AOS);
    }

    public void testInactive(FrameType type) throws KmcException {
        StringWriter w   = new StringWriter();
        PrintWriter  out = new PrintWriter(w);
        dao.createSa(6, (byte) 0, (short) 46, (byte) 0, (byte) 0, type);
        CommandLine cli  = getCmd(new SaList(), true, out, null);
        int         exit = cli.execute("--inactive", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        assertEquals("""
                     "spi","scid","vcid","tfvn","mapid","sa_state","ekid","est","akid","ast","type"
                     "6","46","0","0","0","1","","0","","0","%s"
                     """.formatted(type.name()), w.toString());
    }

    @Test
    public void listFail() {
        listFail(FrameType.TC);
        listFail(FrameType.TM);
        listFail(FrameType.AOS);
    }

    public void listFail(FrameType type) {
        CommandLine cli  = getCmd(new SaList(), true);
        int         exit = cli.execute("-e", "--mysql", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);

        exit = cli.execute("-e", "--json", String.format(TYPE_FMT, type.name()));
        assertNotEquals(0, exit);
    }

    @Test
    public void list() {
        list(FrameType.TC);
        list(FrameType.TM);
        list(FrameType.AOS);
    }

    public void list(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute(String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        if (type.equals(FrameType.TC)) {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","ekid","est","akid","ast","type"
                         "1","46","0","0","0","3","130","1","","1","%s"
                         "2","46","1","0","0","3","130","1","","1","%s"
                         "3","46","2","0","0","3","130","1","","1","%s"
                         "4","46","3","0","0","3","130","0","","1","%s"
                         "5","46","7","0","0","3","","0","130","1","%s"
                         """.formatted(type.name(), type.name(), type.name(), type.name(), type.name()), w.toString());
        } else {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","ekid","est","akid","ast","type"
                         """, w.toString());
        }
    }

    @Test
    public void listExtended() {
        listExtended(FrameType.TC);
        listExtended(FrameType.TM);
        listExtended(FrameType.AOS);
    }

    public void listExtended(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute("--extended", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        if (type == FrameType.TC) {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","st","shivf_len","shsnf_len","shplf_len","stmacf_len","ecs","ekid","iv_len","iv","acs","akid","abm_len","abm","arsn_len","arsn","arsnw","type"
                         "1","46","0","0","0","3","AUTHENTICATED_ENCRYPTION","12","0","0","16","0x01","130","0","","0x00","","19","0x00000000000000000000000000000000000000","0","0x0000000000000000000000000000000000000000","5","%s"
                         "2","46","1","0","0","3","AUTHENTICATED_ENCRYPTION","12","0","0","16","0x01","130","12","0x000000000000000000000001","0x00","","19","0x00000000000000000000000000000000000000","0","0x0000000000000000000000000000000000000000","5","%s"
                         "3","46","2","0","0","3","AUTHENTICATED_ENCRYPTION","12","0","0","16","0x01","130","12","0x000000000000000000000001","0x00","","19","0x00000000000000000000000000000000000000","0","0x0000000000000000000000000000000000000000","5","%s"
                         "4","46","3","0","0","3","AUTHENTICATION","12","0","0","16","0x01","130","12","0x000000000000000000000001","0x00","","1024","0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff","0","0x0000000000000000000000000000000000000000","5","%s"
                         "5","46","7","0","0","3","AUTHENTICATION","0","4","0","16","0x00","","0","","0x01","130","1024","0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff","4","0x00000001","5","%s"
                         """.formatted(type.name(), type.name(), type.name(), type.name(), type.name()), w.toString());
        } else {
            assertEquals("""
                         "spi","scid","vcid","tfvn","mapid","sa_state","st","shivf_len","shsnf_len","shplf_len","stmacf_len","ecs","ekid","iv_len","iv","acs","akid","abm_len","abm","arsn_len","arsn","arsnw","type"
                         """, w.toString());
        }
    }

    @Test
    public void listMysql() {
        listMysql(FrameType.TC);
        listMysql(FrameType.TM);
        listMysql(FrameType.AOS);
    }

    public void listMysql(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute("--mysql", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        if (type == FrameType.TC) {
            assertEquals("""
                         *************************** 1. row ***************************
                                spi: 1
                               ekid: 130
                               akid:\s
                           sa_state: 3
                               tfvn: 0
                               scid: 46
                               vcid: 0
                              mapid: 0
                               lpid:\s
                                 st: AUTHENTICATED_ENCRYPTION
                          shivf_len: 12
                          shsnf_len: 0
                          shplf_len: 0
                         stmacf_len: 16
                            ecs_len: 1
                                ecs: 0x01
                             iv_len: 0
                                 iv:\s
                            acs_len: 0
                                acs: 0x00
                            abm_len: 19
                                abm: 0x00000000000000000000000000000000000000
                           arsn_len: 0
                               arsn: 0x0000000000000000000000000000000000000000
                              arsnw: 5
                               type: %s -- note: field not present in DB, maps to a specific frame table
                         *************************** 2. row ***************************
                                spi: 2
                               ekid: 130
                               akid:\s
                           sa_state: 3
                               tfvn: 0
                               scid: 46
                               vcid: 1
                              mapid: 0
                               lpid:\s
                                 st: AUTHENTICATED_ENCRYPTION
                          shivf_len: 12
                          shsnf_len: 0
                          shplf_len: 0
                         stmacf_len: 16
                            ecs_len: 1
                                ecs: 0x01
                             iv_len: 12
                                 iv: 0x000000000000000000000001
                            acs_len: 0
                                acs: 0x00
                            abm_len: 19
                                abm: 0x00000000000000000000000000000000000000
                           arsn_len: 0
                               arsn: 0x0000000000000000000000000000000000000000
                              arsnw: 5
                               type: %s -- note: field not present in DB, maps to a specific frame table
                         *************************** 3. row ***************************
                                spi: 3
                               ekid: 130
                               akid:\s
                           sa_state: 3
                               tfvn: 0
                               scid: 46
                               vcid: 2
                              mapid: 0
                               lpid:\s
                                 st: AUTHENTICATED_ENCRYPTION
                          shivf_len: 12
                          shsnf_len: 0
                          shplf_len: 0
                         stmacf_len: 16
                            ecs_len: 1
                                ecs: 0x01
                             iv_len: 12
                                 iv: 0x000000000000000000000001
                            acs_len: 0
                                acs: 0x00
                            abm_len: 19
                                abm: 0x00000000000000000000000000000000000000
                           arsn_len: 0
                               arsn: 0x0000000000000000000000000000000000000000
                              arsnw: 5
                               type: %s -- note: field not present in DB, maps to a specific frame table
                         *************************** 4. row ***************************
                                spi: 4
                               ekid: 130
                               akid:\s
                           sa_state: 3
                               tfvn: 0
                               scid: 46
                               vcid: 3
                              mapid: 0
                               lpid:\s
                                 st: AUTHENTICATION
                          shivf_len: 12
                          shsnf_len: 0
                          shplf_len: 0
                         stmacf_len: 16
                            ecs_len: 1
                                ecs: 0x01
                             iv_len: 12
                                 iv: 0x000000000000000000000001
                            acs_len: 0
                                acs: 0x00
                            abm_len: 1024
                                abm: 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
                           arsn_len: 0
                               arsn: 0x0000000000000000000000000000000000000000
                              arsnw: 5
                               type: %s -- note: field not present in DB, maps to a specific frame table
                         *************************** 5. row ***************************
                                spi: 5
                               ekid:\s
                               akid: 130
                           sa_state: 3
                               tfvn: 0
                               scid: 46
                               vcid: 7
                              mapid: 0
                               lpid:\s
                                 st: AUTHENTICATION
                          shivf_len: 0
                          shsnf_len: 4
                          shplf_len: 0
                         stmacf_len: 16
                            ecs_len: 1
                                ecs: 0x00
                             iv_len: 0
                                 iv:\s
                            acs_len: 0
                                acs: 0x01
                            abm_len: 1024
                                abm: 0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
                           arsn_len: 4
                               arsn: 0x00000001
                              arsnw: 5
                               type: %s -- note: field not present in DB, maps to a specific frame table
                         """.formatted(type.name(), type.name(), type.name(), type.name(), type.name()), w.toString());
        } else {
            assertEquals("", w.toString());
        }
    }

    @Test
    public void testJson() {
        testJson(FrameType.TC);
        testJson(FrameType.TM);
        testJson(FrameType.AOS);
    }

    public void testJson(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute("--json", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        if (type == FrameType.TC) {
            assertEquals("""
                         {
                             {
                                 "spi": 1,
                                 "ekid": "130",
                                 "akid": "",
                                 "sa_state": 3,
                                 "tfvn": 0,
                                 "scid": 46,
                                 "vcid": 0,
                                 "mapid": 0,
                                 "st": "AUTHENTICATED_ENCRYPTION",
                                 "shivf_len": 12,
                                 "shsnf_len": 0,
                                 "shplf_len": 0,
                                 "stmacf_len": 16,
                                 "ecs_len": 1,
                                 "ecs": "0x01",
                                 "iv_len": 0,
                                 "iv": "",
                                 "acs_len": 0,
                                 "acs": "0x00",
                                 "abm_len": 19,
                                 "abm": "0x00000000000000000000000000000000000000",
                                 "arsn_len": 0,
                                 "arsn": "0x0000000000000000000000000000000000000000",
                                 "arsnw": 5,
                                 "type": "%s"
                             }
                             {
                                 "spi": 2,
                                 "ekid": "130",
                                 "akid": "",
                                 "sa_state": 3,
                                 "tfvn": 0,
                                 "scid": 46,
                                 "vcid": 1,
                                 "mapid": 0,
                                 "st": "AUTHENTICATED_ENCRYPTION",
                                 "shivf_len": 12,
                                 "shsnf_len": 0,
                                 "shplf_len": 0,
                                 "stmacf_len": 16,
                                 "ecs_len": 1,
                                 "ecs": "0x01",
                                 "iv_len": 12,
                                 "iv": "0x000000000000000000000001",
                                 "acs_len": 0,
                                 "acs": "0x00",
                                 "abm_len": 19,
                                 "abm": "0x00000000000000000000000000000000000000",
                                 "arsn_len": 0,
                                 "arsn": "0x0000000000000000000000000000000000000000",
                                 "arsnw": 5,
                                 "type": "%s"
                             }
                             {
                                 "spi": 3,
                                 "ekid": "130",
                                 "akid": "",
                                 "sa_state": 3,
                                 "tfvn": 0,
                                 "scid": 46,
                                 "vcid": 2,
                                 "mapid": 0,
                                 "st": "AUTHENTICATED_ENCRYPTION",
                                 "shivf_len": 12,
                                 "shsnf_len": 0,
                                 "shplf_len": 0,
                                 "stmacf_len": 16,
                                 "ecs_len": 1,
                                 "ecs": "0x01",
                                 "iv_len": 12,
                                 "iv": "0x000000000000000000000001",
                                 "acs_len": 0,
                                 "acs": "0x00",
                                 "abm_len": 19,
                                 "abm": "0x00000000000000000000000000000000000000",
                                 "arsn_len": 0,
                                 "arsn": "0x0000000000000000000000000000000000000000",
                                 "arsnw": 5,
                                 "type": "%s"
                             }
                             {
                                 "spi": 4,
                                 "ekid": "130",
                                 "akid": "",
                                 "sa_state": 3,
                                 "tfvn": 0,
                                 "scid": 46,
                                 "vcid": 3,
                                 "mapid": 0,
                                 "st": "AUTHENTICATION",
                                 "shivf_len": 12,
                                 "shsnf_len": 0,
                                 "shplf_len": 0,
                                 "stmacf_len": 16,
                                 "ecs_len": 1,
                                 "ecs": "0x01",
                                 "iv_len": 12,
                                 "iv": "0x000000000000000000000001",
                                 "acs_len": 0,
                                 "acs": "0x00",
                                 "abm_len": 1024,
                                 "abm": "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                                 "arsn_len": 0,
                                 "arsn": "0x0000000000000000000000000000000000000000",
                                 "arsnw": 5,
                                 "type": "%s"
                             }
                             {
                                 "spi": 5,
                                 "ekid": "",
                                 "akid": "130",
                                 "sa_state": 3,
                                 "tfvn": 0,
                                 "scid": 46,
                                 "vcid": 7,
                                 "mapid": 0,
                                 "st": "AUTHENTICATION",
                                 "shivf_len": 0,
                                 "shsnf_len": 4,
                                 "shplf_len": 0,
                                 "stmacf_len": 16,
                                 "ecs_len": 1,
                                 "ecs": "0x00",
                                 "iv_len": 0,
                                 "iv": "",
                                 "acs_len": 0,
                                 "acs": "0x01",
                                 "abm_len": 1024,
                                 "abm": "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                                 "arsn_len": 4,
                                 "arsn": "0x00000001",
                                 "arsnw": 5,
                                 "type": "%s"
                             }
                         }
                         """.formatted(type.name(), type.name(), type.name(), type.name(), type.name()), w.toString());
        } else {
            assertEquals("{\n}\n", w.toString());
        }
    }
}