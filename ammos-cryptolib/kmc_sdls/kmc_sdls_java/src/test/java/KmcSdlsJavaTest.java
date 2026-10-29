/*
 * Copyright (c) 2014-26 California Institute of Technology (“Caltech”). U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  • Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  • Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *  • Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Open Source License Approved by Caltech/JPL
 * APACHE LICENSE, VERSION 2.0
 *  • Text version: https://www.apache.org/licenses/LICENSE-2.0.txt
 *  • SPDX short identifier: Apache-2.0
 *  • OSI Approved License: https://opensource.org/licenses/Apache-2.0
 */

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
