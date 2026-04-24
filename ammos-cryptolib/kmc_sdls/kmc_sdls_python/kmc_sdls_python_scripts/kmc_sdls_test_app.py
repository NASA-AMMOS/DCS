#!/usr/bin/env python3

# Copyright 2026, by the California Institute of Technology.
# ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
# Any commercial use must be negotiated with the Office of Technology
# Transfer at the California Institute of Technology.
#
# This software may be subject to U.S. export control laws. By accepting
# this software, the user agrees to comply with all applicable U.S.
# export laws and regulations. User has the responsibility to obtain
# export licenses, or other export authority as may be required before
# exporting such information to foreign countries or providing access to
# foreign persons.

import argparse
import os
import binascii
import sys
from abc import abstractmethod, ABC
from dataclasses import dataclass, field

# Import the DCS SDLS Client
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient


class ArgumentException(Exception):
    """Raise when there is a command line argument error"""
    pass


def build_options_parser():
    arg_parser = argparse.ArgumentParser(
        description='Simple DCS SDLS Python Test Application that will Apply and Process Security on a given frame')
    arg_parser.add_argument("-f", "--frame",
                            dest="frame",
                            help="Hex frame string representation of telecommand transfer-frame to apply & process SDLS layering on.")
    arg_parser.add_argument("-p", "--properties",
                            dest="properties",
                            help="The properties file that contains all the configuration needed by this application (supported properties defined in KMC SIS)",
                            default=(os.path.dirname(
                                os.path.realpath(__file__)) + "/../etc/kmc_sdls_test_app.properties"),
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
    "version": "00",  # 2  bit version number
    "sc_id": "00101100",  # 8  bit spacecraft id (44)
    "vc_id": "000000",  # 6  bit virtual channel id
    "vcfc": "000000000000000000000000",  # 24 bit virtual channel frame count
    "replay_flag": "0",  # 1  bit replay flag
    "vcfc_flag": "1",  # 1  bit vcfc usage flag
    "reserved_spare": "00",  # 2  bit reserved spare
    "vcfc_cycle": "0000",  # 4  bit vcfc cycle
}

tm_defaults = {
    "version": "00",  # 2  bit version number
    "sc_id": "0000101100",  # 10 bit spacecraft id (44)
    "vc_id": "000",  # 3  bit virtual channel id
    "ocf_flag": "0",  # 1  bit operational control field flag
    "mcfc": "00000000",  # 8  bit master channel frame count
    "vcfc": "00000000",  # 8  bit virtual channel frame count
    "shf": "0",  # 1  bit secondary header flag
    "synch": "0",  # 1  bit synch flag
    "pof": "0",  # 1  bit packet order flag
    "sl_id": "00",  # 2  bit segment length id
    "fhp": "00000000000"  # 11 bit first header pointer
}


@dataclass
class Frame(ABC):
    version: int = None
    sc_id: int = None
    vc_id: int = None
    hex_value: str = None
    frame_body_hex: str = None
    header_width: int = 10

    @abstractmethod
    def get_packed_header(self) -> int:
        pass

    def to_hex(self):
        if self.hex_value is not None:
            return self.hex_value

        packed = self.get_packed_header()
        header_hex = format(packed, f'0{self.header_width}x')

        body_hex = self.frame_body_hex or ""
        return f"{header_hex}{body_hex}"


@dataclass
class TC(Frame):
    # Default constants
    BYPASS_FLAG = 1
    CTRL_CMD_FLAG = 0
    SPARE = 0
    FRAME_LENGTH = 0b0000001000
    FRAME_SEQUENCE_NUMBER = 0

    def __post_init__(self):
        if self.version is None: self.version = 0
        if self.vc_id is None: self.vc_id = 1
        if self.sc_id is None: self.sc_id = 44
        if self.frame_body_hex is None:
            self.frame_body_hex = "0001bd37"
        self.header_width = 10

    def get_packed_header(self) -> int:
        return (
            (self.version & 0x3) << 38 |
            (self.BYPASS_FLAG & 0x1) << 37 |
            (self.CTRL_CMD_FLAG & 0x1) << 36 |
            (self.SPARE & 0x3) << 34 |
            (self.sc_id & 0x3FF) << 24 |
            (self.vc_id & 0x3F) << 18 |
            (self.FRAME_LENGTH & 0x3FF) << 8 |
            (self.FRAME_SEQUENCE_NUMBER & 0xFF)
        )


@dataclass
class TM(Frame):
    # Default constants
    OCF = 0
    MCFC = 0
    VCFC = 0
    SHF = 0
    SYNC = 0
    POF = 0
    SLID = 0
    FHP = 0

    def __post_init__(self):
        if self.version is None: self.version = 0
        if self.vc_id is None: self.vc_id = 0
        if self.sc_id is None: self.sc_id = 44
        if self.frame_body_hex is None:
            self.frame_body_hex = "0000000000000000000000000000111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111100000000000000000000000000000000"
        self.header_width = 12

    def get_packed_header(self) -> int:
        return (
            (self.version & 0x3) << 46 |
            (self.sc_id & 0x3FF) << 36 |
            (self.vc_id & 0x7) << 33 |
            (self.OCF & 0x1) << 32 |
            (self.MCFC & 0xFF) << 24 |
            (self.VCFC & 0xFF) << 16 |
            (self.SHF & 0x1) << 15 |
            (self.SYNC & 0x1) << 14 |
            (self.POF & 0x1) << 13 |
            (self.SLID & 0x3) << 11 |
            (self.FHP & 0x7FF)
        )


@dataclass
class AOS(Frame):
    # Default constants
    VCFC = 0
    REPLAY_FLAG = 0
    VCFC_FLAG = 0
    SPARE = 0
    VCC_CYCLE = 0

    def __post_init__(self):
        if self.version is None: self.version = 1
        if self.vc_id is None: self.vc_id = 0
        if self.sc_id is None: self.sc_id = 44
        if self.frame_body_hex is None:
            self.frame_body_hex = "0000000000000000000000000000111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111100000000000000000000000000000000"
        self.header_width = 12

    def get_packed_header(self) -> int:
        return (
            (self.version & 0x3) << 46 |
            (self.sc_id & 0xFF) << 38 |
            (self.vc_id & 0x3F) << 32 |
            (self.VCFC & 0xFFFFFF) << 8 |
            (self.REPLAY_FLAG & 0x1) << 7 |
            (self.VCFC_FLAG & 0x1) << 6 |
            (self.SPARE & 0x3) << 4 |
            (self.VCC_CYCLE & 0xF)
        )


def main():
    parser = build_options_parser()
    cli_args = parser.parse_args()

    # Map frame types to their corresponding classes
    FRAME_MAP = {
        "TC": TC,
        "TM": TM,
        "AOS": AOS
    }

    f_type = (cli_args.type or "TC").upper()
    if f_type not in FRAME_MAP:
        raise ArgumentException("Frame type must be TC, TM, or AOS")

    frame = FRAME_MAP[f_type]()

    # Can't have both custom frame and (SC_ID or VC_ID) overrides specified at the same time
    if cli_args.frame and (cli_args.scid or cli_args.vcid):
        raise ArgumentException(
            "Can't have both Custom Frame override and (SC_ID or VC_ID) overrides specified at the same time.")

    # Override the default frame SC ID if specified
    if cli_args.scid:
        frame.sc_id = int(cli_args.scid)

    # Override the default frame VC ID if specified
    if cli_args.vcid:
        frame.vc_id = int(cli_args.vcid)

    # Use the frame override if passed in
    if cli_args.frame:
        frame.hex_value = cli_args.frame

    kmc_sdls_props = list()
    for line in cli_args.properties:
        if (not line.startswith('#') and line.rstrip() != ''):
            kmc_sdls_props.append(line.rstrip())

    # Initialize the KmcSdlsClient object with configuration
    k = KmcSdlsClient.KmcSdlsClient(kmc_sdls_props)

    # Print hex frame to be used:
    print("Using %s transfer frame: \n%s\n" % (f_type, frame.to_hex()))

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
        print("SDLS %s Apply Security Result:\n%s\n" % (f_type, result.hex()))
    else:
        result = tc

    if (not cli_args.apply_only or (cli_args.process_only and cli_args.apply_only)):
        # Process the security headers on the result of the apply operation (or raw frame if processing only)
        fn = k.process_security_tc
        if f_type == "TM":
            fn = k.process_security_tm
        elif f_type == "AOS":
            fn = k.process_security_aos
        reversed_frame = fn(result)

        print("SDLS %s Process Security Result:" % f_type)
        if f_type == 'TC':
            print_tc(reversed_frame)
        elif f_type == 'AOS':
            print_aos(reversed_frame)
        elif f_type == 'TM':
            print_tm(reversed_frame)


def print_tc(frame):
    print("SPI: ", frame.tc_security_header.spi)
    if (len(frame.tc_security_header.iv) != 0):
        print("IV: ", frame.tc_security_header.iv.hex())
    if (len(frame.tc_security_header.sn) != 0):
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
    print("sec trailer: ", frame.aos_security_trailer.hex())
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
    print("sec trailer: ", frame.tm_security_trailer.hex())
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
        sys.exit(1)
    except Exception as e:
        print("Encountered an unexpected error: ", e)
        sys.exit(1)
