#!/usr/bin/env python3

#Copyright 2021, by the California Institute of Technology.
#ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
#Any commercial use must be negotiated with the Office of Technology
#Transfer at the California Institute of Technology.
#
#This software may be subject to U.S. export control laws. By accepting
#this software, the user agrees to comply with all applicable U.S.
#export laws and regulations. User has the responsibility to obtain
#export licenses, or other export authority as may be required before
#exporting such information to foreign countries or providing access to
#foreign persons.

import argparse
import os
import binascii
from abc import abstractmethod, ABC

#Import the KMC SDLS Client
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient

class ArgumentException(Exception):
    """Raise when there is a command line argument error"""
    pass

def build_options_parser():
    arg_parser=argparse.ArgumentParser(description='Simple KMC SDLS Python Test Application that will Apply and Process Security on a given frame')
    arg_parser.add_argument("-f", "--frame",
                            dest="frame",
                            help="Hex frame string representation of telecommand transfer-frame to apply & process SDLS layering on.")
    arg_parser.add_argument("-p", "--properties", 
                            dest="properties", 
                            help="The properties file that contains all the configuration needed by this application (supported properties defined in KMC SIS)", 
                            default=(os.path.dirname(os.path.realpath(__file__))+"/../etc/kmc_sdls_test_app.properties"), 
                            type=argparse.FileType('r'))
    arg_parser.add_argument("-P", "--processOnly", 
                            dest="process_only", 
                            help="Flag to only process security on the frame (default is to apply & process)", 
                            action='store_true')
    arg_parser.add_argument("-A", "--applyOnly", 
                            dest="apply_only", 
                            help="Flag to only apply security on the frame (default is to apply & process)", 
                            action='store_true')
    arg_parser.add_argument("-s", "--scid", 
                            dest="scid", 
                            type=scid_type, 
                            help="Override the default frame SC ID field")
    arg_parser.add_argument("-V", "--vcid", 
                            dest="vcid", 
                            type=vcid_type, 
                            help="Override the default frame VC ID field")
    arg_parser.add_argument("-t", "--type",
                            dest="type",
                            type=frame_type,
                            help="Frame type, choice between TC (default), TM, and AOS")
    return arg_parser

def scid_type(scid):
    msg = "SC ID must be a number between 0 and 1023 inclusive"
    try:
        int(scid) >= 0 and int(scid) <= 1023
    except:
        raise argparse.ArgumentTypeError(msg)
    return scid

def vcid_type(vcid):
    msg = "VC ID must be a number between 0 and 63 inclusive"    
    try:
        int(vcid) >= 0 and int(vcid) <= 63
    except:
        raise argparse.ArgumentTypeError(msg)
    return vcid

def frame_type(type: str):
    msg = "Frame type must be either 'TC', 'TM', or 'AOS'"
    if type.upper() not in ["TC", "TM", "AOS"]:
        raise argparse.ArgumentTypeError(msg)
    return type

aos_defaults = {
    "version": "00",                    # 2  bit version number
    "sc_id": "00101100",                # 8  bit spacecraft id (44)
    "vc_id": "000000",                  # 6  bit virtual channel id
    "vcfc": "000000000000000000000000", # 24 bit virtual channel frame count
    "replay_flag": "0",                 # 1  bit replay flag
    "vcfc_flag": "1",                   # 1  bit vcfc usage flag
    "reserved_spare": "00",             # 2  bit reserved spare
    "vcfc_cycle": "0000",               # 4  bit vcfc cycle
}

tm_defaults = {
    "version": "00",                    # 2  bit version number
    "sc_id": "0000101100",              # 10 bit spacecraft id (44)
    "vc_id": "000",                     # 3  bit virtual channel id
    "ocf_flag": "0",                    # 1  bit operational control field flag
    "mcfc": "00000000",                 # 8  bit master channel frame count
    "vcfc": "00000000",                 # 8  bit virtual channel frame count
    "shf": "0",                         # 1  bit secondary header flag
    "synch": "0",                       # 1  bit synch flag
    "pof": "0",                         # 1  bit packet order flag
    "sl_id": "00",                      # 2  bit segment length id
    "fhp": "00000000000"                # 11 bit first header pointer
}

class Frame(ABC):
    sc_id = None
    vc_id = None
    hex_value = None
    override = False
    frame_header_hex = None
    frame_body_hex = None
    default_frame_hex = None
    def override_scid(self, scid):
        self.override = True
        self.sc_id = scid

    def override_vcid(self, vcid):
        self.override = True
        self.vc_id = vcid

    def override_hex(self, hex):
        self.override = True
        self.hex_value = hex

    def set_body(self, hex):
        self.frame_body_hex = hex

    @abstractmethod
    def to_hex(self):
        pass


class TC(Frame):
    # Default frame header (202c040800) fields in binary
    version = "00"                     #  2 bit version number
    bypass_flag = "1"                  #  1 bit bypass flag
    ctrl_cmd_flag = "0"                #  1 bit control command flag
    spare = "00"                       #  2 bit spare
    frame_length = "0000001000"        # 10 bit frame length
    frame_sequence_number = "00000000" #  8 bit frame sequence number

    def __init__(self):
        self.vc_id = "000001"                   #  6 bit virtual channel id
        self.sc_id = "0000101100"               # 10 bit spacecraft id (44)
        self.frame_body_hex = "0001bd37"        # default frame body
        self.frame_header_hex = "202c040800"    # Default 5 byte frame header
        self.default_frame_hex = "{}{}".format(self.frame_header_hex, self.frame_body_hex)

    def to_hex(self):
        if self.hex_value is not None:
            return self.hex_value
        elif not self.override:
            return self.default_frame_hex
        else:
            frame_header_bin = "{}{}{}{}{}{}{}{}".format(self.version, self.bypass_flag, self.ctrl_cmd_flag, self.spare, self.sc_id, self.vc_id, self.frame_length, self.frame_sequence_number)
            frame_header_hex = format(int(frame_header_bin, 2), 'x')
            frame_hex = "{}{}".format(frame_header_hex, self.frame_body_hex)
            return frame_hex


class TM(Frame):
    def __init__(self):
        self.vc_id = "000"        #  3 bit virtual channel id
        self.sc_id = "0011111111" # 10 bit spacecraft id (255)
        self.frame_body_hex = "00000000000000000000000000001111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111100000000000000000000000000000000"
        self.frame_header_hex = "4ff000000000"
        self.default_frame_hex = "{}{}".format(self.frame_header_hex, self.frame_body_hex)

    def to_hex(self):
        if self.hex_value is not None:
            return self.hex_value
        elif not self.override:
            return self.default_frame_hex
        else:
            frame_header_bin = ""
            frame_header_hex = format(int(frame_header_bin, 2), 'x')
            frame_hex = "{}{}".format(frame_header_hex, self.frame_body_hex)
            return frame_hex


class AOS(Frame):
    version = "01"
    vcfc = "000000"
    replay_flag = "0"
    vcfc_flag = "0"
    spare = "00"
    vcc_cycle = "0000"

    def __init__(self):
        self.vc_id = "000000"   #  6 bit virtual channel id
        self.sc_id = "111111111" # 8 bit spacecraft id (255)
        self.frame_body_hex = "00000000000000000000000000001111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111100000000000000000000000000000000"
        self.frame_header_hex = "7fc000000000"
        self.default_frame_hex = "{}{}".format(self.frame_header_hex, self.frame_body_hex)

    def to_hex(self):
        if self.hex_value is not None:
            return self.hex_value
        elif not self.override:
            return self.default_frame_hex
        else:
            frame_header_bin = ""
            frame_header_hex = format(int(frame_header_bin, 2), 'x')
            frame_hex = "{}{}".format(frame_header_hex, self.frame_body_hex)
            return frame_hex


def main():
    parser=build_options_parser()
    cli_args=parser.parse_args()

    if cli_args.type is not None:
        f_type = cli_args.type.upper()
        if f_type == "TC":
            frame = TC()
        elif f_type == "TM":
            frame = TM()
        elif f_type == "AOS":
            frame = AOS()
        else:
            raise ArgumentException("Frame type must be TC, TM, or AOS")
    else:
        f_type = "TC"
        frame = TC()

    # Can't have both custom frame and (SC_ID or VC_ID) overrides specified at the same time
    if cli_args.frame and (cli_args.scid or cli_args.vcid):
        raise ArgumentException("Can't have both Custom Frame override and (SC_ID or VC_ID) overrides specified at the same time.")

    # Override the default frame SC ID if specified
    if cli_args.scid:
        fmt = "{0:010b}"
        if f_type == "TM":
            fmt = '{0:10b}'
        elif f_type == "AOS":
            fmt = '{0:08b}'

        sc_id = fmt.format(int(cli_args.scid))
        frame.override_scid(sc_id)

    # Override the default frame VC ID if specified
    if cli_args.vcid:
        fmt = '{0:06b}'
        if f_type == "TM":
            fmt = '{0:03b}'
        elif f_type == "AOS":
            fmt = '{0:06b}'
        vc_id = fmt.format(int(cli_args.vcid))
        frame.override_vcid(vc_id)

    # Use the frame override if passed in
    if cli_args.frame:
        frame_hex = cli_args.frame
        frame.override_hex(frame_hex)

    kmc_sdls_props = list()
    for line in cli_args.properties:
        if(not line.startswith('#') and line.rstrip() != ''):
            kmc_sdls_props.append(line.rstrip())

    # Initialize the KmcSdlsClient object with configuration
    k = KmcSdlsClient.KmcSdlsClient(kmc_sdls_props)

    # Print hex frame to be used:
    print("Using telecommand transfer frame: \n%s\n" % frame.to_hex())

    # Convert a hex-string representation of a JPL frame into a python bytearray
    tc = bytearray(binascii.unhexlify(frame.to_hex()))

    if not cli_args.process_only or (cli_args.process_only and cli_args.apply_only):
        # Apply security to the telecommand transfer frame, store the result
        fn = k.apply_security_tc
        if f_type == "TM":
            fn = k.apply_security_tm
        elif f_type == "AOS":
            fn = k.apply_security_aos
        result = fn(tc)
        print("SDLS TC Apply Security Result:\n%s\n"%result.hex())
    else:
        result = tc

    if(not cli_args.apply_only or (cli_args.process_only and cli_args.apply_only)):
        # Process the security headers on the result of the apply operation (or raw frame if processing only)
        fn = k.process_security_tc
        if f_type == "TM":
            fn = k.process_security_tm
        elif f_type == "AOS":
            fn = k.process_security_aos
        reversed_frame = fn(result)

        print("SDLS "+f_type+" Process Security Result:")
        if f_type == 'TC':
            print_tc(reversed_frame)
        elif f_type == 'AOS':
            print_aos(reversed_frame)
        elif f_type == 'TM':
            print_tm(reversed_frame)


def print_tc(frame):
    print("SPI: ", frame.tc_security_header.spi)
    if(len(frame.tc_security_header.iv) != 0):
        print("IV: ", frame.tc_security_header.iv.hex())
    if(len(frame.tc_security_header.sn) != 0):
        print("SN: ", frame.tc_security_header.sn.hex())
    print("PDU: ", frame.tc_pdu.hex())
    print("MAC: ", frame.tc_security_trailer.mac.hex())
    print("FECF: ", hex(frame.tc_security_trailer.fecf))


def print_aos(frame):
    print("header: ", frame.aos_header.hex())
    print("header len: ", len(frame.aos_header.hex()) / 2)
    print("sec header: ", frame.aos_security_header.hex())
    print("sec header len: ", len(frame.aos_security_header.hex()) / 2)
    print("PDU: ", frame.aos_pdu.hex())
    print("PDU len: ", len(frame.aos_pdu.hex()) / 2)
    print("sec trailer: " , frame.aos_security_trailer.hex())
    print("sec trailer len: ", len(frame.aos_security_trailer.hex()) / 2)
    print("MAC: ", frame.aos_security_trailer.mac.hex())
    print("FECF: ", hex(frame.aos_security_trailer.fecf))
    print("frame: ", frame.hex())
    print("frame len: ", len(frame.hex()) / 2)

def print_tm(frame):
    print("header: ", frame.tm_header.hex())
    print("header len: ", len(frame.tm_header.hex()) / 2)
    print("sec header: ", frame.tm_security_header.hex())
    print("sec header len: ", len(frame.tm_security_header.hex()) / 2)
    print("PDU: ", frame.tm_pdu.hex())
    print("PDU len: ", len(frame.tm_pdu.hex()) / 2)
    print("sec trailer: " , frame.tm_security_trailer.hex())
    print("sec trailer len: ", len(frame.tm_security_trailer.hex()) / 2)
    print("MAC: ", frame.tm_security_trailer.mac.hex())
    print("FECF: ", hex(frame.tm_security_trailer.fecf))
    print("frame: ", frame.hex())
    print("frame len: ", len(frame.hex()) / 2)

if __name__ == "__main__":
    try:
        main()
    except ArgumentException as ae:
        print("Command Line Argument Error: ", ae)            
    except Exception as e:
        print("Encountered an unexpected error: ", e)
    finally:
        os._exit(1)
