import os
import re
import sys
import unittest
from unittest.mock import MagicMock

# Mock the C interface so these tests run without the native build
sys.modules['kmc_python_c_sdls_interface'] = MagicMock()

from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient as client_module

CFFI_DEFS = os.path.join(os.path.dirname(__file__), '..', '_cffi_src', 'cffi_definitions.i')


def _load_enums():
    enums = {}
    with open(CFFI_DEFS) as f:
        text = f.read()
    for body in re.findall(r'typedef enum\s*\{(.*?)\}', text, re.S):
        value = -1
        for entry in body.split(','):
            entry = entry.strip()
            if not entry:
                continue
            name, _, explicit = entry.partition('=')
            value = int(explicit) if explicit.strip() else value + 1
            enums[name.strip()] = value
    return enums


ENUMS = _load_enums()
BASE_CONFIG = ["cryptolib.sadb.type=inmemory", "cryptolib.crypto.type=libgcrypt"]


class TestConfigMapping(unittest.TestCase):
    def setUp(self):
        client_module.frame_global_config.clear()

    def _init(self, *config):
        iface = MagicMock()
        for name, value in ENUMS.items():
            setattr(iface.lib, name, value)
        iface.lib.sdls_init.return_value = 0
        client_module.kmc_python_c_sdls_interface = iface
        client = client_module.KmcSdlsClient.__new__(client_module.KmcSdlsClient)
        client.cast = lambda _type, arg: arg
        client_module.KmcSdlsClient.__init__(client, BASE_CONFIG + list(config))
        return iface.lib

    def test_tc_check_fecf_uses_tc_enum(self):
        lib = self._init("cryptolib.tc.check_fecf=true")
        self.assertEqual(lib.sdls_config_cryptolib_tc.call_args.args[6], ENUMS["TC_CHECK_FECF_TRUE"])
        lib = self._init("cryptolib.tc.check_fecf=false")
        self.assertEqual(lib.sdls_config_cryptolib_tc.call_args.args[6], ENUMS["TC_CHECK_FECF_FALSE"])

    def test_verifyserver_maps_to_inverse_of_ignore_ssl_hostname_validation(self):
        for config, expected_ignore in (((), 0), (("cryptolib.crypto.kmccryptoservice.verifyserver=true",), 0),
                                        (("cryptolib.crypto.kmccryptoservice.verifyserver=false",), 1)):
            with self.subTest(config=config):
                lib = self._init(*config)
                self.assertEqual(int(lib.sdls_config_kmc_crypto_service.call_args.args[6]), expected_ignore)

    def test_tfvn_passed_as_int(self):
        lib = self._init("cryptolib.tc.44.1.0.has_ecf=true",
                         "cryptolib.tm.44.1.0.has_ecf=true", "cryptolib.tm.44.1.0.max_frame_length=1786",
                         "cryptolib.aos.44.1.1.has_ecf=true", "cryptolib.aos.44.1.1.max_frame_length=1786")
        for fn, expected in ((lib.sdls_config_add_gvcid_managed_parameter_tc, 0),
                             (lib.sdls_config_add_gvcid_managed_parameter_tm, 0),
                             (lib.sdls_config_add_gvcid_managed_parameter_aos, 1)):
            tfvn = fn.call_args.args[0]
            self.assertIs(type(tfvn), int)
            self.assertEqual(tfvn, expected)

    def test_max_frame_length_required_for_tm_and_aos(self):
        for frame_type in ("tm", "aos"):
            with self.subTest(frame_type=frame_type):
                with self.assertRaises(client_module.SdlsClientException) as ctx:
                    self._init("cryptolib.%s.44.1.0.has_ecf=true" % frame_type)
                self.assertEqual(ctx.exception.get_error_code(), client_module.SdlsClientException.MISSING_CONFIGURATION_PARAMETER)

    def test_max_frame_length_defaults_for_tc(self):
        lib = self._init("cryptolib.tc.44.1.0.has_ecf=true")
        self.assertEqual(lib.sdls_config_add_gvcid_managed_parameter_tc.call_args.args[5], 1024)

    def test_tc_keys_override_deprecated_process_tc_keys(self):
        lib = self._init("cryptolib.process_tc.ignore_sa_state=true", "cryptolib.tc.ignore_sa_state=false",
                         "cryptolib.process_tc.process_pdus=false", "cryptolib.tc.process_pdus=true")
        args = lib.sdls_config_cryptolib_tc.call_args.args
        self.assertEqual(args[4], 0)
        self.assertEqual(args[1], 1)

    def test_deprecated_process_tc_keys_still_honored(self):
        lib = self._init("cryptolib.process_tc.ignore_sa_state=false", "cryptolib.process_tc.process_pdus=true")
        args = lib.sdls_config_cryptolib_tc.call_args.args
        self.assertEqual(args[4], 0)
        self.assertEqual(args[1], 1)


if __name__ == "__main__":
    unittest.main()
