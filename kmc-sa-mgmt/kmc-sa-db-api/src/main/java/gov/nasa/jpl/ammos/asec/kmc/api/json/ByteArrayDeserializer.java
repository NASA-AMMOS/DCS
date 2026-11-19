package gov.nasa.jpl.ammos.asec.kmc.api.json;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserializes byte arrays to hex strings for JSON
 */
public class ByteArrayDeserializer extends ValueDeserializer<byte[]> {
    @Override
    public byte[] deserialize(JsonParser p, DeserializationContext ctxt) {
        try {
            return Hex.decodeHex(p.getString());
        } catch (DecoderException e) {
            return (byte[]) ctxt.handleWeirdStringValue(byte[].class, p.getString(), "Cannot decode hex: %s", e.getMessage());
        }
    }
}
