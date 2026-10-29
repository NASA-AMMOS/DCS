package gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnAos;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnTm;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SecAssnDeserializerTest {
    private static final ObjectMapper mapper;

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ISecAssn.class, new SecAssnDeserializer());
        mapper = JsonMapper.builder()
                .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .addModule(module)
                .build();
    }

    private ObjectNode createSaJson() {
        ObjectNode node = mapper.createObjectNode();
        node.withObject("/id").put("spi", 100).put("scid", 46);
        node.put("tfvn", 0).put("vcid", 20).put("mapid", 0).put("ekid", "kmc/test/key128").putNull(
                "akid").put("saState", 3).putNull("lpid").put("est", 1).put("ast", 1).put("shivfLen", 12).put(
                "shsnfLen", 0).put("shplfLen", 0).put("stmacfLen", 16).put("ecsLen", 1).put("ecs", "01").put(
                "ivLen", 12).put("iv", "000000000000000000000001").put("acsLen", 1).put("acs", "00").put("abmLen",
                19).put("abm", "ffffffffffffff000000000000000000000000").put("arsnLen", 0).put("arsn", "").put("arsnw"
                , 5).put("spi", 100).put("scid", 46).put("serviceType", "AUTHENTICATED_ENCRYPTION");
        return node;
    }

    private ISecAssn convertObject(ObjectNode n) {
        return mapper.convertValue(n, ISecAssn.class);
    }

    @Test
    public void testSerialize() throws IOException {
        testSerializeType(FrameType.TC);
        testSerializeType(FrameType.TM);
        testSerializeType(FrameType.AOS);
    }

    public void testSerializeType(FrameType type) throws IOException {
        ObjectNode n = createSaJson();
        n = n.put("type", type.name().toLowerCase());
        ISecAssn sa = convertObject(n);
        assertEquals(type, sa.getType());
        switch (type) {
            case TC -> assertTrue(sa instanceof SecAssn);
            case TM -> assertTrue(sa instanceof SecAssnTm);
            case AOS -> assertTrue(sa instanceof SecAssnAos);
            default -> fail("unknown frame type");
        }
    }

    @Test
    public void testSerializeDefaultTc() {
        ObjectNode n  = createSaJson();
        ISecAssn   sa = convertObject(n);
        assertEquals(FrameType.TC, sa.getType());
        assertTrue(sa instanceof SecAssn);
    }

    @Test
    public void testSerializeUnknown() {
        assertThrows(InvalidFormatException.class, () -> {
            ObjectNode n = createSaJson();
            n = n.put("type", "blah");
            convertObject(n);
        });
    }

    @Test
    public void testDeserializeByType() {
        testDeserialize(FrameType.TC);
        testDeserialize(FrameType.TM);
        testDeserialize(FrameType.AOS);
    }

    public void testDeserialize(FrameType type) {
        ObjectNode n = createSaJson();
        n.put("type", type.name());
        ISecAssn   sa    = convertObject(n);
        ObjectNode deser = mapper.convertValue(sa, ObjectNode.class);
        assertTrue(deser.has("type"));
        assertEquals(type.name(), deser.get("type").stringValue());
    }
}