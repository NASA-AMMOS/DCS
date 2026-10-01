package gov.nasa.jpl.ammos.asec.kmc.sadb;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcStartException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnAos;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnTm;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IDbSession;
import gov.nasa.jpl.ammos.asec.kmc.format.SaCsvInput;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for KMC DAO
 */
public class KmcDaoTest extends BaseH2Test {

    @Test(expected = KmcException.class)
    public void testConnectionFail() throws KmcException {
        try (KmcDao fail = new KmcDao()) {
            fail.init();
        }
    }

    public void createDeleteSa(FrameType type) throws KmcException {
        dao.createSa(6, (byte) 0, (short) 255, (byte) 1, (byte) 0, type);
        List<? extends ISecAssn> sas = dao.getSas(type);
        assertNotNull(sas);
        assertEquals(6, sas.size());
        ISecAssn sa = sas.get(5);
        assertEquals(6, (int) sa.getSpi());
        assertEquals(1, (short) sa.getSaState());
        dao.deleteSa(sas.get(5).getId(), type);
        sas = dao.getSas(type);
        assertNotNull(sas);
        assertEquals(5, sas.size());
    }

    @Test
    public void testCreateDeleteSa() throws KmcException {
        createDeleteSa(FrameType.TC);
    }

    @Test
    public void testCreateDeleteSaTm() throws KmcException {
        createDeleteSa(FrameType.TM);
    }

    @Test
    public void testCreateDeleteSaAos() throws KmcException {
        createDeleteSa(FrameType.AOS);
    }

    public void createDeleteSa2(FrameType type) throws KmcException {
        ISecAssn sa;
        switch (type) {
            case TM -> sa = new SecAssnTm(new SpiScid(6, (short) 255));
            case AOS -> sa = new SecAssnAos(new SpiScid(6, (short) 255));
            default -> sa = new SecAssn(new SpiScid(6, (short) 255));
        }
        sa.setTfvn((byte) 0);
        sa.setVcid((byte) 1);
        sa.setMapid((byte) 0);
        dao.createSa(sa);
        List<? extends ISecAssn> sas = dao.getSas(type);
        assertNotNull(sas);
        assertEquals(6, sas.size());
        sa = sas.get(5);
        assertEquals(6, (int) sa.getSpi());
        assertEquals(1, (short) sa.getSaState());
        assertEquals(255, (short) sa.getScid());
        assertEquals(1, (short) sa.getVcid());
        dao.deleteSa(sa.getId(), type);
        sas = dao.getSas(type);
        assertEquals(5, sas.size());
    }

    @Test
    public void testCreateDeleteSa2() throws KmcException {
        createDeleteSa2(FrameType.TC);
    }

    @Test
    public void testCreateDeleteSa2Aos() throws KmcException {
        createDeleteSa2(FrameType.AOS);
    }

    @Test
    public void testCreateDeleteSa2Tm() throws KmcException {
        createDeleteSa2(FrameType.TM);
    }

    public void createDeleteSaNonNullSpi(FrameType type) throws KmcException {
        dao.createSa(null, (byte) 0, (short) 255, (byte) 1, (byte) 0, type);
        List<? extends ISecAssn> sas = dao.getSas(type);
        assertNotNull(sas);
        assertEquals(6, sas.size());
        ISecAssn sa = sas.get(5);
        assertEquals(1, (int) sa.getSpi());
        assertEquals(1, (short) sa.getSaState());
        assertEquals(255, (short) sa.getScid());
        dao.deleteSa(sas.get(5).getId(), type);
        sas = dao.getSas(type);
        assertNotNull(sas);
        assertEquals(5, sas.size());
    }

    @Test
    public void testCreateDeleteSaNonNullSpi() throws KmcException {
        createDeleteSaNonNullSpi(FrameType.TC);
    }

    @Test
    public void testCreateDeleteSaNonNullSpiAos() throws KmcException {
        createDeleteSaNonNullSpi(FrameType.AOS);
    }

    @Test
    public void testCreateDeleteSaNonNullSpiTm() throws KmcException {
        createDeleteSaNonNullSpi(FrameType.TM);
    }

    public void deleteDne(FrameType type) throws KmcException {
        SpiScid fake = new SpiScid(10, (short) 46);
        dao.deleteSa(fake, type);
    }

    @Test(expected = KmcException.class)
    public void testDeleteDne() throws KmcException {
        deleteDne(FrameType.TC);
    }

    @Test(expected = KmcException.class)
    public void testDeleteDneAos() throws KmcException {
        deleteDne(FrameType.AOS);
    }

    @Test(expected = KmcException.class)
    public void testDeleteDneTm() throws KmcException {
        deleteDne(FrameType.TM);
    }

    public void startSa(FrameType type) throws KmcException {
        SpiScid id1 = new SpiScid(8, (short) 46);
        SpiScid id2 = new SpiScid(9, (short) 46);
        dao.createSa(8, (byte) 0, (short) 46, (byte) 10, (byte) 0, type);
        dao.createSa(9, (byte) 0, (short) 46, (byte) 10, (byte) 0, type);
        dao.startSa(id1, false, type);
        ISecAssn sa1 = dao.getSa(id1, type);
        ISecAssn sa2 = dao.getSa(id2, type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa1.getSaState());
        assertEquals(KmcDao.SA_UNKEYED, (short) sa2.getSaState());
    }

    @Test
    public void testStartSa() throws KmcException {
        startSa(FrameType.TC);
    }

    @Test
    public void testStartSaAos() throws KmcException {
        startSa(FrameType.AOS);
    }

    @Test
    public void testStartSaTm() throws KmcException {
        startSa(FrameType.TM);
    }

    public void startSaAlreadyActive(FrameType type) throws KmcException {
        SpiScid id2 = new SpiScid(9, (short) 46);
        testStartSa();
        dao.startSa(id2, false, type);
        ISecAssn sa2 = dao.getSa(id2, type);
        assertEquals(KmcDao.SA_UNKEYED, (short) sa2.getSaState());
    }

    @Test(expected = KmcStartException.class)
    public void testStartSaAlreadyActive() throws KmcException {
        startSaAlreadyActive(FrameType.TC);
    }

    @Test(expected = KmcStartException.class)
    public void testStartSaAlreadyActiveAos() throws KmcException {
        startSaAlreadyActive(FrameType.AOS);
    }

    @Test(expected = KmcStartException.class)
    public void testStartSaAlreadyActiveTm() throws KmcException {
        startSaAlreadyActive(FrameType.TM);
    }

    public void createDupe(FrameType type) throws KmcException {
        dao.createSa(8, (byte) 0, (short) 46, (byte) 1, (byte) 0, type);
        dao.createSa(8, (byte) 0, (short) 46, (byte) 1, (byte) 0, type);
    }

    @Test(expected = KmcException.class)
    public void testCreateDupe() throws KmcException {
        createDupe(FrameType.TC);
    }

    @Test(expected = KmcException.class)
    public void testCreateDupeAos() throws KmcException {
        createDupe(FrameType.AOS);
    }

    @Test(expected = KmcException.class)
    public void testCreateDupeTm() throws KmcException {
        createDupe(FrameType.TM);
    }

    public void startSaDne(FrameType type) throws KmcException {
        SpiScid fake = new SpiScid(10, (short) 46);
        dao.startSa(fake, false, type);
    }

    @Test(expected = KmcException.class)
    public void testStartSaDne() throws KmcException {
        startSaDne(FrameType.TC);
    }

    @Test(expected = KmcException.class)
    public void testStartSaDneAos() throws KmcException {
        startSaDne(FrameType.AOS);
    }

    @Test(expected = KmcException.class)
    public void testStartSaDneTm() throws KmcException {
        startSaDne(FrameType.TM);
    }

    public void stopSaDne(FrameType type) throws KmcException {
        SpiScid fake = new SpiScid(10, (short) 46);
        dao.stopSa(fake, type);
    }

    @Test(expected = KmcException.class)
    public void testStopSaDne() throws KmcException {
        stopSaDne(FrameType.TC);
    }

    @Test(expected = KmcException.class)
    public void testStopSaDneAos() throws KmcException {
        stopSaDne(FrameType.AOS);
    }

    @Test(expected = KmcException.class)
    public void testStopSaDneTm() throws KmcException {
        stopSaDne(FrameType.TM);
    }

    public void expireSaDne(FrameType type) throws KmcException {
        SpiScid fake = new SpiScid(10, (short) 46);
        dao.expireSa(fake, type);
    }

    @Test(expected = KmcException.class)
    public void testExpireSaDne() throws KmcException {
        expireSaDne(FrameType.TC);
    }

    @Test(expected = KmcException.class)
    public void testExpireSaDneAos() throws KmcException {
        expireSaDne(FrameType.AOS);
    }

    @Test(expected = KmcException.class)
    public void testExpireSaDneTm() throws KmcException {
        expireSaDne(FrameType.TM);
    }

    public void rekeyAuthSaDne(FrameType type) throws KmcException {
        SpiScid fake = new SpiScid(10, (short) 46);
        dao.rekeySaAuth(fake, "0", new byte[]{0x01}, (short) 2, type);
    }

    @Test(expected = KmcException.class)
    public void testRekeyAuthSaDne() throws KmcException {
        rekeyAuthSaDne(FrameType.TC);
    }

    @Test(expected = KmcException.class)
    public void testRekeyAuthSaDneAos() throws KmcException {
        rekeyAuthSaDne(FrameType.AOS);
    }

    @Test(expected = KmcException.class)
    public void testRekeyAuthSaDneTm() throws KmcException {
        rekeyAuthSaDne(FrameType.TM);
    }

    public void rekeyEncSaDne(FrameType type) throws KmcException {
        SpiScid fake = new SpiScid(10, (short) 46);
        dao.rekeySaEnc(fake, "130", new byte[]{0x01}, (short) 1, type);
    }

    @Test(expected = KmcException.class)
    public void testRekeyEncSaDne() throws KmcException {
        rekeyEncSaDne(FrameType.TC);
    }

    @Test(expected = KmcException.class)
    public void testRekeyEncSaDneAos() throws KmcException {
        rekeyEncSaDne(FrameType.AOS);
    }

    @Test(expected = KmcException.class)
    public void testRekeyEncSaDneTm() throws KmcException {
        rekeyEncSaDne(FrameType.TM);
    }

    public void stopSa(FrameType type) throws KmcException {
        SpiScid  id1 = new SpiScid(1, (short) 46);
        ISecAssn sa1 = dao.getSa(id1, type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa1.getSaState());
        dao.stopSa(id1, type);
        sa1 = dao.getSa(id1, type);
        assertEquals(KmcDao.SA_KEYED, (short) sa1.getSaState());
    }

    @Test
    public void testStopSa() throws KmcException {
        stopSa(FrameType.TC);
    }

    @Test
    public void testStopSaAos() throws KmcException {
        stopSa(FrameType.AOS);
    }

    @Test
    public void testStopSaTm() throws KmcException {
        stopSa(FrameType.TM);
    }

    public void expireSa(FrameType type) throws KmcException {
        ISecAssn sa = dao.createSa(8, (byte) 0, (short) 255, (byte) 1, (byte) 0, type);
        dao.rekeySaEnc(sa.getId(), "130", new byte[]{0x01}, (short) 1, type);
        dao.rekeySaAuth(sa.getId(), "0", new byte[]{0x00}, (short) 0, type);
        dao.startSa(sa.getId(), false, type);
        sa = dao.getSa(sa.getId(), type);
        assertEquals(KmcDao.SA_OPERATIONAL, (short) sa.getSaState());
        dao.expireSa(sa.getId(), type);
        sa = dao.getSa(sa.getId(), type);
        assertEquals(KmcDao.SA_UNKEYED, (short) sa.getSaState());
        assertNull(sa.getAkid());
        assertNull(sa.getEkid());
    }

    @Test
    public void testExpireSa() throws KmcException {
        expireSa(FrameType.TC);
    }

    @Test
    public void testExpireSaAos() throws KmcException {
        expireSa(FrameType.AOS);
    }

    @Test
    public void testExpireSaTm() throws KmcException {
        expireSa(FrameType.TM);
    }

    public void listSas(FrameType type) throws KmcException {
        List<? extends ISecAssn> sas = dao.getSas(type);
        assertNotNull(sas);
        assertEquals(5, sas.size());
    }

    @Test
    public void testListSas() throws KmcException {
        listSas(FrameType.TC);
    }

    @Test
    public void testListSasAos() throws KmcException {
        listSas(FrameType.AOS);
    }

    @Test
    public void testListSasTm() throws KmcException {
        listSas(FrameType.TM);
    }

    public void getSa(FrameType type) throws KmcException {
        SpiScid  id = new SpiScid(1, (short) 46);
        ISecAssn sa = dao.getSa(id, type);
        assertNotNull(sa);
        assertEquals(1, (int) sa.getSpi());
        assertEquals("130", sa.getEkid());
        assertNull(sa.getAkid());
        assertEquals((Byte) ((byte) 0), sa.getTfvn());
    }

    @Test
    public void testGetSa() throws KmcException {
        getSa(FrameType.TC);
    }

    @Test
    public void testGetSaAos() throws KmcException {
        getSa(FrameType.AOS);
    }

    @Test
    public void testGetSaTm() throws KmcException {
        getSa(FrameType.TM);
    }

    public void getActiveSas(FrameType type) throws KmcException {
        List<? extends ISecAssn> sas = dao.getActiveSas(type);
        assertNotNull(sas);
        assertEquals(5, sas.size());
        for (ISecAssn sa : sas) {
            assertEquals((short) 3, (short) sa.getSaState());
        }
    }

    @Test
    public void testGetActiveSas() throws KmcException {
        getActiveSas(FrameType.TC);
    }

    @Test
    public void testGetActiveSasAos() throws KmcException {
        getActiveSas(FrameType.AOS);
    }

    @Test
    public void testGetActiveSasTm() throws KmcException {
        getActiveSas(FrameType.TM);
    }

    public void updateSa(FrameType type) throws KmcException {
        SpiScid  id = new SpiScid(1, (short) 46);
        ISecAssn sa = dao.getSa(id, type);
        sa.setTfvn((byte) 1);
        dao.updateSa(sa);
        sa = dao.getSa(id, type);
        assertEquals((byte) 1, (byte) sa.getTfvn());
        assertNotNull(sa);
    }

    @Test
    public void testUpdateSa() throws KmcException {
        updateSa(FrameType.TC);
    }

    @Test
    public void testUpdateSaAos() throws KmcException {
        updateSa(FrameType.AOS);
    }

    @Test
    public void testUpdateSaTm() throws KmcException {
        updateSa(FrameType.TM);
    }

    public void rollbackUpdate(FrameType type) throws KmcException {
        assertTrue(dao.status());
        SpiScid  id = new SpiScid(1, (short) 46);
        ISecAssn sa = dao.getSa(id, type);
        assertNotNull(sa);
        sa.setTfvn((byte) 1);
        IDbSession session = dao.newSession();
        session.beginTransaction();
        dao.updateSa(session, sa);
        sa.setTfvn((byte) 2);
        dao.updateSa(session, sa);
        session.rollback();
        sa = dao.getSa(id, type);
        assertEquals((byte) 0, (byte) sa.getTfvn());
    }

    @Test
    public void testRollbackUpdate() throws KmcException {
        rollbackUpdate(FrameType.TC);
    }

    @Test
    public void testRollbackUpdateAos() throws KmcException {
        rollbackUpdate(FrameType.AOS);
    }

    @Test
    public void testRollbackUpdateTm() throws KmcException {
        rollbackUpdate(FrameType.TM);
    }

    @Test
    public void testImportCsvBlankArsnDoesNotViolateNotNullConstraint() throws KmcException {
        String header =
                "spi,scid,vcid,tfvn,mapid,sa_state,st,shivf_len,shsnf_len,shplf_len,stmacf_len,ecs,ekid,iv_len,iv," +
                        "acs,akid,abm_len,abm,arsn_len,arsn,arsnw,type";
        // test blank/'0x' ARSN with arsn_len=0
        String csv = header + "\n"
                + "50,99,0,0,0,3,3,12,0,0,16,0x01,kmc/test/KEY130,12,0x000000000000000000000001,0x00,,19,"
                + "0x00000000000000000000000000000000000000,0,0x,5,tc";

        List<ISecAssn> parsed = new SaCsvInput().parseCsv(new StringReader(csv), FrameType.ALL);
        assertEquals(1, parsed.size());
        ISecAssn sa = parsed.get(0);
        assertNotNull("parsed ARSN must not be null before persisting", sa.getArsn());

        dao.createSa(sa);

        ISecAssn persisted = dao.getSa(new SpiScid(50, (short) 99), FrameType.TC);
        assertNotNull(persisted);
        assertNotNull(persisted.getArsn());
        assertArrayEquals(new byte[]{}, persisted.getArsn());
        assertEquals((short) 0, (short) persisted.getArsnLen());
    }
}