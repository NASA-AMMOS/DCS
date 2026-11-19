package gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import tools.jackson.databind.ValueDeserializer;

public class SecAssnDeserializer extends ValueDeserializer<ISecAssn> {
    private static ObjectMapper M;

    static {
        M = new JsonMapper();
    }

    @Override
    public ISecAssn deserialize(JsonParser p, DeserializationContext ctxt) {
        ObjectNode root = p.readValueAs(ObjectNode.class);
        FrameType  type;
        if (root.has("type")) {
            type = FrameType.fromString(root.get("type").asString());
            if (type == FrameType.UNKNOWN) {
                throw new InvalidFormatException(p, String.format("Frame type %s is invalid",
                        root.get("type").asString()), root, ISecAssn.class);
            }
        } else {
            type = FrameType.TC;
        }
        return M.convertValue(root, type.getClazz());
    }
}
