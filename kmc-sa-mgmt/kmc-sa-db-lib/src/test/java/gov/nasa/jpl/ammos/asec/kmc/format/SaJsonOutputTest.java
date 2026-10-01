package gov.nasa.jpl.ammos.asec.kmc.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnFactory;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * JSON output tests
 */
public class SaJsonOutputTest {

    private JsonNode render(List<ISecAssn> sas) throws Exception {
        StringWriter w = new StringWriter();
        try (PrintWriter pw = new PrintWriter(w)) {
            new SaJsonOutput().print(pw, sas);
        }
        return new ObjectMapper().readTree(w.toString());
    }

    @Test
    public void testEmptyListIsValidJsonArray() throws Exception {
        JsonNode node = render(Collections.emptyList());
        assertTrue(node.isArray());
        assertEquals(0, node.size());
    }

    @Test
    public void testSingleSaIsValidJsonArray() throws Exception {
        ISecAssn sa = SecAssnFactory.createSecAssn(new SpiScid(1, (short) 44), FrameType.TC);
        sa.setTfvn((byte) 0);
        sa.setVcid((byte) 0);
        sa.setMapid((byte) 0);

        JsonNode node = render(List.of(sa));
        assertTrue(node.isArray());
        assertEquals(1, node.size());
        assertEquals(1, node.get(0).get("spi").asInt());
        assertEquals("TC", node.get(0).get("type").asText());
    }

    @Test
    public void testMultipleSasIsValidJsonArray() throws Exception {
        ISecAssn sa1 = SecAssnFactory.createSecAssn(new SpiScid(1, (short) 44), FrameType.TC);
        sa1.setTfvn((byte) 0);
        sa1.setVcid((byte) 0);
        sa1.setMapid((byte) 0);
        ISecAssn sa2 = SecAssnFactory.createSecAssn(new SpiScid(2, (short) 44), FrameType.TC);
        sa2.setTfvn((byte) 0);
        sa2.setVcid((byte) 1);
        sa2.setMapid((byte) 0);

        JsonNode node = render(List.of(sa1, sa2));
        assertTrue(node.isArray());
        assertEquals(2, node.size());
        assertEquals(1, node.get(0).get("spi").asInt());
        assertEquals(2, node.get(1).get("spi").asInt());
    }
}
