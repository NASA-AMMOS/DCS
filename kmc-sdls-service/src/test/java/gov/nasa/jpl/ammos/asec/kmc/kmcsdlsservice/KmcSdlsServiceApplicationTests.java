package gov.nasa.jpl.ammos.asec.kmc.kmcsdlsservice;

import tools.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
// @ActiveProfiles("unittest")
@TestPropertySource(locations = "classpath:kmc-sdls-service-unittest.properties")
public class KmcSdlsServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void contextLoads() {
    }

    @Test
    public void testHealthEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/health")).andReturn();
        int status = result.getResponse().getStatus();
        assertEquals(200, status);
    }

    @Test
    public void testStatusEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/status")).andReturn();
        int status = result.getResponse().getStatus();
        assertEquals(200, status);
    }


    @Test
    public void testApplySecurityEndpoint() throws Exception {
        byte[] tcFrame = Hex.decodeHex("20030015000080d2c70008197f0b00310000b1fe3128");

        MvcResult result = mockMvc.perform(post("/apply_security").contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE).content(tcFrame)).andReturn();
        String errorMessage = result.getResponse().getErrorMessage();
        System.out.println(errorMessage);
        byte[] bytesSdlsAppliedTC = result.getResponse().getContentAsByteArray();

        String stringSdlsAppliedTc = Hex.encodeHexString(bytesSdlsAppliedTC,true);
        assertEquals("200300230000000100000000000000000000000080d2c70008197f0b00310000b1fefe0f",stringSdlsAppliedTc);
    }

    @Test
    public void testProcessSecurityEndpoint() throws Exception {
        byte[] tcSdlsFrame = Hex.decodeHex("200300230000000100000000000000000000000080d2c70008197f0b00310000b1fefe0f");

        mockMvc.perform(post("/process_security").contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE).content(tcSdlsFrame))
                .andExpect(jsonPath("$.tc_pdu").value("80D2C70008197F0B00310000B1FE"))
                .andExpect(jsonPath("$.tc_pdu_len").value(14))
                .andExpect(jsonPath("$.spi").value(1))
                .andExpect(jsonPath("$.fecf").value(0xFE0F));

    }

}
