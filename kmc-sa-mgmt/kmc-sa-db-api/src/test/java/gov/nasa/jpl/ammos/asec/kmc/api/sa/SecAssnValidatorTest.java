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
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn((byte[]) null, (short) 0));
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn(new byte[0], (short) 0));
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn((byte[]) null, null));
    }

    @Test
    public void verifyArsnLenZeroWithZeroBytesTolerated() throws KmcException {
        assertArrayEquals(new byte[0], SecAssnValidator.verifyArsn(new byte[20], (short) 0));
    }

    @Test
    public void verifyArsnLenZeroWithValueFails() {
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(new byte[]{1, 2}, (short) 0));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(new byte[]{1, 2}, null));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(new byte[]{1}, (short) 0));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(new byte[]{0, 0, 1}, (short) 0));
    }

    @Test
    public void verifyArsnLenPositiveMismatchFails() {
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(new byte[]{1, 2, 3}, (short) 5));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(new byte[]{1, 2, 3}, (short) 2));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn((byte[]) null, (short) 5));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyArsn(new byte[0], (short) 5));
    }

    @Test
    public void verifyArsnLenPositiveMatch() throws KmcException {
        assertArrayEquals(new byte[]{1, 2, 3}, SecAssnValidator.verifyArsn(new byte[]{1, 2, 3}, (short) 3));
    }

    @Test
    public void verifyArsnNotHexFails() {
        assertThrows(KmcException.class, () -> SecAssnValidator.convertHexToBytes("zz", "ARSN must be a hex string"));
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

    @Test
    public void verifyArsnSaNormalizesZeroArsn() throws KmcException {
        ISecAssn sa = new SecAssn(new SpiScid(1, (short) 44));
        sa.setArsnLen((short) 0);
        sa.setArsn(new byte[20]);
        SecAssnValidator.verifyArsn(sa);
        assertArrayEquals(new byte[0], sa.getArsn());
        sa.setArsnLen((short) 3);
        sa.setArsn(new byte[]{0, 0, 1});
        SecAssnValidator.verifyArsn(sa);
        assertArrayEquals(new byte[]{0, 0, 1}, sa.getArsn());
    }

    private static ISecAssn saWithIv(byte[] iv, short ivLen, short est, short ast) {
        ISecAssn sa = new SecAssn(new SpiScid(1, (short) 44));
        sa.setTfvn((byte) 0);
        sa.setVcid((byte) 0);
        sa.setMapid((byte) 0);
        sa.setSaState((short) 1);
        sa.setEcs(new byte[]{0x01});
        sa.setIvLen(ivLen);
        sa.setIv(iv);
        sa.setEst(est);
        sa.setAst(ast);
        return sa;
    }

    @Test
    public void validatePlaintextAndAuthOnlyWithoutIvAcceptAnyIvLen() throws KmcException {
        SecAssnValidator.validate(saWithIv(new byte[0], (short) 0, (short) 0, (short) 0));
        SecAssnValidator.validate(saWithIv(new byte[0], (short) 16, (short) 0, (short) 1));
        SecAssnValidator.validate(saWithIv(new byte[0], (short) 12, (short) 1, (short) 1));
    }

    @Test
    public void validatePlaintextAndAuthOnlyWithMatchingIvAccepted() throws KmcException {
        SecAssnValidator.validate(saWithIv(new byte[12], (short) 12, (short) 0, (short) 0));
        SecAssnValidator.validate(saWithIv(new byte[16], (short) 16, (short) 0, (short) 1));
    }

    @Test
    public void validatePlaintextWithStaleIvRejected() {
        // a stored IV that disagrees with iv_len is rejected even when encryption is off (AKMC-380)
        assertThrows(KmcException.class,
                () -> SecAssnValidator.validate(saWithIv(new byte[12], (short) 16, (short) 0, (short) 0)));
        assertThrows(KmcException.class,
                () -> SecAssnValidator.validate(saWithIv(new byte[12], (short) 0, (short) 0, (short) 1)));
    }

    @Test
    public void verifyVcidBoundsPerFrameType() throws KmcException {
        SecAssnValidator.verifyVcid(FrameType.TC, (byte) 63);
        SecAssnValidator.verifyVcid(FrameType.AOS, (byte) 63);
        SecAssnValidator.verifyVcid(FrameType.TM, (byte) 7);
        SecAssnValidator.verifyVcid(FrameType.TM, (byte) 0);
        SecAssnValidator.verifyVcid(FrameType.TC, null);
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyVcid(FrameType.TC, (byte) 64));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyVcid(FrameType.AOS, (byte) 64));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyVcid(FrameType.TM, (byte) 8));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyVcid(FrameType.TC, (byte) -1));
    }

    @Test
    public void verifyVcidSa() throws KmcException {
        ISecAssn tm = new SecAssnTm(new SpiScid(1, (short) 44));
        tm.setVcid((byte) 8);
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyVcid(tm));
        ISecAssn tc = new SecAssn(new SpiScid(1, (short) 44));
        tc.setVcid((byte) 8);
        SecAssnValidator.verifyVcid(tc);
    }

    @Test
    public void verifyScidBoundsPerFrameType() throws KmcException {
        SecAssnValidator.verifyScid(FrameType.TC, (short) 1023);
        SecAssnValidator.verifyScid(FrameType.TM, (short) 1023);
        SecAssnValidator.verifyScid(FrameType.AOS, (short) 255);
        SecAssnValidator.verifyScid(FrameType.AOS, (short) 0);
        SecAssnValidator.verifyScid(FrameType.TC, null);
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyScid(FrameType.TC, (short) 1024));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyScid(FrameType.TM, (short) 1024));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyScid(FrameType.AOS, (short) 256));
        assertThrows(KmcException.class, () -> SecAssnValidator.verifyScid(FrameType.TC, (short) -1));
    }

    @Test
    public void validateRejectsOutOfRangeScid() {
        ISecAssn aos = new SecAssnAos(new SpiScid(1, (short) 256));
        assertThrows(KmcException.class, () -> SecAssnValidator.validate(aos));
    }
}
