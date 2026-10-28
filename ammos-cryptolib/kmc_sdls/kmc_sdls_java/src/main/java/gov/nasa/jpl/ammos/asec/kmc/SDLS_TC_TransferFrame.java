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
