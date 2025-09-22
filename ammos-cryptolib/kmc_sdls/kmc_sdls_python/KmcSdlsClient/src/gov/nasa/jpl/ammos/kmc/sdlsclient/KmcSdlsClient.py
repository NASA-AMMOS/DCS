#
# Copyright 2021, by the California Institute of Technology.
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
#


import distutils.util
import os.path
import re
from typing import NamedTuple

import kmc_python_c_sdls_interface

SUCCESS = 0

"""
This module defines a pythonic library for interfacing with the kmc_python_c_sdls_interface

"""


class KmcSdlsClient:
    ffi = None
    global_dict = dict()

    def __init__(self, config):
        '''
        Default KmcSdlsClient Constructor

        Parameters
        ----------
        config : list
            A list of properties that configure the CryptoLib interface.
            See the KMC SIS for what the supported properties are.

        '''
        self.ffi = kmc_python_c_sdls_interface.ffi

        config_dict = dict(config_str.split('=', 1) for config_str in config)

        home = os.path.expanduser('~')

        sadb_type_map = {"uninitialized": 0, "custom": 1, "inmemory": 2, "mariadb": 3}
        crypto_type_map = {"uninitialized": 0, "libgcrypt": 1, "kmccryptoservice": 2, "wolfssl": 3}
        cam_login_method = {"none": 0, "kerberos": 1, "keytab_file": 2}

        # Configure CryptoLib
        cryptolib_sadb_type = sadb_type_map.get(config_dict.get("cryptolib.sadb.type", "mariadb"), 3)
        cryptolib_crypto_type = crypto_type_map.get(config_dict.get("cryptolib.crypto.type", "kmccryptoservice"), 2)

        cryptolib_process_tc_ignore_antireplay = distutils.util.strtobool(
            config_dict.get("cryptolib.process_tc.ignore_antireplay", "true"))
        cryptolib_process_tc_ignore_sa_state = distutils.util.strtobool(
            config_dict.get("cryptolib.process_tc.ignore_sa_state", "true"))
        cryptolib_process_tc_process_pdus = distutils.util.strtobool(
            config_dict.get("cryptolib.process_tc.process_pdus", "false"))
        if "cryptolib.apply_tm.create_ecf" in config_dict:
            cryptolib_apply_create_ecf = distutils.util.strtobool(
                config_dict.get("cryptolib.apply_tm.create_ecf", "false")) + 2
        elif "cryptolib.apply_aos.create_ecf" in config_dict:
            cryptolib_apply_create_ecf = distutils.util.strtobool(
                config_dict.get("cryptolib.apply_aos.create_ecf", "false")) + 4
        else:
            cryptolib_apply_create_ecf = distutils.util.strtobool(
                config_dict.get("cryptolib.apply_tc.create_ecf", "false"))

        cryptolib_tc_has_pus_header = distutils.util.strtobool(config_dict.get("cryptolib.tc.has_pus_header", "false"))
        cryptolib_tc_unique_sa_per_mapid = distutils.util.strtobool(
            config_dict.get("cryptolib.tc.unique_sa_per_mapid", "false"))
        if "cryptolib.process_tm.check_fecf" in config_dict:
            cryptolib_check_fecf = distutils.util.strtobool(
                config_dict.get("cryptolib.process_tm.check_fecf", "false")) + 2
        elif "cryptolib.process_aos.check_fecf" in config_dict:
            cryptolib_check_fecf = distutils.util.strtobool(
                config_dict.get("cryptolib.process_aos.check_fecf", "false")) + 4
        else:
            cryptolib_check_fecf = distutils.util.strtobool(config_dict.get("cryptolib.process_tc.check_fecf", "false"))

        if "cryptolib.tm.vcid_bitmask" in config_dict:
            cryptolib_vcid_bitmask = int(config_dict.get("cryptolib.tm.vcid_bitmask", "0x3F"), 16)
        elif "cryptolib.aos.vcid_bitmask" in config_dict:
            cryptolib_vcid_bitmask = int(config_dict.get("cryptolib.aos.vcid_bitmask", "0x3F"), 16)
        else:
            cryptolib_vcid_bitmask = int(config_dict.get("cryptolib.tc.vcid_bitmask", "0x3F"), 16)

        cryptolib_tc_on_rollover_increment_nontransmitted_counter = distutils.util.strtobool(
            config_dict.get("cryptolib.tc.on_rollover_increment_nontransmitted_counter", "true"))
        kmc_python_c_sdls_interface.lib.sdls_config_cryptolib(self.ffi.cast("uint8_t", cryptolib_sadb_type)
                                                              , self.ffi.cast("uint8_t", cryptolib_crypto_type)
                                                              , self.ffi.cast("uint8_t", cryptolib_apply_create_ecf)
                                                              , self.ffi.cast("uint8_t",
                                                                              cryptolib_process_tc_process_pdus)
                                                              , self.ffi.cast("uint8_t", cryptolib_tc_has_pus_header)
                                                              , self.ffi.cast("uint8_t",
                                                                              cryptolib_process_tc_ignore_sa_state)
                                                              , self.ffi.cast("uint8_t",
                                                                              cryptolib_process_tc_ignore_antireplay)
                                                              ,
                                                              self.ffi.cast("uint8_t", cryptolib_tc_unique_sa_per_mapid)
                                                              , self.ffi.cast("uint8_t", cryptolib_check_fecf)
                                                              , self.ffi.cast("uint8_t", cryptolib_vcid_bitmask)
                                                              , self.ffi.cast("uint8_t",
                                                                              cryptolib_tc_on_rollover_increment_nontransmitted_counter)
                                                              )

        # MariaDB Property Keys
        mariadb_tls_cacert_property_key = "cryptolib.sadb.mariadb.tls.cacert"
        mariadb_mtls_clientcert_property_key = "cryptolib.sadb.mariadb.mtls.clientcert"
        mariadb_mtls_clientkey_property_key = "cryptolib.sadb.mariadb.mtls.clientkey"
        # Configure MySQL
        sadb_mariadb_fqdn = config_dict.get("cryptolib.sadb.mariadb.fqdn", "localhost")
        sadb_mariadb_port = int(config_dict.get("cryptolib.sadb.mariadb.port", 3306))
        sadb_mariadb_database_name = config_dict.get("cryptolib.sadb.mariadb.database_name", "sadb")
        sadb_mariadb_username = config_dict.get("cryptolib.sadb.mariadb.username", "sadb_user")
        sadb_mariadb_password = config_dict.get("cryptolib.sadb.mariadb.password", "")
        sadb_mariadb_cacert = config_dict.get(mariadb_tls_cacert_property_key,
                                              "")  # Start empty, defaults to /etc/pki/tls/certs/ammos-ca-bundle.crt below if mtls_client_certs are set
        sadb_mariadb_capath = config_dict.get("cryptolib.sadb.mariadb.tls.capath", "")
        sadb_mariadb_tls_verifyserver = distutils.util.strtobool(
            config_dict.get("cryptolib.sadb.mariadb.tls.verifyserver",
                            "false"))  # Must be false by default,defaults to true below if mTLS/TLS is used!
        sadb_mariadb_clientcert = config_dict.get(mariadb_mtls_clientcert_property_key, "")
        sadb_mariadb_clientkey = config_dict.get(mariadb_mtls_clientkey_property_key, "")
        sadb_mariadb_clientkeypassword = config_dict.get("cryptolib.sadb.mariadb.mtls.clientkeypassword", "")
        sadb_mariadb_require_secure_transport = distutils.util.strtobool(
            config_dict.get("cryptolib.sadb.mariadb.require_secure_transport",
                            "false"))  # Must be false by default, otherwise tls connections are attempted, even if not configured for them.

        # Create char[] for config strings
        sadb_mariadb_username_ffi = self._ffi_null_or_char(sadb_mariadb_username)
        sadb_mariadb_password_ffi = self._ffi_null_or_char(sadb_mariadb_password)
        sadb_mariadb_fqdn_ffi = self._ffi_null_or_char(sadb_mariadb_fqdn)
        sadb_mariadb_database_name_ffi = self._ffi_null_or_char(sadb_mariadb_database_name)
        sadb_mariadb_clientcert_ffi = self._ffi_null_or_char(sadb_mariadb_clientcert)
        sadb_mariadb_clientkey_ffi = self._ffi_null_or_char(sadb_mariadb_clientkey)
        sadb_mariadb_cacert_ffi = self._ffi_null_or_char(sadb_mariadb_cacert)
        sadb_mariadb_capath_ffi = self._ffi_null_or_char(sadb_mariadb_capath)
        sadb_mariadb_clientkeypassword_ffi = self._ffi_null_or_char(sadb_mariadb_clientkeypassword)

        # Verify mtls cert files are present if specified!
        if (sadb_mariadb_clientcert_ffi != self.ffi.NULL or sadb_mariadb_clientkey_ffi != self.ffi.NULL):
            sadb_mariadb_cacert = config_dict.get(mariadb_tls_cacert_property_key,
                                                  "/etc/pki/tls/certs/ammos-ca-bundle.crt")
            sadb_mariadb_clientcert_ffi = self._ffi_null_or_char(sadb_mariadb_clientcert)
            if (sadb_mariadb_clientcert_ffi == self.ffi.NULL):
                raise SdlsClientException(SdlsClientException.MISSING_CONFIGURATION_PARAMETER, (
                        "Configuration Parameter is necessary for SADB mTLS connection: %s" % mariadb_mtls_clientcert_property_key))
            if (sadb_mariadb_clientkey_ffi == self.ffi.NULL):
                raise SdlsClientException(SdlsClientException.MISSING_CONFIGURATION_PARAMETER, (
                        "Configuration Parameter is necessary for SADB mTLS connection: %s" % mariadb_mtls_clientkey_property_key))
            self._file_exists_or_exception(sadb_mariadb_clientcert, mariadb_mtls_clientcert_property_key)
            self._file_exists_or_exception(sadb_mariadb_clientkey, mariadb_mtls_clientkey_property_key)
            self._file_exists_or_exception(sadb_mariadb_cacert, mariadb_tls_cacert_property_key)

        # Verify tls cert files are present
        if (sadb_mariadb_clientcert_ffi != self.ffi.NULL):
            sadb_mariadb_tls_verifyserver = distutils.util.strtobool(
                config_dict.get("cryptolib.sadb.mariadb.tls.verifyserver", "true"))  # default to true if TLS
            sadb_mariadb_require_secure_transport = distutils.util.strtobool(
                config_dict.get("cryptolib.sadb.mariadb.require_secure_transport", "true"))  # default to true if TLS

        kmc_python_c_sdls_interface.lib.sdls_config_mariadb(sadb_mariadb_fqdn_ffi
                                                            , sadb_mariadb_database_name_ffi
                                                            , self.ffi.cast("uint16_t", sadb_mariadb_port)
                                                            , sadb_mariadb_require_secure_transport
                                                            , sadb_mariadb_tls_verifyserver
                                                            , sadb_mariadb_cacert_ffi
                                                            , sadb_mariadb_capath_ffi
                                                            , sadb_mariadb_clientcert_ffi
                                                            , sadb_mariadb_clientkey_ffi
                                                            , sadb_mariadb_clientkeypassword_ffi
                                                            , sadb_mariadb_username_ffi
                                                            , sadb_mariadb_password_ffi
                                                            )

        # KMC Crypto Service Property Keys
        kmc_crypto_mtls_client_cert_property_key = "cryptolib.crypto.kmccryptoservice.mtls.clientcert"
        kmc_crypto_mtls_client_key_property_key = "cryptolib.crypto.kmccryptoservice.mtls.clientkey"
        kmc_crypto_mtls_cacert_property_key = "cryptolib.crypto.kmccryptoservice.cacert"

        # Configure KMC Crypto Service
        kmc_crypto_protocol = config_dict.get("cryptolib.crypto.kmccryptoservice.protocol", "https")
        kmc_crypto_hostname = config_dict.get("cryptolib.crypto.kmccryptoservice.fqdn", "localhost")
        kmc_crypto_port = int(config_dict.get("cryptolib.crypto.kmccryptoservice.port", 8443))
        kmc_crypto_app_uri = config_dict.get("cryptolib.crypto.kmccryptoservice.app", "crypto-service")
        kmc_crypto_mtls_client_cert = config_dict.get(kmc_crypto_mtls_client_cert_property_key, "")
        kmc_crypto_mtls_client_cert_format = config_dict.get("cryptolib.crypto.kmccryptoservice.mtls.clientcertformat",
                                                             "PEM")
        kmc_crypto_mtls_client_key = config_dict.get(kmc_crypto_mtls_client_key_property_key, "")
        kmc_crypto_mtls_client_key_pass = config_dict.get("cryptolib.crypto.kmccryptoservice.mtls.clientkeypassword",
                                                          "")
        kmc_crypto_mtls_ca_bundle = config_dict.get(kmc_crypto_mtls_cacert_property_key,
                                                    "")  # Start empty, defaults to /etc/pki/tls/certs/ammos-ca-bundle.crt below if KMC Crypto Service used (mTLS is only supported connection type right now)
        kmc_crypto_mtls_ca_path = config_dict.get("cryptolib.crypto.kmccryptoservice.cacertpath", "")
        kmc_crypto_mtls_issuer_cert = config_dict.get("cryptolib.crypto.kmccryptoservice.issuercert", "")
        kmc_crypto_mtls_ignore_ssl_hostname_validation = distutils.util.strtobool(
            config_dict.get("cryptolib.crypto.kmccryptoservice.verifyserver",
                            "true"))  # only mtls crypto service connections are supported, defualt to true

        # Create char[] for config strings
        kmc_crypto_protocol_ffi = self._ffi_null_or_char(kmc_crypto_protocol)
        kmc_crypto_hostname_ffi = self._ffi_null_or_char(kmc_crypto_hostname)
        kmc_crypto_app_uri_ffi = self._ffi_null_or_char(kmc_crypto_app_uri)
        kmc_crypto_mtls_client_cert_ffi = self._ffi_null_or_char(kmc_crypto_mtls_client_cert)
        kmc_crypto_mtls_client_cert_format_ffi = self._ffi_null_or_char(kmc_crypto_mtls_client_cert_format)
        kmc_crypto_mtls_client_key_ffi = self._ffi_null_or_char(kmc_crypto_mtls_client_key)
        kmc_crypto_mtls_client_key_pass_ffi = self._ffi_null_or_char(kmc_crypto_mtls_client_key_pass)
        kmc_crypto_mtls_ca_bundle_ffi = self._ffi_null_or_char(kmc_crypto_mtls_ca_bundle)
        kmc_crypto_mtls_ca_path_ffi = self._ffi_null_or_char(kmc_crypto_mtls_ca_path)
        kmc_crypto_mtls_issuer_cert_ffi = self._ffi_null_or_char(kmc_crypto_mtls_issuer_cert)

        # Add some cert file existence sanity checks
        if (
                cryptolib_crypto_type == 2):  # KMC Crypto Service -- verify mTLS certs! (mtls is only supported KMC Crypto Connection type as of now)
            kmc_crypto_mtls_ca_bundle = config_dict.get(kmc_crypto_mtls_cacert_property_key,
                                                        "/etc/pki/tls/certs/ammos-ca-bundle.crt")
            kmc_crypto_mtls_ca_bundle_ffi = self._ffi_null_or_char(kmc_crypto_mtls_ca_bundle)
            if (kmc_crypto_mtls_client_cert_ffi == self.ffi.NULL):
                raise SdlsClientException(SdlsClientException.MISSING_CONFIGURATION_PARAMETER, (
                        "Configuration Parameter is necessary for KMC Crypto Service mTLS connection: %s" % kmc_crypto_mtls_client_cert_property_key))
            if (kmc_crypto_mtls_client_key_ffi == self.ffi.NULL):
                raise SdlsClientException(SdlsClientException.MISSING_CONFIGURATION_PARAMETER, (
                        "Configuration Parameter is necessary for KMC Crypto Service mTLS connection: %s" % kmc_crypto_mtls_client_key_property_key))
            self._file_exists_or_exception(kmc_crypto_mtls_client_cert, kmc_crypto_mtls_client_cert_property_key)
            self._file_exists_or_exception(kmc_crypto_mtls_client_key, kmc_crypto_mtls_client_key_property_key)
            self._file_exists_or_exception(kmc_crypto_mtls_ca_bundle, kmc_crypto_mtls_cacert_property_key)

        # Add strong references to CFFI string objects so they don't get freed.
        self.global_dict["sadb_mariadb_username_ffi"] = sadb_mariadb_username_ffi
        self.global_dict["sadb_mariadb_password_ffi"] = sadb_mariadb_password_ffi
        self.global_dict["sadb_mariadb_fqdn_ffi"] = sadb_mariadb_fqdn_ffi
        self.global_dict["sadb_mariadb_database_name_ffi "] = sadb_mariadb_database_name_ffi
        self.global_dict["sadb_mariadb_clientcert_ffi"] = sadb_mariadb_clientcert_ffi
        self.global_dict["sadb_mariadb_clientkey_ffi "] = sadb_mariadb_clientkey_ffi
        self.global_dict["sadb_mariadb_cacert_ffi"] = sadb_mariadb_cacert_ffi
        self.global_dict["sadb_mariadb_capath_ffi"] = sadb_mariadb_capath_ffi

        self.global_dict["kmc_crypto_protocol_ffi"] = kmc_crypto_protocol_ffi
        self.global_dict["kmc_crypto_hostname_ffi"] = kmc_crypto_hostname_ffi
        self.global_dict["kmc_crypto_app_uri_ffi"] = kmc_crypto_app_uri_ffi
        self.global_dict["kmc_crypto_mtls_client_cert_ffi"] = kmc_crypto_mtls_client_cert_ffi
        self.global_dict["kmc_crypto_mtls_client_cert_format_ffi"] = kmc_crypto_mtls_client_cert_format_ffi
        self.global_dict["kmc_crypto_mtls_client_key_ffi"] = kmc_crypto_mtls_client_key_ffi
        self.global_dict["kmc_crypto_mtls_client_key_pass_ffi"] = kmc_crypto_mtls_client_key_pass_ffi
        self.global_dict["kmc_crypto_mtls_ca_bundle_ffi"] = kmc_crypto_mtls_ca_bundle_ffi
        self.global_dict["kmc_crypto_mtls_ca_path_ffi"] = kmc_crypto_mtls_ca_path_ffi
        self.global_dict["kmc_crypto_mtls_issuer_cert_ffi"] = kmc_crypto_mtls_issuer_cert_ffi

        kmc_python_c_sdls_interface.lib.sdls_config_kmc_crypto_service(kmc_crypto_protocol_ffi
                                                                       , kmc_crypto_hostname_ffi
                                                                       , self.ffi.cast("uint16_t", kmc_crypto_port)
                                                                       , kmc_crypto_app_uri_ffi
                                                                       , kmc_crypto_mtls_ca_bundle_ffi
                                                                       , kmc_crypto_mtls_ca_path_ffi
                                                                       , self.ffi.cast("uint8_t",
                                                                                       kmc_crypto_mtls_ignore_ssl_hostname_validation)
                                                                       , kmc_crypto_mtls_client_cert_ffi
                                                                       , kmc_crypto_mtls_client_cert_format_ffi
                                                                       , kmc_crypto_mtls_client_key_ffi
                                                                       , kmc_crypto_mtls_client_key_pass_ffi
                                                                       , kmc_crypto_mtls_issuer_cert_ffi
                                                                       )

        # Configure CAM
        home = os.path.expanduser("~")
        cam_enabled = distutils.util.strtobool(config_dict.get("cryptolib.cam.enabled", "false"))
        cam_cookie_file_path = config_dict.get("cryptolib.cam.cookie_file", home + "/.cam_cookie_file")
        cam_keytab_file_path = config_dict.get("cryptolib.cam.keytab_file", "")
        cam_home = config_dict.get("cryptolib.cam.cam_home", "/ammos/css")
        cam_login_method = cam_login_method.get(config_dict.get("cryptolib.cam.login_method", "none"), 0)
        cam_access_manager_uri = config_dict.get("cryptolib.cam.access_manager_uri", "")
        cam_username = config_dict.get("cryptolib.cam.username", "")

        cam_cookie_file_path_ffi = self._ffi_null_or_char(cam_cookie_file_path)
        cam_keytab_file_path_ffi = self._ffi_null_or_char(cam_keytab_file_path)
        cam_home_ffi = self._ffi_null_or_char(cam_home)
        cam_access_manager_uri_ffi = self._ffi_null_or_char(cam_access_manager_uri)
        cam_username_ffi = self._ffi_null_or_char(cam_username)

        self.global_dict["cam_cookie_file_path"] = cam_cookie_file_path_ffi
        self.global_dict["cam_keytab_file_path"] = cam_keytab_file_path_ffi
        self.global_dict["cam_home"] = cam_home_ffi
        self.global_dict["cam_access_manager_uri"] = cam_access_manager_uri_ffi
        self.global_dict["cam_username"] = cam_username_ffi

        if (cam_enabled):
            kmc_python_c_sdls_interface.lib.sdls_config_cam(self.ffi.cast("uint8_t", cam_enabled)
                                                            , cam_cookie_file_path_ffi
                                                            , cam_keytab_file_path_ffi
                                                            , self.ffi.cast("uint8_t", cam_login_method)
                                                            , cam_access_manager_uri_ffi
                                                            , cam_username_ffi
                                                            , cam_home_ffi)

        # Configure Managed Parameters
        managed_parameter_regex = r'cryptolib\.(?P<f_type>tc|tm|aos)\.(?P<scid>\d+)\.(?P<vcid>\d+)\.(?P<tfvn>\d+)\.has_ecf'
        for key in config_dict:
            if ("has_ecf" in key):
                if (not re.match(managed_parameter_regex, key)):
                    raise SdlsClientException(SdlsClientException.INVALID_MANAGED_PARAMETER_FORMAT,
                                              "Invalid Managed Parameter Format. Format must be 'cryptolib.<frame type>.<scid>.<vcid>.<tfvn>.has_ecf=<bool>'")
                key_parts = key.split(".")  # EG, cryptolib.tc.44.1.0.has_ecf
                frame_type = key_parts[1]
                managed_parameter_scid = key_parts[2]
                managed_parameter_vcid = key_parts[3]
                managed_parameter_tfvn = key_parts[4]
                managed_parameter_has_ecf = distutils.util.strtobool(
                    config_dict.get(key))  # ECF is required per managed parameter and has no default.
                managed_parameter_max_frame_length = int(config_dict.get(
                    "cryptolib." + frame_type + "." + managed_parameter_scid + "." + managed_parameter_vcid + "." + managed_parameter_tfvn + ".max_frame_length",
                    1024))
                managed_parameter_has_ecf_enum = managed_parameter_has_ecf
                if frame_type != 'tc':
                    if frame_type == 'tm':
                        # see FecfPresent enum in CryptoLib's crypto_config_structs.h
                        if managed_parameter_has_ecf:
                            managed_parameter_has_ecf_enum = 3
                        else:
                            managed_parameter_has_ecf_enum = 2
                    elif frame_type == 'aos':
                        if managed_parameter_has_ecf:
                            managed_parameter_has_ecf_enum = 5
                        else:
                            managed_parameter_has_ecf_enum = 4
                # managed_parameter_vcid_bitmask = int(config_dict.get("cryptolib.tc."+managed_parameter_scid+"."+managed_parameter_vcid+"."+managed_parameter_tfvn+".vcid_bitmask", 0x3F),16)
                managed_parameter_has_segmentation_header = distutils.util.strtobool(config_dict.get(
                    "cryptolib." + frame_type + "." + managed_parameter_scid + "." + managed_parameter_vcid + "." + managed_parameter_tfvn + ".has_segmentation_header",
                    "false"))
                kmc_python_c_sdls_interface.lib.sdls_config_add_gvcid_managed_parameter(
                    self.ffi.cast("uint8_t", managed_parameter_tfvn)
                    , self.ffi.cast("uint16_t", int(managed_parameter_scid))
                    , self.ffi.cast("uint8_t", int(managed_parameter_vcid))
                    , self.ffi.cast("uint8_t", managed_parameter_has_ecf_enum)
                    , self.ffi.cast("uint8_t", managed_parameter_has_segmentation_header)
                    , self.ffi.cast("uint16_t", int(managed_parameter_max_frame_length)))

        init_status = kmc_python_c_sdls_interface.lib.sdls_init()
        if (init_status != SUCCESS):
            raise SdlsClientException(SdlsClientException.SDLS_INITIALIZATION_ERROR,
                                      "Unable to Initialize KMC SDLS CryptoLib with provided configuration.",
                                      init_status)

    def apply_security_tc(self, input_byte_array):
        '''
        Apply SDLS security to the supplied TC Transfer Frame.

        Parameters
        ----------
        input_byte_array : bytearray
             The TC Transfer Frame byte array that will be wrapped in a security layer.
             Note that the TC transfer frame FECF should not be included in this byte array.

        Returns
        ----------
        bytearray
            The TC Transfer Frame bytearray that has been wrapped in a security layer.
        '''
        if input_byte_array is None:
            raise SdlsClientException(SdlsClientException.NO_FRAME_DATA, "Input Transfer Frame Byte Array is Empty")

        if not isinstance(input_byte_array, bytearray):
            raise SdlsClientException(SdlsClientException.BAD_DATA_FORMAT,
                                      "Input Transfer Frame is not a bytearray, actual type: %s" % type(
                                          input_byte_array).__name__)

        tc_char_in_frame = self.ffi.from_buffer(input_byte_array, require_writable=True)
        output_bytearray = bytearray()
        tc_char_out_frame = self.ffi.from_buffer(output_bytearray, require_writable=True)
        # tc_char_star_in = self.ffi.new("uint8_t *")
        tc_char_star_in = tc_char_in_frame
        tc_char_star_star_out = self.ffi.new("uint8_t **")
        tc_char_star_star_out[0] = tc_char_out_frame
        tc_len_in = self.ffi.cast("uint16_t", len(tc_char_in_frame))
        tc_len_out = self.ffi.new("uint16_t *")
        apply_security_result = kmc_python_c_sdls_interface.lib.apply_security_tc(tc_char_star_in, tc_len_in,
                                                                                  tc_char_star_star_out, tc_len_out)
        if (apply_security_result != SUCCESS):
            raise SdlsClientException(SdlsClientException.APPLY_SECURITY_EXCEPTION,
                                      "KMC CryptoLib Apply Security Exception.", apply_security_result)
        return bytearray(self.ffi.buffer(tc_char_star_star_out[0], tc_len_out[0]))

    def process_security_tc(self, input_byte_array):
        '''
        Process SDLS security from the supplied TC Transfer Frame.

        Parameters
        ----------
        input_byte_array : bytearray
            The TC Transfer Frame byte array that currently wrapped in a security layer, that will be unwrapped
        '''
        if input_byte_array is None:
            raise SdlsClientException(SdlsClientException.NO_FRAME_DATA, "Input Transfer Frame Byte Array is Empty")
        if not isinstance(input_byte_array, bytearray):
            raise SdlsClientException(SdlsClientException.BAD_DATA_FORMAT,
                                      "Input Transfer Frame is not a bytearray, actual type: %s" % type(
                                          input_byte_array).__name__)

        tc_char = self.ffi.from_buffer(input_byte_array, require_writable=True)
        tc_len = self.ffi.new("int *")
        tc_len[0] = len(tc_char)
        tc_result = self.ffi.new("TC_t *")  # Frame that will contain the processed SDLS fields
        process_security_result = kmc_python_c_sdls_interface.lib.process_security_tc(tc_char, tc_len, tc_result)

        if (process_security_result != SUCCESS):
            raise SdlsClientException(SdlsClientException.PROCESS_SECURITY_EXCEPTION,
                                      "KMC CryptoLib Process Security Exception.", process_security_result)

        tc_sdls_object = TC(
            TC_FramePrimaryHeader(tc_result.tc_header.tfvn
                                  , tc_result.tc_header.bypass
                                  , tc_result.tc_header.cc
                                  , tc_result.tc_header.spare
                                  , tc_result.tc_header.scid
                                  , tc_result.tc_header.vcid
                                  , tc_result.tc_header.fl
                                  , tc_result.tc_header.fsn)
            , FrameSecurityHeader(tc_result.tc_sec_header.sh
                                  , tc_result.tc_sec_header.spi
                                  , self.c_array_to_bytearray(tc_result.tc_sec_header.iv,
                                                              tc_result.tc_sec_header.iv_field_len)
                                  , tc_result.tc_sec_header.iv_field_len
                                  , self.c_array_to_bytearray(tc_result.tc_sec_header.sn,
                                                              tc_result.tc_sec_header.sn_field_len)
                                  , tc_result.tc_sec_header.sn_field_len
                                  , self.c_array_to_bytearray(tc_result.tc_sec_header.pad,
                                                              tc_result.tc_sec_header.pad_field_len),
                                  tc_result.tc_sec_header.pad_field_len
                                  )
            , self.c_array_to_bytearray(tc_result.tc_pdu, tc_result.tc_pdu_len)
            , FrameSecurityTrailer(
                self.c_array_to_bytearray(tc_result.tc_sec_trailer.mac, tc_result.tc_sec_trailer.mac_field_len)
                , tc_result.tc_sec_trailer.mac_field_len
                , bytearray()
                , 0
                , tc_result.tc_sec_trailer.fecf)
        )
        self.ffi.release(tc_result)
        # Returning Python objects instead of the CFFI objects is somewhat inefficient. If performance becomes a problem, consider removing this nicety.
        return tc_sdls_object

    def apply_security_aos(self, input_byte_array):
        '''
        Apply SDLS security to the supplied AOS Transfer Frame.

        Parameters
        ----------
        input_byte_array : bytearray
             The AOS Transfer Frame byte array that will be wrapped in a security layer.
             Note that the AOS transfer frame FECF should not be included in this byte array.

        Returns
        ----------
        bytearray
            The AOS Transfer Frame bytearray that has been wrapped in a security layer.
        '''
        if input_byte_array is None:
            raise SdlsClientException(SdlsClientException.NO_FRAME_DATA, "Input Transfer Frame Byte Array is Empty")

        if not isinstance(input_byte_array, bytearray):
            raise SdlsClientException(SdlsClientException.BAD_DATA_FORMAT,
                                      "Input Transfer Frame is not a bytearray, actual type: %s" % type(
                                          input_byte_array).__name__)

        in_copy = bytearray(input_byte_array)
        aos_char_in_frame = self.ffi.from_buffer(in_copy, require_writable=True)
        aos_char_star_in = aos_char_in_frame
        aos_len_in = self.ffi.cast("uint16_t", len(aos_char_in_frame))
        apply_security_result = kmc_python_c_sdls_interface.lib.apply_security_aos(aos_char_star_in, aos_len_in)
        if apply_security_result != SUCCESS:
            raise SdlsClientException(SdlsClientException.APPLY_SECURITY_EXCEPTION,
                                      "KMC CryptoLib Apply Security Exception.", apply_security_result)
        buf = self.ffi.buffer(aos_char_star_in, int(aos_len_in))
        return bytearray(buf)

    def process_security_aos(self, input_byte_array):
        '''
        Process SDLS security from the supplied AOS Transfer Frame.

        Parameters
        ----------
        input_byte_array : bytearray
            The AOS Transfer Frame byte array that currently wrapped in a security layer, that will be unwrapped
        '''
        if input_byte_array is None:
            raise SdlsClientException(SdlsClientException.NO_FRAME_DATA, "Input Transfer Frame Byte Array is Empty")
        if not isinstance(input_byte_array, bytearray):
            raise SdlsClientException(SdlsClientException.BAD_DATA_FORMAT,
                                      "Input Transfer Frame is not a bytearray, actual type: %s" % type(
                                          input_byte_array).__name__)

        in_copy = bytearray(input_byte_array)
        aos_char = self.ffi.from_buffer(in_copy, require_writable=True)
        aos_len = self.ffi.new("int *")
        aos_len[0] = len(aos_char)
        aos_result = self.ffi.new("AOS_t *")  # Frame that will contain the processed SDLS fields
        aos_result_len = self.ffi.new("uint16_t *")
        process_security_result = kmc_python_c_sdls_interface.lib.process_security_aos(aos_char, aos_len[0], aos_result,
                                                                                       aos_result_len)

        if (process_security_result != SUCCESS):
            raise SdlsClientException(SdlsClientException.PROCESS_SECURITY_EXCEPTION,
                                      "KMC CryptoLib Process Security Exception.", process_security_result)

        aos_sdls_object = AOS(
            AOS_FramePrimaryHeader(aos_result.aos_header.tfvn
                                   , aos_result.aos_header.scid
                                   , aos_result.aos_header.vcid
                                   , aos_result.aos_header.vcfc
                                   , aos_result.aos_header.rf
                                   , aos_result.aos_header.sf
                                   , aos_result.aos_header.spare
                                   , aos_result.aos_header.vfcc
                                   , aos_result.aos_header.fhecf)
            , FrameSecurityHeader(
                0,  # segment header
                aos_result.aos_sec_header.spi,
                self.c_array_to_bytearray(aos_result.aos_sec_header.iv, aos_result.aos_sec_header.iv_field_len),
                aos_result.aos_sec_header.iv_field_len,
                self.c_array_to_bytearray(aos_result.aos_sec_header.sn, aos_result.aos_sec_header.sn_field_len),
                aos_result.aos_sec_header.sn_field_len,
                aos_result.aos_sec_header.pad,
                aos_result.aos_sec_header.pad_field_len
            )
            , self.c_array_to_bytearray(aos_result.aos_pdu, aos_result.aos_pdu_len)
            # crypto_structs.h defines this as constant 1786?
            , FrameSecurityTrailer(
                self.c_array_to_bytearray(aos_result.aos_sec_trailer.mac, aos_result.aos_sec_trailer.mac_field_len),
                aos_result.aos_sec_trailer.mac_field_len,
                self.c_array_to_bytearray(aos_result.aos_sec_trailer.ocf, aos_result.aos_sec_trailer.ocf_field_len),
                aos_result.aos_sec_trailer.ocf_field_len,
                aos_result.aos_sec_trailer.fecf)  # CCSDS spec doesn't have an OCF here
        )
        self.ffi.release(aos_result)
        # Returning Python objects instead of the CFFI objects is somewhat inefficient. If performance becomes a problem, consider removing this nicety.
        return aos_sdls_object

    def apply_security_tm(self, input_byte_array):
        '''
        Apply SDLS security to the supplied AOS Transfer Frame.

        Parameters
        ----------
        input_byte_array : bytearray
             The AOS Transfer Frame byte array that will be wrapped in a security layer.
             Note that the AOS transfer frame FECF should not be included in this byte array.

        Returns
        ----------
        bytearray
            The AOS Transfer Frame bytearray that has been wrapped in a security layer.
        '''
        if input_byte_array is None:
            raise SdlsClientException(SdlsClientException.NO_FRAME_DATA, "Input Transfer Frame Byte Array is Empty")

        if not isinstance(input_byte_array, bytearray):
            raise SdlsClientException(SdlsClientException.BAD_DATA_FORMAT,
                                      "Input Transfer Frame is not a bytearray, actual type: %s" % type(
                                          input_byte_array).__name__)

        in_copy = bytearray(input_byte_array)
        tm_char_in_frame = self.ffi.from_buffer(in_copy, require_writable=True)
        tm_char_star_in = tm_char_in_frame
        tm_len_in = self.ffi.cast("uint16_t", len(tm_char_in_frame))
        apply_security_result = kmc_python_c_sdls_interface.lib.apply_security_tm(tm_char_star_in, tm_len_in)
        if apply_security_result != SUCCESS:
            raise SdlsClientException(SdlsClientException.APPLY_SECURITY_EXCEPTION,
                                      "KMC CryptoLib Apply Security Exception.", apply_security_result)
        buf = self.ffi.buffer(tm_char_star_in, int(tm_len_in))
        return bytearray(buf)

    def process_security_tm(self, input_byte_array):
        '''
        Process SDLS security from the supplied AOS Transfer Frame.

        Parameters
        ----------
        input_byte_array : bytearray
            The AOS Transfer Frame byte array that currently wrapped in a security layer, that will be unwrapped
        '''
        if input_byte_array is None:
            raise SdlsClientException(SdlsClientException.NO_FRAME_DATA, "Input Transfer Frame Byte Array is Empty")
        if not isinstance(input_byte_array, bytearray):
            raise SdlsClientException(SdlsClientException.BAD_DATA_FORMAT,
                                      "Input Transfer Frame is not a bytearray, actual type: %s" % type(
                                          input_byte_array).__name__)

        in_copy = bytearray(input_byte_array)
        tm_char = self.ffi.from_buffer(in_copy, require_writable=True)
        tm_len = self.ffi.new("int *")
        tm_len[0] = len(tm_char)
        tm_result = self.ffi.new("TM_t *")  # Frame that will contain the processed SDLS fields
        tm_result_len = self.ffi.new("uint16_t *")
        process_security_result = kmc_python_c_sdls_interface.lib.process_security_tm(tm_char, tm_len[0], tm_result,
                                                                                      tm_result_len)

        if (process_security_result != SUCCESS):
            raise SdlsClientException(SdlsClientException.PROCESS_SECURITY_EXCEPTION,
                                      "KMC CryptoLib Process Security Exception.", process_security_result)

        tm_sdls_object = TM(
            TM_FramePrimaryHeader(tm_result.tm_header.tfvn
                                  , tm_result.tm_header.scid
                                  , tm_result.tm_header.vcid
                                  , tm_result.tm_header.ocff
                                  , tm_result.tm_header.mcfc
                                  , tm_result.tm_header.vcfc
                                  , tm_result.tm_header.tfsh
                                  , tm_result.tm_header.sf
                                  , tm_result.tm_header.pof
                                  , tm_result.tm_header.slid
                                  , tm_result.tm_header.fhp)
            , FrameSecurityHeader(
                0,  # sh
                tm_result.tm_sec_header.spi,
                self.c_array_to_bytearray(tm_result.tm_sec_header.iv, tm_result.tm_sec_header.iv_field_len),
                tm_result.tm_sec_header.iv_field_len,
                self.c_array_to_bytearray(tm_result.tm_sec_header.sn, tm_result.tm_sec_header.sn_field_len),
                tm_result.tm_sec_header.sn_field_len,
                tm_result.tm_sec_header.pad,
                tm_result.tm_sec_header.pad_field_len
            )
            , self.c_array_to_bytearray(tm_result.tm_pdu, tm_result.tm_pdu_len)
            # crypto_structs.h defines this as constant 1786?
            , FrameSecurityTrailer(
                self.c_array_to_bytearray(tm_result.tm_sec_trailer.mac, tm_result.tm_sec_trailer.mac_field_len),
                # Using whole field length here -- only 128 bit macs are currently supported.
                tm_result.tm_sec_trailer.mac_field_len,
                self.c_array_to_bytearray(tm_result.tm_sec_trailer.ocf, tm_result.tm_sec_trailer.ocf_field_len),
                tm_result.tm_sec_trailer.ocf_field_len,
                tm_result.tm_sec_trailer.fecf
            )  # CCSDS spec doesn't have an OCF here
        )
        self.ffi.release(tm_result)
        # Returning Python objects instead of the CFFI objects is somewhat inefficient. If performance becomes a problem, consider removing this nicety.
        return tm_sdls_object

    def shutdown(self):
        return kmc_python_c_sdls_interface.lib.sdls_shutdown()

    def c_array_to_bytearray(self, c_array, c_array_len):
        '''
        Helper function to convert a CFFI uint8 block into a Python bytearray.

        Parameters
        ----------
        :param c_array: CFFI uint8 allocated memory block
        :param c_array_len: length of actual data to be parsed from c_array block.
        :return: bytearray: Python bytearray of data.
        '''
        py_bytearray = bytearray()
        for idx in range(0, c_array_len):
            py_bytearray.append(c_array[idx])
        return py_bytearray

    def _ffi_null_or_char(self, py_obj):
        if py_obj == self.ffi.NULL:
            return self.ffi.NULL
        if py_obj is None:
            return self.ffi.NULL
        if isinstance(py_obj, str):
            if py_obj == "":
                return self.ffi.NULL
            else:
                return self.ffi.new("char[]", py_obj.encode())
        else:
            raise SdlsClientException(SdlsClientException.INVALID_CONFIGURATION_VALUE,
                                      "Unable to Parse Configuration Value." + str(py_obj))

    def _file_exists_or_exception(self, filepath, property_string):
        if (not os.path.exists(filepath)):
            raise SdlsClientException(SdlsClientException.FILE_DOESNT_EXIST,
                                      "Necessary file doesn't exist '%s' from configuration parameter: %s" % (filepath,
                                                                                                              property_string))


class TC_FramePrimaryHeader(NamedTuple):
    tfvn: int  # Transfer Frame Version Number
    bypass: int  # Bypass Flag
    cc: int  # Command Control Flag
    spare: int  # Spare Bits
    scid: int  # Spacecraft ID
    vcid: int  # Virtual Channel ID
    fl: int  # Frame Length
    fsn: int  # Frame Sequence Number


class AOS_FramePrimaryHeader(NamedTuple):
    tfvn: int  # Transfer Frame Version Number
    scid: int  # Spacecraft ID
    vcid: int  # Virtual Channel ID
    vcfc: int  # Virtual Channel Frame Count
    replay: int  # Replay Flag
    vcflag: int  # VC Frame Count Usage Flag
    spare: int  # Reserved Spare
    vcfcc: int  # VC Frame Count Cycle
    fhec: int  # Frame Header Error Control

    def hex(self):
        from bitstring import Bits, BitArray
        tfvn_b = Bits(uint=self.tfvn, length=2)
        scid_b = Bits(uint=self.scid, length=8)
        vcid_b = Bits(uint=self.vcid, length=6)
        vcfc_b = Bits(uint=self.vcfc, length=24)
        replay_b = Bits(uint=self.replay, length=1)
        vcflag_b = Bits(uint=self.vcflag, length=1)
        spare_b = Bits(uint=self.spare, length=2)
        vcfcc_b = Bits(uint=self.vcfcc, length=4)
        # fhec_b = Bits(uint=self.fhec, length=16)
        header = BitArray(length=48)
        header.overwrite(tfvn_b, 0)
        header.overwrite(scid_b, 2)
        header.overwrite(vcid_b, 10)
        header.overwrite(vcfc_b, 16)
        header.overwrite(replay_b, 40)
        header.overwrite(vcflag_b, 41)
        header.overwrite(spare_b, 42)
        header.overwrite(vcfcc_b, 44)
        return header.h


class TM_FramePrimaryHeader(NamedTuple):
    tfvn: int  # Transfer Frame Version Number
    scid: int  # Spacecraft ID
    vcid: int  # Virtual Channel ID
    ocf: int  # Operational Control Field Flag
    mcfc: int  # Master Channel Frame Count
    vcfc: int  # Virtual Channel Frame Count
    shf: int  # Secondary Header Flag
    sf: int  # Synch Flag
    pof: int  # Packet Order Flag
    slid: int  # Segment Length ID
    fhp: int  # First Header Pointer

    def hex(self):
        from bitstring import Bits, BitArray
        tfvn_b = Bits(uint=self.tfvn, length=2)
        scid_b = Bits(uint=self.scid, length=10)
        vcid_b = Bits(uint=self.vcid, length=3)
        ocf_f_b = Bits(uint=self.ocf, length=1)
        mcfc_b = Bits(uint=self.mcfc, length=8)
        vcfc_b = Bits(uint=self.vcfc, length=8)
        shf_f_b = Bits(uint=self.shf, length=1)
        sync_f_b = Bits(uint=self.sf, length=1)
        po_f_b = Bits(uint=self.pof, length=1)
        slid_b = Bits(uint=self.slid, length=2)
        fhp_b = Bits(uint=self.fhp, length=11)
        header = BitArray(length=48)
        header.overwrite(tfvn_b, 0)
        header.overwrite(scid_b, 2)  # 0 + 2
        header.overwrite(vcid_b, 12)  # 2 + 10
        header.overwrite(ocf_f_b, 15)  # 12 + 3
        header.overwrite(mcfc_b, 16)  # 15 + 1
        header.overwrite(vcfc_b, 24)  # 16 + 8
        header.overwrite(shf_f_b, 32)  # 24 + 8
        header.overwrite(sync_f_b, 33)  # 32 + 1
        header.overwrite(po_f_b, 34)  # 33 + 1
        header.overwrite(slid_b, 35)  # 34 + 1
        header.overwrite(fhp_b, 37)  # 35 + 2
        return header.h


class FrameSecurityHeader(NamedTuple):
    sh: int  # Segment Header
    spi: int  # Security Parameter Index
    iv: bytearray  # Initialization Vector
    iv_field_len: int
    sn: bytearray  # Sequence Number
    sn_field_len: int
    pad: bytearray  # Pad
    pad_field_len: int

    def hex(self):
        from bitstring import Bits, BitArray
        length = 16 + (self.iv_field_len * 8) + (self.sn_field_len * 8) + (self.pad_field_len * 8)
        spi_b = Bits(uint=self.spi, length=16)
        iv_b = Bits(self.iv)
        sn_b = Bits(self.sn)
        pad_b = Bits(self.pad)
        idx = 0
        header = BitArray(length=length)
        header.overwrite(spi_b, idx)
        idx += 16
        if self.iv_field_len > 0:
            header.overwrite(iv_b, idx)
            idx += (self.iv_field_len * 8)

        if self.sn_field_len > 0:
            header.overwrite(sn_b, idx)
            idx += (self.sn_field_len * 8)

        if self.pad_field_len > 0:
            header.overwrite(pad_b, idx)
            idx += (self.pad_field_len * 8)

        return header.h


class FrameSecurityTrailer(NamedTuple):
    mac: bytearray  # Message Authentication Code
    mac_field_len: int
    ocf: bytearray
    ocf_field_len: int
    fecf: int  # Frame Error Control Field

    def hex(self):
        from bitstring import Bits, BitArray
        mac_b = Bits(self.mac)
        ocf_b = Bits(self.ocf)
        fecf_b = Bits(uint=self.fecf, length=16)

        length = 16 + (self.mac_field_len * 8) + (self.ocf_field_len * 8)
        idx = 0
        header = BitArray(length=length)
        if self.mac_field_len > 0:
            header.overwrite(mac_b, idx)
            idx += (self.mac_field_len * 8)

        if self.ocf_field_len > 0:
            header.overwrite(ocf_b, idx)
            idx += (self.ocf_field_len * 8)

        header.overwrite(fecf_b, idx)
        return header.h


class TC(NamedTuple):
    tc_header: TC_FramePrimaryHeader
    tc_security_header: FrameSecurityHeader
    tc_pdu: bytearray
    tc_security_trailer: FrameSecurityTrailer


class AOS(NamedTuple):
    aos_header: AOS_FramePrimaryHeader
    aos_security_header: FrameSecurityHeader
    aos_pdu: bytearray
    aos_security_trailer: FrameSecurityTrailer

    def hex(self):
        return self.aos_header.hex() + self.aos_security_header.hex() + self.aos_pdu.hex() + self.aos_security_trailer.hex()


class TM(NamedTuple):
    tm_header: TM_FramePrimaryHeader
    tm_security_header: FrameSecurityHeader
    tm_pdu: bytearray
    tm_security_trailer: FrameSecurityTrailer

    def hex(self):
        return self.tm_header.hex() + self.tm_security_header.hex() + self.tm_pdu.hex() + self.tm_security_trailer.hex()


class SdlsClientException(Exception):
    '''
    This class defines the exceptions that can be raised by the KmcSdlsClient.
    '''
    # CONFIG_FILE_NOT_FOUND = 'CONFIG_FILE_NOT_FOUND' -- will be necessary later when required configs are added
    NO_FRAME_DATA = "NO_FRAME_DATA"
    BAD_DATA_FORMAT = "BAD_DATA_TYPE"
    INVALID_CONNECTION_TYPE = "INVALID_CONNECTION_TYPE"
    INVALID_CONFIGURATION = "INVALID_CONFIGURATION"
    SDLS_INITIALIZATION_ERROR = "SDLS_INITIALIZATION_ERROR"
    APPLY_SECURITY_EXCEPTION = "APPLY_SECURITY_EXCEPTION"
    PROCESS_SECURITY_EXCEPTION = "PROCESS_SECURITY_EXCEPTION"
    INVALID_CONFIGURATION_VALUE = "INVALID_CONFIGURATION_VALUE"
    MISSING_CONFIGURATION_PARAMETER = "MISSING_CONFIGURATION_PARAMETER"
    FILE_DOESNT_EXIST = "FILE_DOESNT_EXIST"
    INVALID_MANAGED_PARAMETER_FORMAT = "INVALID_MANAGED_PARAMETER_FORMAT"

    def __init__(self, error_code, message, cryptolib_error_code=0):
        '''
        SdlsClientException Constructor.

        Parameters
        ----------
        error_code : str
            One of the SdlsClientException error codes.
        message : str
            The detailed message about the exception.
        '''
        error_message = ""
        enum_string = ""

        if (cryptolib_error_code != 0):
            enum_string = kmc_python_c_sdls_interface.ffi.string(
                kmc_python_c_sdls_interface.lib.sdls_get_error_code_enum_string(cryptolib_error_code)).decode('utf-8')
            error_message = " Error code: %d, %s" % (cryptolib_error_code, enum_string)
        Exception.__init__(self, message + error_message)
        self.error_code = error_code
        if (cryptolib_error_code != 0):
            self.error_code = cryptolib_error_code

    def get_error_code(self):
        '''
        Returns the error code of the exception.
        '''
        return self.error_code
