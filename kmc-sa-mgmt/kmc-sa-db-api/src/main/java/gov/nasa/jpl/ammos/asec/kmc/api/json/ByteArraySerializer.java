package gov.nasa.jpl.ammos.asec.kmc.api.json;

import tools.jackson.core.JsonGenerator;
import org.apache.commons.codec.binary.Hex;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Serializes byte arrays to hex strings for JSON
 */
public class ByteArraySerializer extends ValueSerializer<byte[]> {

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializationContext serializers) {
        gen.writePOJO(Hex.encodeHexString(value));
    }
}
