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

import unittest
import binascii
#Import the KMC SDLS Client
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient

#Configure KMC SDLS Client for MariaDB with managed parameters
kmc_mmt_kmccrypto_mariadb_default_config_rev =  ['cryptolib.sadb.type=mariadb','cryptolib.sadb.mariadb.password=sadb_password','cryptolib.crypto.type=kmccryptoservice','cryptolib.process_tc.ignore_antireplay=true',
                                                 'cryptolib.process_tc.ignore_sa_state=true','cryptolib.process_tc.process_pdus=false',
                                                 'cryptolib.crypto.kmccryptoservice.protocol=https','cryptolib.crypto.kmccryptoservice.fqdn=asec-dev-vm18.jpl.nasa.gov',
                                                 'cryptolib.crypto.kmccryptoservice.app=crypto-service','cryptolib.crypto.kmccryptoservice.mtls.clientcert=/home/isaleh/git/KMC/CryptoLib-IbraheemYSaleh/util/etc/local-test-cert.pem',
                                                 'cryptolib.crypto.kmccryptoservice.mtls.clientcertformat=PEM','cryptolib.crypto.kmccryptoservice.mtls.clientkey=/home/isaleh/git/KMC/CryptoLib-IbraheemYSaleh/util/etc/local-test-key.pem',
                                                 'cryptolib.crypto.kmccryptoservice.cacert=/home/isaleh/git/KMC/CryptoLib-IbraheemYSaleh/util/etc/ammos-ca-bundle.crt','cryptolib.crypto.kmccryptoservice.verifyserver=true',
                                                 'cryptolib.tc.vcid_bitmask=0x07',"cryptolib.apply_tc.create_ecf=true",
                                                 'cryptolib.tc.44.0.0.has_segmentation_header=false',
                                                 'cryptolib.tc.44.0.0.has_pus_header=false','cryptolib.tc.44.0.0.has_ecf=true',
                                                 'cryptolib.tc.44.0.0.max_frame_length=1024','cryptolib.tc.44.1.0.has_segmentation_header=false',
                                                 'cryptolib.tc.44.1.0.has_pus_header=false','cryptolib.tc.44.1.0.has_ecf=true',
                                                 'cryptolib.tc.44.1.0.max_frame_length=1024','cryptolib.tc.44.2.0.has_segmentation_header=false',
                                                 'cryptolib.tc.44.2.0.has_pus_header=false','cryptolib.tc.44.2.0.has_ecf=true',
                                                 'cryptolib.tc.44.2.0.max_frame_length=1024','cryptolib.tc.44.3.0.has_segmentation_header=false',
                                                 'cryptolib.tc.44.3.0.has_pus_header=false','cryptolib.tc.44.3.0.has_ecf=true',
                                                 'cryptolib.tc.44.3.0.max_frame_length=1024',
                                                 'cryptolib.cam.enabled=true','cryptolib.cam.cookie_file=/home/isaleh/.cam_cookie_file',
                                                 'cryptolib.cam.keytab_file=/home/isaleh/secret/testuser3300.kt','cryptolib.cam.login_method=keytab_file',
                                                 'cryptolib.cam.access_manager_uri=https://asec-dev-vm10.jpl.nasa.gov','cryptolib.cam.username=testuser3300']

class TestConfigMethods(unittest.TestCase):
    # def test_config_prop_init_mariadb_mmt_defaults(self):
    #     k = KmcSdlsClient.KmcSdlsClient(kmc_mmt_kmccrypto_mariadb_default_config_rev)
    #     k.shutdown()
    #     #self.assertEqual('foo'.upper(),'FOO')
    def test_simple_mariadb_apply_security(self):
        #Initialize the KmcSdlsClient object with configuration
        k = KmcSdlsClient.KmcSdlsClient(kmc_mmt_kmccrypto_mariadb_default_config_rev)
        #tc = bytearray(binascii.unhexlify("20030008000001")) #no FECF
        #Convert a hex-string representation of a JPL frame into a python bytearray
        tc = bytearray(binascii.unhexlify("202c0408000001bd37")) #With FECF Passed in
        #Apply security to the JPL telecommand transfer frame, store the result
        result = k.apply_security_tc(tc)
        #Process the security headers on the result of the previous operation, and store the result
        reversed_tc = k.process_security_tc(result)
        #Set a variable for the expected data from the original telecommand transfer frame (0001 is a CMD_NO_OP)
        tc_data_expect = "0001"
        print(reversed_tc)
        #Verify that the processed security result's data matches the original unencrypted data.
        print("PDU:", reversed_tc.tc_pdu.to_hex())
        print("IV:", reversed_tc.tc_security_header.iv.to_hex())
        print("MAC:", reversed_tc.tc_security_trailer.mac.to_hex())
        self.assertEqual(tc_data_expect, reversed_tc.tc_pdu.to_hex())
        k.shutdown()
        #print(result.hex())
        #result_string = binascii.hexlify(result)
        #print("THE RESULT IS:",result_string)

if __name__ == '__main__':
    unittest.main()