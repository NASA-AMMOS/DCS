package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

public class SecAssnValidatorTest {
    private static final byte[] GCM = ECSTYPE.AES_GCM.getValue();
    private static final byte[] CBC = ECSTYPE.AES_CBC.getValue();

    @Test
    public void verifyIvNullIvLenDoesNotThrow() throws KmcException {
        SecAssnValidator.verifyIv(new byte[12], null, (short) 1, GCM);
        SecAssnValidator.verifyIv(new byte[12], null, null, null);
    }

    @Test
    public void verifyIvEmptyOrNullIvAlwaysValid() throws KmcException {
        SecAssnValidator.verifyIv((byte[]) null, (short) 16, (short) 0, null);
        SecAssnValidator.verifyIv(new byte[0], (short) 16, (short) 0, null);
        SecAssnValidator.verifyIv(new byte[0], (short) 12, (short) 1, GCM);
    }

    @Test
    public void verifyIvMismatchWithoutEcs() {
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyIv(new byte[12], (short) 16, null, null));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyIv(new byte[12], (short) 16, (short) 0, null));
    }

    @Test
    public void verifyIvMatchWithoutEcs() throws KmcException {
        SecAssnValidator.verifyIv(new byte[16], (short) 16, null, null);
        SecAssnValidator.verifyIv(new byte[16], (short) 16, (short) 0, new byte[]{0x7f});
    }

    @Test
    public void verifyIvMismatchWithEcs() {
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyIv(new byte[12], (short) 16, (short) 1, CBC));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyIv(new byte[16], (short) 16, (short) 1, GCM));
    }

    @Test
    public void verifyIvMatchWithEcs() throws KmcException {
        SecAssnValidator.verifyIv(new byte[16], (short) 16, (short) 1, CBC);
        SecAssnValidator.verifyIv(new byte[12], (short) 12, (short) 1, GCM);
    }

    @Test
    public void verifyArsnLenZero() throws KmcException {
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn((String) null, (short) 0));
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn("", (short) 0));
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn("0x", (short) 0));
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn((String) null, null));
    }

    @Test
    public void verifyArsnLenZeroWithZeroBytesTolerated() throws KmcException {
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn(new byte[20], (short) 0));
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn("0x" + "00".repeat(20), (short) 0));
    }

    @Test
    public void verifyArsnLenZeroWithValueFails() {
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn("0102", (short) 0));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn("0102", null));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(new byte[]{1}, (short) 0));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(new byte[]{0, 0, 1}, (short) 0));
    }

    @Test
    public void verifyArsnLenPositiveMismatchFails() {
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn("010203", (short) 5));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn("010203", (short) 2));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn((String) null, (short) 5));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn("0x", (short) 5));
    }

    @Test
    public void verifyArsnLenPositiveMatch() throws KmcException {
        assertArrayEquals(new byte[]{1, 2, 3}, SecAssnValidator.verifyArsn("010203", (short) 3));
        assertArrayEquals(new byte[]{1, 2, 3}, SecAssnValidator.verifyArsn("0x010203", (short) 3));
    }

    @Test
    public void verifyArsnNotHexFails() {
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn("zz", (short) 1));
    }

    @Test
    public void verifyArsnSaDefaultsValid() throws KmcException {
        SecAssnValidator.verifyArsn(new SecAssn(new SpiScid(1, (short) 44)));
    }

    @Test
    public void verifyArsnSaMismatchFails() {
        ISecAssn sa = new SecAssnTm(new SpiScid(1, (short) 44));
        sa.setArsnLen((short) 5);
        sa.setArsn(new byte[3]);
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(sa));
    }
}
