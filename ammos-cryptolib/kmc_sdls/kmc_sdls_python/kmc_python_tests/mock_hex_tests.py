import unittest
from typing import NamedTuple
import sys
from unittest.mock import MagicMock

# Mock the C interface since we cannot build it here
sys.modules['kmc_python_c_sdls_interface'] = MagicMock()

# Import the classes after mocking the C interface
# We need to make sure the path is correct.
# Since we are running this script from the root, we'll use PYTHONPATH
from gov.nasa.jpl.ammos.kmc.sdlsclient.KmcSdlsClient import (
    AOS_FramePrimaryHeader, TM_FramePrimaryHeader, FrameSecurityHeader, FrameSecurityTrailer, frame_global_config
)

class TestHexMethodsGolden(unittest.TestCase):
    def test_aos_header_hex(self):
        scid, vcid, tfvn = 1, 1, 1
        frame_global_config[f"aos.{scid}.{vcid}.{tfvn}"] = {
            "has_fhec": True,
            "has_iz": True
        }
        header = AOS_FramePrimaryHeader(
            tfvn=tfvn, scid=scid, vcid=vcid, vcfc=123456,
            replay=1, vcflag=0, spare=1, vcfcc=2,
            fhec=65535, iz=bytearray([0xAA, 0xBB])
        )
        # Run and print to capture golden value
        print(f"\nGolden AOS: {header.hex()}")

    def test_tm_header_hex(self):
        header = TM_FramePrimaryHeader(
            tfvn=1, scid=1023, vcid=7, ocf=1,
            mcfc=255, vcfc=255, shf=1, sf=0, pof=1, slid=3, fhp=2047
        )
        print(f"Golden TM: {header.hex()}")

    def test_security_header_hex(self):
        header = FrameSecurityHeader(
            sh=0, spi=65535, iv=bytearray([0x11, 0x22]), iv_field_len=2,
            sn=bytearray([0x33, 0x44]), sn_field_len=2,
            pad=bytearray([0x55]), pad_field_len=1
        )
        print(f"Golden Sec Header: {header.hex()}")

    def test_security_trailer_hex(self):
        trailer = FrameSecurityTrailer(
            mac=bytearray([0x66, 0x77]), mac_field_len=2,
            ocf=bytearray([0x88]), ocf_field_len=1,
            fecf=65535
        )
        print(f"Golden Sec Trailer: {trailer.hex()}")

if __name__ == "__main__":
    unittest.main()
