package gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ServiceType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.sadb.BaseH2Test;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SaControllerTest extends BaseH2Test {

    public static final String ARSN_LEN = "arsnLen";
    public static final String ARSNW = "arsnw";
    public static final String ERROR = "error";
    public static final String PLAINTEXT = "PLAINTEXT";
    public static final String SERVICE_TYPE = "serviceType";
    public static final String TFVN = "tfvn";
    public static final String EST = "est";
    public static final String AST = "ast";
    public static final String TYPE = "type";
    public static final String SA_STATE = "saState";
    public static final String START = "/start";
    public static final String STOP = "/stop";
    public static final String EXPIRE = "/expire";
    public static final String ENCRYPTION = "ENCRYPTION";
    public static final String EKID = "ekid";
    public static final String IV_LEN = "ivLen";
    public static final String IV = "iv";
    public static final String AKID = "akid";
    public static final String AUTHENTICATION = "AUTHENTICATION";
    public static final String ACS = "acs";
    public static final String SUCCESS = "success";
    public static final String ARSN = "arsn";
    public static final String SCID = "scid";
    public static final String SPI = "spi";
    public static final String STATUS = "status";
    public static final String BOGUS_EKID = "bogus/ekid";
    public static final String BOGUS_AKID = "bogus/akid";
    public static final String FORCE = "force";
    public static final String FILE = "file";
    public static final String BOGUS_AKID_2 = "bogus/akid/2";
    public static final String BOGUS_EKID_2 = "bogus/ekid/2";
    public static final String KEY_PATH = "/key";
    public static final String CSV_PATH = "/csv";
    public static final String CREATE_PATH = "/create";
    public static final String ID_PATH = "/id";
    public static final String IV_PATH = "/iv";
    public static final String ARSN_PATH = "/arsn";
    public static final String IV_1 = "00000000000000000000000000000001";
    @Autowired
    private SaController sa;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void contextLoads() {
        assertNotNull(sa);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testGetSas() {
        ArrayNode resp = restTemplate.getForObject(getUrl(), ArrayNode.class);
        assertNotNull(resp);
        assertEquals(15, resp.size());
    }

    @Test
    public void testGetSasByType() {
        testGetSasByType("tc");
        testGetSasByType("tm");
        testGetSasByType("aos");

    }

    private void testGetSasByType(String type) {
        ArrayNode resp = restTemplate.getForObject(getUrl() + "/" + type, ArrayNode.class);
        assertNotNull(resp);
        assertEquals(5, resp.size());
    }

    private String getUrl() {
        return String.format("http://localhost:%d/api/sa", port);
    }


    private ObjectNode createSaJson() {
        ObjectNode node = mapper.createObjectNode();
        node.withObject(ID_PATH).put(SPI, 100).put(SCID, 46);
        node.put(TFVN, 0).put("vcid", 20).put("mapid", 0).put(EKID, "kmc/test/key128").putNull(
                AKID).put(SA_STATE, 3).putNull("lpid").put(EST, 1).put(AST, 1).put("shivfLen", 12).put(
                "shsnfLen", 0).put("shplfLen", 0).put("stmacfLen", 16).put("ecsLen", 1).put("ecs", "01").put(
                IV_LEN, 12).put(IV, "000000000000000000000001").put("acsLen", 1).put(ACS, "00").put("abmLen",
                19).put("abm", "ffffffffffffff000000000000000000000000").put(ARSN_LEN, 0).put(ARSN, "").put(ARSNW
                , 5).put(SPI, 100).put(SCID, 46).put(SERVICE_TYPE, "AUTHENTICATED_ENCRYPTION");
        return node;
    }

    @Test
    public void testCreateSaByType() throws KmcException {
        createSaByType(FrameType.TC);
        createSaByType(FrameType.TM);
        createSaByType(FrameType.AOS);
    }

    public void createSaByType(FrameType type) throws KmcException {
        ObjectNode node = createSaJson();
        node.put(TYPE, type.name());
        HttpEntity<JsonNode> req = new HttpEntity<>(node);

        ResponseEntity<ObjectNode> resp = restTemplate.exchange(getUrl(),
                HttpMethod.PUT, req, ObjectNode.class, new Object[0]);
        assertNotNull(resp);
        ObjectNode body = resp.getBody();
        JsonNode statusNode = body.get(STATUS);
        if (statusNode != null) {
            String status = statusNode.asText();
            assertNotEquals(ERROR, status);
        }
        assertEquals(100, body.get(SPI).asInt());
        assertEquals(46, body.get(SCID).asInt());

        ISecAssn created = dao.getSa(new SpiScid(100, (short) 46), type);
        assertNotNull(created);
        assertArrayEquals(new byte[]{0x00}, created.getAcs());
        assertArrayEquals(new byte[]{(byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
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
                0x00}, created.getAbm());
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
                0x01}, created.getIv());
        assertEquals(type, created.getType());
    }

    @Test
    public void testCreateSa() throws KmcException {
        ObjectNode node = createSaJson();
        HttpEntity<JsonNode> req = new HttpEntity<>(node);

        ResponseEntity<ObjectNode> resp = restTemplate.exchange(getUrl(),
                HttpMethod.PUT, req, ObjectNode.class, new Object[0]);
        assertNotNull(resp);
        ObjectNode body = resp.getBody();
        JsonNode statusNode = body.get(STATUS);
        if (statusNode != null) {
            String status = statusNode.asText();
            assertNotEquals(ERROR, status);
        }
        assertEquals(100, body.get(SPI).asInt());
        assertEquals(46, body.get(SCID).asInt());

        ISecAssn created = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertNotNull(created);
        assertArrayEquals(new byte[]{0x00}, created.getAcs());
        assertArrayEquals(new byte[]{(byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
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
                0x00}, created.getAbm());
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
                0x01}, created.getIv());
    }

    @Test
    public void testUpdateSaByType() throws KmcException {
        updateSaByType(FrameType.TC);
        updateSaByType(FrameType.TM);
        updateSaByType(FrameType.AOS);
    }

    public void updateSaByType(FrameType type) throws KmcException {
        createSaByType(type);
        ObjectNode node = createSaJson();
        node.put(TYPE, type.name());
        node.put(TFVN, 1).put(EST, 0).put(AST, 0).put(SERVICE_TYPE, PLAINTEXT);

        ObjectNode body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);

        assertEquals(1, body.get(TFVN).asInt());
        assertEquals(PLAINTEXT, body.get(SERVICE_TYPE).asText());
        assertEquals(0, body.get(EST).asInt());
        assertEquals(0, body.get(AST).asInt());
        assertEquals(type.name(), body.get(TYPE).asText());

        ISecAssn updated = dao.getSa(new SpiScid(100, (short) 46), type);
        assertEquals(1, updated.getTfvn().intValue());
        assertEquals(ServiceType.PLAINTEXT, updated.getServiceType());
        assertEquals(0, (short) updated.getEst());
        assertEquals(0, (short) updated.getAst());
        assertEquals(type, updated.getType());

        node.put(SA_STATE, 2);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 3);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 1);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + START, node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + STOP, node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + EXPIRE, node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());
    }

    @Test
    public void testUpdateSa() throws KmcException {
        testCreateSa();
        ObjectNode node = createSaJson();
        node.put(TFVN, 1).put(EST, 0).put(AST, 0).put(SERVICE_TYPE, PLAINTEXT);

        ObjectNode body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);

        assertEquals(1, body.get(TFVN).asInt());
        assertEquals(PLAINTEXT, body.get(SERVICE_TYPE).asText());
        assertEquals(0, body.get(EST).asInt());
        assertEquals(0, body.get(AST).asInt());

        ISecAssn updated = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertEquals(1, updated.getTfvn().intValue());
        assertEquals(ServiceType.PLAINTEXT, updated.getServiceType());
        assertEquals(0, (short) updated.getEst());
        assertEquals(0, (short) updated.getAst());

        node.put(SA_STATE, 2);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 3);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 1);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + START, node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + STOP, node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + EXPIRE, node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());
    }

    @Test
    public void testUpdateSaUnkeyedEncryptedByType() throws KmcException {
        updateSaUnkeyedEncryptedByType(FrameType.TC);
        updateSaUnkeyedEncryptedByType(FrameType.TM);
        updateSaUnkeyedEncryptedByType(FrameType.AOS);
    }

    public void updateSaUnkeyedEncryptedByType(FrameType type) throws KmcException {
        createSaByType(type);
        ObjectNode node = createSaJson();
        node.put(TYPE, type.name());
        node.put(TFVN, 1).put(EST, 0).put(AST, 0).put(SERVICE_TYPE, ENCRYPTION);

        ObjectNode body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);

        assertEquals(1, body.get(TFVN).asInt());
        assertEquals(ENCRYPTION, body.get(SERVICE_TYPE).asText());
        assertEquals(1, body.get(EST).asInt());
        assertEquals(0, body.get(AST).asInt());
        assertEquals(type.name(), body.get(TYPE).asText());

        ISecAssn updated = dao.getSa(new SpiScid(100, (short) 46), type);
        assertEquals(type, updated.getType());
        assertEquals(1, updated.getTfvn().intValue());
        assertEquals(ServiceType.ENCRYPTION, updated.getServiceType());
        assertEquals(1, (short) updated.getEst());
        assertEquals(0, (short) updated.getAst());

        node.put(SA_STATE, 2);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 3);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 1);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + START, node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + STOP, node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + EXPIRE, node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        node.put(IV, IV_1);
        node.put(IV_LEN, "16");
        node.put(EKID, "null");
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertNotNull(body);
    }

    @Test
    public void testUpdateSaUnkeyedEncrypted() throws KmcException {
        testCreateSa();
        ObjectNode node = createSaJson();
        node.put(TFVN, 1).put(EST, 0).put(AST, 0).put(SERVICE_TYPE, ENCRYPTION);

        ObjectNode body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);

        assertEquals(1, body.get(TFVN).asInt());
        assertEquals(ENCRYPTION, body.get(SERVICE_TYPE).asText());
        assertEquals(1, body.get(EST).asInt());
        assertEquals(0, body.get(AST).asInt());

        ISecAssn updated = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertEquals(1, updated.getTfvn().intValue());
        assertEquals(ServiceType.ENCRYPTION, updated.getServiceType());
        assertEquals(1, (short) updated.getEst());
        assertEquals(0, (short) updated.getAst());

        node.put(SA_STATE, 2);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 3);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 1);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + START, node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + STOP, node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + EXPIRE, node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        node.put(IV, IV_1);
        node.put(IV_LEN, "16");
        node.put(EKID, "null");
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertNotNull(body);
    }

    @Test
    public void testUpdateSaUnkeyedAuthByType() throws KmcException {
        updateSaUnkeyedAuthByType(FrameType.TC);
        updateSaUnkeyedAuthByType(FrameType.TM);
        updateSaUnkeyedAuthByType(FrameType.AOS);
    }

    public void updateSaUnkeyedAuthByType(FrameType type) throws KmcException {
        createSaByType(type);
        ObjectNode node = createSaJson();
        node.put(TFVN, 1).put(EST, 0).put(AST, 0).put(SERVICE_TYPE, AUTHENTICATION).put(ACS, "01");
        node.putNull(EKID);
        node.put(AKID, "kmc/test/key129");
        node.put(TYPE, type.name());

        ObjectNode body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);

        assertEquals(1, body.get(TFVN).asInt());
        assertEquals(AUTHENTICATION, body.get(SERVICE_TYPE).asText());
        assertEquals(0, body.get(EST).asInt());
        assertEquals(1, body.get(AST).asInt());
        assertEquals(type.name(), body.get(TYPE).asText());

        ISecAssn updated = dao.getSa(new SpiScid(100, (short) 46), type);
        assertEquals(1, updated.getTfvn().intValue());
        assertEquals(ServiceType.AUTHENTICATION, updated.getServiceType());
        assertEquals(0, (short) updated.getEst());
        assertEquals(1, (short) updated.getAst());
        assertEquals(type, updated.getType());

        node.put(SA_STATE, 2);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 3);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 1);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + START, node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + STOP, node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + EXPIRE, node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        node.put(IV, IV_1);
        node.put(IV_LEN, "16");
        node.put(EKID, "null");
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertNotNull(body);
    }

    @Test
    public void testUpdateSaUnkeyedAuth() throws KmcException {
        testCreateSa();
        ObjectNode node = createSaJson();
        node.put(TFVN, 1).put(EST, 0).put(AST, 0).put(SERVICE_TYPE, AUTHENTICATION).put(ACS, "01");
        node.putNull(EKID);
        node.put(AKID, "kmc/test/key129");

        ObjectNode body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);

        assertEquals(1, body.get(TFVN).asInt());
        assertEquals(AUTHENTICATION, body.get(SERVICE_TYPE).asText());
        assertEquals(0, body.get(EST).asInt());
        assertEquals(1, body.get(AST).asInt());

        ISecAssn updated = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertEquals(1, updated.getTfvn().intValue());
        assertEquals(ServiceType.AUTHENTICATION, updated.getServiceType());
        assertEquals(0, (short) updated.getEst());
        assertEquals(1, (short) updated.getAst());

        node.put(SA_STATE, 2);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 3);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        node.put(SA_STATE, 1);
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + START, node, ObjectNode.class);
        assertEquals(3, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + STOP, node, ObjectNode.class);
        assertEquals(2, body.get(SA_STATE).asInt());

        body = restTemplate.postForObject(getUrl() + EXPIRE, node, ObjectNode.class);
        assertEquals(1, body.get(SA_STATE).asInt());

        node.put(IV, IV_1);
        node.put(IV_LEN, "16");
        node.put(EKID, "null");
        body = restTemplate.postForObject(getUrl(), node, ObjectNode.class);
        assertNotNull(body);
    }

    @Test
    public void testResetArsnByType() throws KmcException {
        resetArsnByType(FrameType.TC);
        resetArsnByType(FrameType.TM);
        resetArsnByType(FrameType.AOS);
    }

    public void resetArsnByType(FrameType type) throws KmcException {
        createSaByType(type);
        ObjectNode idArsn = mapper.createObjectNode();
        idArsn.withObject(ID_PATH).put(SPI, 100).put(SCID, 46);
        idArsn.put(ARSN_LEN, 8).put(ARSN, "0000000000000001").put(ARSNW, 10);
        idArsn.put(TYPE, type.name());
        ObjectNode body = restTemplate.postForObject(getUrl() + ARSN_PATH + "/" + type.name(), idArsn, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        ISecAssn arsn = dao.getSa(new SpiScid(100, (short) 46), type);
        assertEquals(8, (short) arsn.getArsnLen());
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01}, arsn.getArsn());
        assertEquals(10, (short) arsn.getArsnw());
        assertEquals(type, arsn.getType());

        idArsn.put(ARSN, "02");
        body = restTemplate.postForObject(getUrl() + ARSN_PATH + "/" + type.name(), idArsn, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        arsn = dao.getSa(new SpiScid(100, (short) 46), type);
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02}, arsn.getArsn());

        idArsn.put(ARSN, "000000000000000001");
        body = restTemplate.postForObject(getUrl() + ARSN_PATH + "/" + type.name(), idArsn, ObjectNode.class);
        assertEquals(ERROR, body.get(STATUS).asText());
        arsn = dao.getSa(new SpiScid(100, (short) 46), type);
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02}, arsn.getArsn());
    }

    @Test
    public void testResetArsn() throws KmcException {
        testCreateSa();

        ObjectNode idArsn = mapper.createObjectNode();
        idArsn.withObject(ID_PATH).put(SPI, 100).put(SCID, 46);
        idArsn.put(ARSN_LEN, 8).put(ARSN, "0000000000000001").put(ARSNW, 10);
        ObjectNode body = restTemplate.postForObject(getUrl() + ARSN_PATH, idArsn, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        ISecAssn arsn = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertEquals(8, (short) arsn.getArsnLen());
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01}, arsn.getArsn());
        assertEquals(10, (short) arsn.getArsnw());

        idArsn.put(ARSN, "01");
        body = restTemplate.postForObject(getUrl() + ARSN_PATH, idArsn, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());

        idArsn.put(ARSN, "000000000000000001");
        body = restTemplate.postForObject(getUrl() + ARSN_PATH, idArsn, ObjectNode.class);
        assertEquals(ERROR, body.get(STATUS).asText());
    }

    @Test
    public void testResetIvByType() throws KmcException {
        resetIvByType(FrameType.TC);
        resetIvByType(FrameType.TM);
        resetIvByType(FrameType.AOS);
    }

    public void resetIvByType(FrameType type) throws KmcException {
        createSaByType(type);
        ObjectNode idIv = mapper.createObjectNode();
        idIv.withObject(ID_PATH).put(SPI, 100).put(SCID, 46);
        idIv.put(IV, IV_1);
        idIv.put(IV_LEN, 16);
        ObjectNode body = restTemplate.postForObject(getUrl() + IV_PATH + "/" + type.name(), idIv, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        ISecAssn iv = dao.getSa(new SpiScid(100, (short) 46), type);
        assertEquals(type, iv.getType());
        assertEquals(16, (short) iv.getIvLen());
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
                0x01}, iv.getIv());

        idIv.put(IV, "02");
        body = restTemplate.postForObject(getUrl() + IV_PATH + "/" + type.name(), idIv, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        iv = dao.getSa(new SpiScid(100, (short) 46), type);
        assertEquals(type, iv.getType());
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
                0x02}, iv.getIv());

        idIv.put(IV_LEN, 2);
        idIv.put(IV, "00000001");
        body = restTemplate.postForObject(getUrl() + IV_PATH + "/" + type.name(), idIv, ObjectNode.class);
        assertEquals(ERROR, body.get(STATUS).asText());
    }

    @Test
    public void testResetIv() throws KmcException {
        testCreateSa();
        ObjectNode idIv = mapper.createObjectNode();
        idIv.withObject(ID_PATH).put(SPI, 100).put(SCID, 46);
        idIv.put(IV, IV_1);
        idIv.put(IV_LEN, 16);
        ObjectNode body = restTemplate.postForObject(getUrl() + IV_PATH, idIv, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        ISecAssn iv = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertEquals(16, (short) iv.getIvLen());
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
                0x01}, iv.getIv());

        idIv.put(IV, "02");
        body = restTemplate.postForObject(getUrl() + IV_PATH, idIv, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        iv = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
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
                0x02}, iv.getIv());

        idIv.put(IV_LEN, 2);
        idIv.put(IV, "00000001");
        body = restTemplate.postForObject(getUrl() + IV_PATH, idIv, ObjectNode.class);
        assertEquals(ERROR, body.get(STATUS).asText());
    }

    @Test
    public void testRekeyByType() throws KmcException {
        rekeyByType(FrameType.TC);
        rekeyByType(FrameType.TM);
        rekeyByType(FrameType.AOS);
    }

    public void rekeyByType(FrameType type) throws KmcException {
        createSaByType(type);
        ObjectNode rekey = mapper.createObjectNode();
        rekey.withObject(ID_PATH).put(SPI, 100).put(SCID, 46);
        rekey.put(EKID, BOGUS_EKID);
        ObjectNode body = restTemplate.postForObject(getUrl() + KEY_PATH + "/" + type.name(), rekey, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        ISecAssn keyed = dao.getSa(new SpiScid(100, (short) 46), type);
        assertEquals(type, keyed.getType());
        assertEquals(BOGUS_EKID, keyed.getEkid());
        rekey.put(AKID, BOGUS_AKID);
        rekey.put(EKID, "");
        body = restTemplate.postForObject(getUrl() + KEY_PATH + "/" + type.name(), rekey, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        keyed = dao.getSa(new SpiScid(100, (short) 46), type);
        assertEquals(type, keyed.getType());
        assertEquals(BOGUS_AKID, keyed.getAkid());
        assertEquals("", keyed.getEkid());
        rekey.put(AKID, BOGUS_AKID_2);
        rekey.put(EKID, BOGUS_EKID_2);
        body = restTemplate.postForObject(getUrl() + KEY_PATH + "/" + type.name(), rekey, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        keyed = dao.getSa(new SpiScid(100, (short) 46), type);
        assertEquals(type, keyed.getType());
        assertEquals(BOGUS_AKID_2, keyed.getAkid());
        assertEquals(BOGUS_EKID_2, keyed.getEkid());
    }

    @Test
    public void testRekey() throws KmcException {
        testCreateSa();
        ObjectNode rekey = mapper.createObjectNode();
        rekey.withObject(ID_PATH).put(SPI, 100).put(SCID, 46);
        rekey.put(EKID, BOGUS_EKID);
        ObjectNode body = restTemplate.postForObject(getUrl() + KEY_PATH, rekey, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        ISecAssn keyed = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertEquals(BOGUS_EKID, keyed.getEkid());
        rekey.put(AKID, BOGUS_AKID);
        rekey.put(EKID, "");
        body = restTemplate.postForObject(getUrl() + KEY_PATH, rekey, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        keyed = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertEquals(BOGUS_AKID, keyed.getAkid());
        assertEquals("", keyed.getEkid());
        rekey.put(AKID, BOGUS_AKID_2);
        rekey.put(EKID, BOGUS_EKID_2);
        body = restTemplate.postForObject(getUrl() + KEY_PATH, rekey, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
        keyed = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertEquals(BOGUS_AKID_2, keyed.getAkid());
        assertEquals(BOGUS_EKID_2, keyed.getEkid());
    }

    @Test
    public void testGetCsvByType() throws IOException, KmcException {
        getCsvByType(FrameType.TC);
        getCsvByType(FrameType.TM);
        getCsvByType(FrameType.AOS);
    }

    public void getCsvByType(FrameType type) throws IOException, KmcException {
        createSaByType(type);
        String csvResp = restTemplate.getForObject(getUrl() + CSV_PATH + "/" + type.name(), String.class);
        assertNotNull(csvResp);
        List<String> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvResp))) {
            String line;
            while ((line = reader.readLine()) != null) {
                entries.add(line);
            }
        }
        assertEquals(7, entries.size());
    }

    @Test
    public void testGetCsv() throws IOException, KmcException {
        testCreateSa();
        String csvResp = restTemplate.getForObject(getUrl() + CSV_PATH, String.class);
        assertNotNull(csvResp);
        List<String> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvResp))) {
            String line;
            while ((line = reader.readLine()) != null) {
                entries.add(line);
            }
        }
        assertEquals(17, entries.size());
    }

    @Test
    public void testBulkUpload() throws KmcException {
        testCreateSa();
        // test bulk creating SAs
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> csvBody = new LinkedMultiValueMap<>();
        csvBody.add(FILE, new FileSystemResource(getClass().getClassLoader().getResource("test.csv").getPath()));
        HttpEntity<MultiValueMap<String, Object>> csvUploadReq = new HttpEntity<>(csvBody, header);

        // this should fail with an error response, the SAs already exist
        ObjectNode body = restTemplate.postForObject(getUrl() + CREATE_PATH, csvUploadReq, ObjectNode.class);
        assertEquals(ERROR, body.get(STATUS).asText());
        assertEquals(6, body.get("messages").size());

        // this forces creation for existing, which should succeed
        csvBody.add(FORCE, "true");
        csvUploadReq = new HttpEntity<>(csvBody, header);
        body = restTemplate.postForObject(getUrl() + CREATE_PATH, csvUploadReq, ObjectNode.class);
        assertEquals(SUCCESS, body.get(STATUS).asText());
    }

    @Test
    public void testDeleteSaByType() throws KmcException {
        deleteSaByType(FrameType.TC);
        deleteSaByType(FrameType.TM);
        deleteSaByType(FrameType.AOS);
    }

    public void deleteSaByType(FrameType type) throws KmcException {
        createSaByType(type);
        ISecAssn present = dao.getSa(new SpiScid(100, (short) 46), type);
        assertNotNull(present);
        assertEquals(type, present.getType());

        ArrayNode anode = mapper.createArrayNode();
        ObjectNode node = anode.addObject();
        node.put(SPI, 100);
        node.put(SCID, 46);
        HttpEntity<JsonNode> entity = new HttpEntity<>(anode);
        restTemplate.exchange(getUrl() + "/" + type.name(), HttpMethod.DELETE, entity, JsonNode.class);
        ISecAssn deleted = dao.getSa(new SpiScid(100, (short) 46), type);
        assertNull(deleted);
    }

    @Test
    public void testDeleteSa() throws KmcException {
        testCreateSa();
        ArrayNode anode = mapper.createArrayNode();
        ObjectNode node = anode.addObject();
        node.put(SPI, 100);
        node.put(SCID, 46);
        HttpEntity<JsonNode> entity = new HttpEntity<>(anode);
        restTemplate.exchange(getUrl(), HttpMethod.DELETE, entity, JsonNode.class);
        ISecAssn deleted = dao.getSa(new SpiScid(100, (short) 46), FrameType.TC);
        assertNull(deleted);
    }

}