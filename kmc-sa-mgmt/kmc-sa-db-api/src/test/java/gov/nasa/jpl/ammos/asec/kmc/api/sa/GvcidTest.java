package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class GvcidTest {
    @Test
    public void testToString() {
        Gvcid gvcid = new Gvcid();
        gvcid.setMapid((byte) 0);
        gvcid.setScid((short) 44);
        gvcid.setTfvn((byte) 0);
        gvcid.setVcid((byte) 1);
        assertEquals("{\"tfvn\":0,\"scid\":44,\"vcid\":1,\"mapid\":0}", gvcid.toString());
    }
}