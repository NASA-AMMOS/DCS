import binascii
import ctypes
import ctypes.util
import sys
import unittest

import kmc_python_c_sdls_interface
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient

# Matches Crypto_Init_TC_Unit_Test (SCID 3, VCID 0, FECF + segment header) so the default in-memory SAs apply
TC_CONFIG = ['cryptolib.sadb.type=inmemory', 'cryptolib.crypto.type=libgcrypt',
             'cryptolib.tc.ignore_antireplay=true', 'cryptolib.tc.ignore_sa_state=true',
             'cryptolib.tc.create_ecf=true', 'cryptolib.tc.vcid_bitmask=0x3F',
             'cryptolib.tc.3.0.0.has_ecf=true', 'cryptolib.tc.3.0.0.has_segmentation_header=true',
             'cryptolib.tc.3.0.0.max_frame_length=1024']
# Clear-mode SDLS ping from CryptoLib's TC_APPLY_SECURITY.HAPPY_PATH_CLEAR unit test
TC_PING = "20030015000080d2c70008197f0b00310000b1fe3128"

# SCID 3, VCID 0, SPI 1: a default in-memory SA that processes clear TM frames without further SA setup
TM_MAX_FRAME_LENGTH = 1786
TM_CONFIG = ['cryptolib.sadb.type=inmemory', 'cryptolib.crypto.type=libgcrypt',
             'cryptolib.tm.ignore_antireplay=true', 'cryptolib.tm.check_fecf=false',
             'cryptolib.tm.vcid_bitmask=0x3F', 'cryptolib.tm.3.0.0.has_ecf=true',
             'cryptolib.tm.3.0.0.max_frame_length=%d' % TM_MAX_FRAME_LENGTH]
TM_HEADER_AND_SPI = bytes.fromhex("0030000018000001")

_libc = ctypes.CDLL(ctypes.util.find_library("c"))


class _MallInfo2(ctypes.Structure):
    _fields_ = [(name, ctypes.c_size_t) for name in
                ("arena", "ordblks", "smblks", "hblks", "hblkhd", "usmblks", "fsmblks", "uordblks", "fordblks",
                 "keepcost")]


if sys.platform.startswith("linux"):
    _libc.mallinfo2.restype = _MallInfo2


class _MallocStatistics(ctypes.Structure):  # macOS malloc_statistics_t
    _fields_ = [("blocks_in_use", ctypes.c_uint), ("size_in_use", ctypes.c_size_t),
                ("max_size_in_use", ctypes.c_size_t), ("size_allocated", ctypes.c_size_t)]


def _heap_in_use():
    """Bytes currently allocated from the C heap (glibc mallinfo2 on Linux, malloc zone statistics on macOS)."""
    if sys.platform.startswith("linux"):
        return _libc.mallinfo2().uordblks
    stats = _MallocStatistics()
    _libc.malloc_zone_statistics(None, ctypes.byref(stats))
    return stats.size_in_use


class _KmcCryptoServiceConfig(ctypes.Structure):
    _fields_ = [("kmc_crypto_hostname", ctypes.c_char_p), ("protocol", ctypes.c_char_p),
                ("kmc_crypto_port", ctypes.c_uint16), ("kmc_crypto_app_uri", ctypes.c_char_p),
                ("mtls_client_cert_path", ctypes.c_char_p), ("mtls_client_cert_type", ctypes.c_char_p),
                ("mtls_client_key_path", ctypes.c_char_p), ("mtls_client_key_pass", ctypes.c_char_p),
                ("mtls_ca_bundle", ctypes.c_char_p), ("mtls_ca_path", ctypes.c_char_p),
                ("mtls_issuer_cert", ctypes.c_char_p), ("ignore_ssl_hostname_validation", ctypes.c_uint8)]


def _error_name(code):
    lib, ffi = kmc_python_c_sdls_interface.lib, kmc_python_c_sdls_interface.ffi
    return ffi.string(lib.sdls_get_error_code_enum_string(code)).decode()


def _crypto_service_config():
    # The variable lives in libcryptolib, which the cffi extension has already loaded. macOS exposes it through the
    # global namespace; on Linux the library is found by name (same already-loaded image). Never dlopen another copy
    # of the library by path: it would hold its own, uninitialised variable.
    name = "libcryptolib.dylib" if sys.platform == "darwin" else "libcryptolib.so"
    for handle in (lambda: ctypes.CDLL(None), lambda: ctypes.CDLL(name)):
        try:
            ptr = ctypes.POINTER(_KmcCryptoServiceConfig).in_dll(handle(), "cryptography_kmc_crypto_config")
            if ptr:
                return ptr.contents
        except (OSError, ValueError):
            continue
    raise RuntimeError("cryptography_kmc_crypto_config not found (is libcryptolib loaded and initialised?)")


class NativeRegressionTest(unittest.TestCase):
    def setUp(self):
        self.client = None

    def tearDown(self):
        if self.client is not None:
            self.client.shutdown()
        KmcSdlsClient.frame_global_config.clear()

    def _client(self, config):
        self.client = KmcSdlsClient.KmcSdlsClient(config)
        return self.client

    def _applied_ping(self, client, cam_cookies=None):
        return client.apply_security_tc(bytearray(binascii.unhexlify(TC_PING)), cam_cookies)

    def test_tc_check_fecf_true_rejects_bad_fecf(self):
        client = self._client(TC_CONFIG + ['cryptolib.tc.check_fecf=true'])
        frame = self._applied_ping(client)
        client.process_security_tc(bytearray(frame))
        frame[-1] ^= 0xFF
        with self.assertRaises(KmcSdlsClient.SdlsClientException) as ctx:
            client.process_security_tc(frame)
        self.assertEqual(_error_name(ctx.exception.get_error_code()), "CRYPTO_LIB_ERR_INVALID_FECF")

    def test_tc_check_fecf_false_ignores_bad_fecf(self):
        client = self._client(TC_CONFIG + ['cryptolib.tc.check_fecf=false'])
        frame = self._applied_ping(client)
        frame[-1] ^= 0xFF
        client.process_security_tc(frame)

    def test_apply_security_tc_frees_output_frame(self):
        client = self._client(TC_CONFIG)
        for _ in range(100):
            self._applied_ping(client)
        before = _heap_in_use()
        iterations = 20000
        for _ in range(iterations):
            self._applied_ping(client)
        growth = _heap_in_use() - before
        # A leaked output frame is >= len(TC_PING) bytes per call (~440 KB here)
        self.assertLess(growth, iterations * 4)

    def test_tc_apply_and_process_accept_cam_cookies(self):
        client = self._client(TC_CONFIG)
        frame = self._applied_ping(client, cam_cookies="cookie=value")
        client.process_security_tc(frame, cam_cookies="cookie=value")

    def test_verifyserver(self):
        for config, expected_ignore in (([], 0), (['cryptolib.crypto.kmccryptoservice.verifyserver=true'], 0),
                                        (['cryptolib.crypto.kmccryptoservice.verifyserver=false'], 1)):
            with self.subTest(config=config):
                client = self._client(TC_CONFIG + config)
                self.assertEqual(_crypto_service_config().ignore_ssl_hostname_validation, expected_ignore)
                client.shutdown()
                self.client = None

    def test_tm_full_length_frame_processes(self):
        client = self._client(TM_CONFIG)
        frame = TM_HEADER_AND_SPI + b"\xAB" * (TM_MAX_FRAME_LENGTH - len(TM_HEADER_AND_SPI))
        client.process_security_tm(bytearray(frame))

    def test_tm_short_frame_rejected(self):
        client = self._client(TM_CONFIG)
        frame = TM_HEADER_AND_SPI + b"\xAB" * (TM_MAX_FRAME_LENGTH - len(TM_HEADER_AND_SPI) - 10)
        with self.assertRaises(KmcSdlsClient.SdlsClientException) as ctx:
            client.process_security_tm(bytearray(frame))
        self.assertEqual(_error_name(ctx.exception.get_error_code()), "CRYPTO_LIB_ERR_TM_FRAME_LENGTH_UNDERFLOW")


if __name__ == '__main__':
    unittest.main()
