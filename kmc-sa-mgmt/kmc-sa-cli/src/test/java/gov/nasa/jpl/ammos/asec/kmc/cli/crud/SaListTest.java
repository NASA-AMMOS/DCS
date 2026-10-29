package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import org.junit.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for listing SAs
 */
public class SaListTest extends BaseCommandLineTest {

    public static final String TYPE_FMT = "--type=%s";

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
        assertEquals("""
                     "spi","scid","vcid","tfvn","mapid","sa_state","st","ekid","akid","type"
                     "1","46","0","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     """.formatted(type.name()), w.toString());
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
        assertEquals("\"spi\",\"scid\",\"vcid\",\"tfvn\",\"mapid\",\"sa_state\",\"st\",\"ekid\",\"akid\"," +
                "\"type\"\n", w.toString());
    }

    @Test
    public void listFilterVcid() {
        listFilterVcid(FrameType.TC);
        listFilterVcid(FrameType.TM);
        listFilterVcid(FrameType.AOS);
    }

    public void listFilterVcid(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute("--vcid=2", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        assertEquals("""
                     "spi","scid","vcid","tfvn","mapid","sa_state","st","ekid","akid","type"
                     "3","46","2","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     """.formatted(type.name()), w.toString());
    }

    @Test
    public void listFilterScidVcid() {
        listFilterScidVcid(FrameType.TC);
        listFilterScidVcid(FrameType.TM);
        listFilterScidVcid(FrameType.AOS);
    }

    public void listFilterScidVcid(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute("--scid=46", "--vcid=2", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        assertEquals("""
                     "spi","scid","vcid","tfvn","mapid","sa_state","st","ekid","akid","type"
                     "3","46","2","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     """.formatted(type.name()), w.toString());
    }

    @Test
    public void listFilterVcidNoMatch() {
        listFilterVcidNoMatch(FrameType.TC);
        listFilterVcidNoMatch(FrameType.TM);
        listFilterVcidNoMatch(FrameType.AOS);
    }

    public void listFilterVcidNoMatch(FrameType type) {
        StringWriter w    = new StringWriter();
        PrintWriter  out  = new PrintWriter(w);
        CommandLine  cli  = getCmd(new SaList(), true, out, null);
        int          exit = cli.execute("--vcid=99", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        assertEquals("\"spi\",\"scid\",\"vcid\",\"tfvn\",\"mapid\",\"sa_state\",\"st\",\"ekid\",\"akid\"," +
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
        assertEquals("""
                     "spi","scid","vcid","tfvn","mapid","sa_state","st","ekid","akid","type"
                     "2","46","1","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     """.formatted(type.name()), w.toString());
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
        assertEquals("""
                     "spi","scid","vcid","tfvn","mapid","sa_state","st","ekid","akid","type"
                     "1","46","0","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     "2","46","1","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     "3","46","2","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     "4","46","3","0","0","3","AUTHENTICATION","130","","%s"
                     "5","46","7","0","0","3","AUTHENTICATION","","130","%s"
                     """.formatted(type.name(), type.name(), type.name(), type.name(), type.name()), w.toString());
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
                     "spi","scid","vcid","tfvn","mapid","sa_state","st","ekid","akid","type"
                     "6","46","0","0","0","1","PLAINTEXT","","","%s"
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
        assertEquals("""
                     "spi","scid","vcid","tfvn","mapid","sa_state","st","ekid","akid","type"
                     "1","46","0","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     "2","46","1","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     "3","46","2","0","0","3","AUTHENTICATED_ENCRYPTION","130","","%s"
                     "4","46","3","0","0","3","AUTHENTICATION","130","","%s"
                     "5","46","7","0","0","3","AUTHENTICATION","","130","%s"
                     """.formatted(type.name(), type.name(), type.name(), type.name(), type.name()), w.toString());
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
        assertEquals("""
                     "spi","scid","vcid","tfvn","mapid","sa_state","st","shivf_len","shsnf_len","shplf_len","stmacf_len","ecs","ekid","iv_len","iv","acs","akid","abm_len","abm","arsn_len","arsn","arsnw","type"
                     "1","46","0","0","0","3","AUTHENTICATED_ENCRYPTION","12","0","0","16","0x01","130","0","","0x00","","19","0x00000000000000000000000000000000000000","0","0x0000000000000000000000000000000000000000","5","%s"
                     "2","46","1","0","0","3","AUTHENTICATED_ENCRYPTION","12","0","0","16","0x01","130","12","0x000000000000000000000001","0x00","","19","0x00000000000000000000000000000000000000","0","0x0000000000000000000000000000000000000000","5","%s"
                     "3","46","2","0","0","3","AUTHENTICATED_ENCRYPTION","12","0","0","16","0x01","130","12","0x000000000000000000000001","0x00","","19","0x00000000000000000000000000000000000000","0","0x0000000000000000000000000000000000000000","5","%s"
                     "4","46","3","0","0","3","AUTHENTICATION","12","0","0","16","0x01","130","12","0x000000000000000000000001","0x00","","1024","0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff","0","0x0000000000000000000000000000000000000000","5","%s"
                     "5","46","7","0","0","3","AUTHENTICATION","0","4","0","16","0x00","","0","","0x01","130","1024","0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff","4","0x00000001","5","%s"
                     """.formatted(type.name(), type.name(), type.name(), type.name(), type.name()), w.toString());
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
    }

    @Test
    public void testCsvRoundTrip() throws Exception {
        testCsvRoundTrip(FrameType.TC);
        testCsvRoundTrip(FrameType.TM);
        testCsvRoundTrip(FrameType.AOS);
    }

    public void testCsvRoundTrip(FrameType type) throws Exception {
        StringWriter listOut = new StringWriter();
        PrintWriter  listPw  = new PrintWriter(listOut);
        CommandLine  listCmd = getCmd(new SaList(), true, listPw, null);
        int          exit    = listCmd.execute("--extended", String.format(TYPE_FMT, type.name()));
        assertEquals(0, exit);
        String csv = listOut.toString();

        java.io.File tmpFile = java.io.File.createTempFile("sa-roundtrip-", ".csv");
        try {
            java.nio.file.Files.writeString(tmpFile.toPath(), csv);

            truncateTable(type);
            assertEquals(0, dao.getSas(type).size());

            StringWriter createOut = new StringWriter();
            PrintWriter  createPw  = new PrintWriter(createOut);
            StringWriter createErr = new StringWriter();
            PrintWriter  createErrPw = new PrintWriter(createErr);
            CommandLine  createCmd = getCmd(new SaCreate(), true, createPw, createErrPw);
            int createExit = createCmd.execute("--file=" + tmpFile.getAbsolutePath(), String.format(TYPE_FMT, type.name()));
            String output = createOut.toString() + createErr.toString();
            if (createExit != 0) {
                fail("Create failed with exit " + createExit + ":\n" + output);
            }

            int actualCount = dao.getSas(type).size();
            if (actualCount != 5) {
                fail("Expected 5 SAs, got " + actualCount + ". Create output:\n" + output + "\nCSV:\n" + csv);
            }
            gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn sa1 = dao.getSa(new gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid(1, (short) 46), type);
            assertNotNull(sa1);
            assertEquals(gov.nasa.jpl.ammos.asec.kmc.api.sa.ServiceType.AUTHENTICATED_ENCRYPTION, sa1.getServiceType());
            assertEquals(1, (short) sa1.getEst());
            assertEquals(1, (short) sa1.getAst());
        } finally {
            tmpFile.delete();
        }
    }

    private void truncateTable(FrameType type) throws Exception {
        switch (type) {
            case TC -> truncateTc();
            case TM -> truncateTm();
            case AOS -> truncateAos();
        }
    }
}