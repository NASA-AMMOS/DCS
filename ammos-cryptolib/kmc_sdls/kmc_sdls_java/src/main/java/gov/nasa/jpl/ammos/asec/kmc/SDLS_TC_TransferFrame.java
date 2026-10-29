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

package gov.nasa.jpl.ammos.asec.kmc;

public class SDLS_TC_TransferFrame
{
    //TC_FramePrimaryHeader_t tc_header;
    public int tfvn;   // Transfer Frame Version Number
    public int bypass; // Bypass
    public int cc;     // Control Command
    public int spare;  // Reserved Spare - Shall be 00
    public int scid; // Spacecraft ID
    public int vcid;   // Virtual Channel ID
    public int fl;   // The whole transfer frame length (max 1024)
    public int fsn;    // Frame sequence number, also N(S), zeroed on Type-B frames

    //TC_FrameSecurityHeader_t tc_sec_header;
    public int sh;  // Segment Header
    public int spi;             // Security Parameter Index
    public String iv;      // HexString of Initialization Vector for encryption
    public int iv_field_len;
    public String sn;   // HexString of Sequence Number for anti-replay
    public int sn_field_len;
    public String pad; // HexString of Count of the used fill Bytes
    public int pad_field_len;

    // Processed TC Frame Data
    public String tc_pdu; // Hex String of PDU data
    public int tc_pdu_len;

    //TC_FrameSecurityTrailer_t tc_sec_trailer;
    public String mac; // Hex String of Message Authentication Code
    public int mac_field_len;
    public int fecf;  // Frame Error Control Field


    public SDLS_TC_TransferFrame() { // Blank Init; this is just a struct
    }

    public void printSdlsTransferFrame()
    {
        System.out.println("Primary Header::");
        System.out.println("Transfer Frame Version Number: " + this.tfvn);
        System.out.println("Bypass Flag: " + this.bypass);
        System.out.println("Command Control Flag: " + this.cc);
        System.out.println("Spare Bits: " + this.spare);
        System.out.println("Spacecraft ID: " + this.scid);
        System.out.println("Virtual Channel ID: " + this.vcid);
        System.out.println("Frame Length: " + this.fl);
        System.out.println("Frame Sequence Number: " + this.fsn);
        System.out.println("Segment Header: " + this.sh);
        System.out.println("");
        System.out.println("Security Header::");
        System.out.println("Security Parameter Index: " + this.spi);
        System.out.println("Initialization Vector: " + this.iv);
        System.out.println("Initialization Vector Length: " + this.iv_field_len);
        System.out.println("Sequence Number: " + this.sn);
        System.out.println("Sequence Number Length: " + this.sn_field_len);
        System.out.println("Pad: " + this.pad);
        System.out.println("Pad Length: " + this.pad_field_len);
        System.out.println("");
        System.out.println("PDU::");
        System.out.println("PDU Bytes: " + this.tc_pdu);
        System.out.println("PDU Length: " + this.tc_pdu_len);
        System.out.println("");
        System.out.println("Security Trailer::");
        System.out.println("Message Authentication Code: " + this.mac);
        System.out.println("Message Authentication Code Length: " + this.mac_field_len);
        System.out.println("Frame Error Correction Field: " + this.fecf);
    }
}
