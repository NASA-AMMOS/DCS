/*
Test KmcSdlsJNI bindings.
 */

import gov.nasa.jpl.ammos.asec.kmc.SDLS_TC_TransferFrame;
import org.junit.Test;
import static org.junit.Assert.*;

import gov.nasa.jpl.ammos.asec.kmc.KmcSdlsEngine;

public class KmcSdlsJavaTest
{
    public KmcSdlsEngine setupKmcSdlsEngineForUnitTestInmemoryLibgcrypt() throws Exception
    {
        KmcSdlsEngine kmcEngine = new KmcSdlsEngine();
        try {
            kmcEngine.configureCryptoLib(KmcSdlsEngine.sadbType.INMEMORY, KmcSdlsEngine.cryptographyType.LIBGCRYPT, true, false, false, false, true, false, true, 0x3F, true);
            kmcEngine.addGvcidManagedParameter(0,0x0003,0,true,true,1024);
            kmcEngine.addGvcidManagedParameter(0,0x0003,1,true,true,1024);
            kmcEngine.init();
        } catch (Exception e) {
            e.printStackTrace();
            kmcEngine.shutdown();
            throw e;
        }
        return kmcEngine;
    }

    @Test
    public void testApplySecurityTcPropertyFileConfig() throws Exception
    {
        String result = "";
        try{
            KmcSdlsEngine kmcEngine = new KmcSdlsEngine("kmc_sdls_java_local_unit_tests.properties");
            result = kmcEngine.applySecurity("20030015000080d2c70008197f0b00310000b1fe3128");
            kmcEngine.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        assertEquals("200300230000000100000000000000000000000080D2C70008197F0B00310000B1FEFE0F",result);
    }

    @Test
    public void testApplySecurityTc() throws Exception
    {
        KmcSdlsEngine kmcEngine = setupKmcSdlsEngineForUnitTestInmemoryLibgcrypt();
        String result = "";
        try{
            result = kmcEngine.applySecurity("20030015000080d2c70008197f0b00310000b1fe3128");
        } catch (Exception e) {
            e.printStackTrace();
            kmcEngine.shutdown();
            throw e;
        }
        kmcEngine.shutdown();
        assertEquals("200300230000000100000000000000000000000080D2C70008197F0B00310000B1FEFE0F",result);
    }

    @Test
    public void testProcessSecurityTc() throws Exception
    {
        KmcSdlsEngine kmcEngine = setupKmcSdlsEngineForUnitTestInmemoryLibgcrypt();
        try{
            SDLS_TC_TransferFrame sdlsTcFrame = kmcEngine.processSecurity("200300230000000100000000000000000000000080D2C70008197F0B00310000B1FEFE0F");
            sdlsTcFrame.printSdlsTransferFrame();
            assertEquals("80D2C70008197F0B00310000B1FE",sdlsTcFrame.tc_pdu);
            assertEquals(14,sdlsTcFrame.tc_pdu_len);
            assertEquals(1,sdlsTcFrame.spi);
            assertEquals(0xFE0F,sdlsTcFrame.fecf);
        } catch (Exception e){
            e.printStackTrace();
            kmcEngine.shutdown();
            throw e;
        }
        kmcEngine.shutdown();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        sb.append("]");
        return sb.toString();
    }
}