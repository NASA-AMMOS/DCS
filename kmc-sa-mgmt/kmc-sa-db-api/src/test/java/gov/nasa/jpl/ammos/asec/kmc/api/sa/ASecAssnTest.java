package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import org.junit.Test;

import static org.junit.Assert.*;

public class ASecAssnTest {

    @Test
    public void testToString() {
        ISecAssn tc = new SecAssn(new SpiScid(1, (short) 44));
        tc.setTfvn((byte) 0);
        tc.setVcid((byte) 0);
        assertEquals("SA [TC, 1, 44, 0, 0]", tc.toString());

        ISecAssn tm = new SecAssnTm(new SpiScid(1, (short) 44));
        tm.setTfvn((byte) 0);
        tm.setVcid((byte) 0);
        assertEquals("SA [TM, 1, 44, 0, 0]", tm.toString());

        ISecAssn aos = new SecAssnAos(new SpiScid(1, (short) 44));
        aos.setTfvn((byte) 0);
        aos.setVcid((byte) 0);
        assertEquals("SA [AOS, 1, 44, 0, 0]", aos.toString());
    }

}