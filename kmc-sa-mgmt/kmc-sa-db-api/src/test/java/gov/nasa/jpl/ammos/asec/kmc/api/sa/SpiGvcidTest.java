package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

public class SpiGvcidTest {
    @Test
    public void testToString() {
        ObjectMapper mapper   = new JsonMapper();
        SpiGvcid     spiGvcid = new SpiGvcid(1, (byte) 0, (short) 44, (byte) 1, (byte) 0);
        JsonNode     got      = mapper.readTree(spiGvcid.toString());
        JsonNode expect = mapper.createObjectNode()
                .put("spi", 1)
                .put("scid", 44)
                .put("vcid", 1)
                .put("mapid", 0)
                .put("tfvn", 0);

        assertTrue(expect.equals(got));
    }
}