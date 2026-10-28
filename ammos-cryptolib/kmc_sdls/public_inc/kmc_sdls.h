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

#ifndef AMMOS_CRYPTOLIB_KMC_SDLS_H
#define AMMOS_CRYPTOLIB_KMC_SDLS_H

#include <crypto_structs.h>
#include <crypto_config_structs.h>

extern int32_t sdls_config_cryptolib(uint8_t sadb_type, uint8_t cryptography_type);

extern int32_t sdls_config_cryptolib_ex(uint8_t sadb_type, uint8_t cryptography_type, uint8_t key_type);

typedef enum
{
    SDLS_SA_SA_STATE = 0,
    SDLS_SA_TFVN,
    SDLS_SA_SCID,
    SDLS_SA_VCID,
    SDLS_SA_MAPID,
    SDLS_SA_LPID,
    SDLS_SA_EST,
    SDLS_SA_AST,
    SDLS_SA_SHIVF_LEN,
    SDLS_SA_SHSNF_LEN,
    SDLS_SA_SHPLF_LEN,
    SDLS_SA_STMACF_LEN,
    SDLS_SA_ECS_LEN,
    SDLS_SA_ECS,
    SDLS_SA_IV_LEN,
    SDLS_SA_ACS_LEN,
    SDLS_SA_ACS,
    SDLS_SA_ABM_LEN,
    SDLS_SA_ARSN_LEN,
    SDLS_SA_ARSNW,
    SDLS_SA_EKID,
    SDLS_SA_AKID,
    SDLS_SA_NULL_IV,
    SDLS_SA_NULL_ARSN,
    SDLS_SA_NUM_FIELDS
} SdlsSaScalarField;

typedef enum
{
    SDLS_SA_BYTES_IV = 0,
    SDLS_SA_BYTES_ARSN,
    SDLS_SA_BYTES_ABM,
    SDLS_SA_NUM_BYTE_FIELDS
} SdlsSaByteField;

/* Accessors for the in-memory SADB's SAs (struct bitfields can't be mapped by cffi). Only for cryptolib.sadb.type=inmemory. */
extern int32_t sdls_sa_set_scalar(uint16_t spi, int32_t field, uint32_t value);
extern int32_t sdls_sa_get_scalar(uint16_t spi, int32_t field, uint32_t *value);
extern int32_t sdls_sa_set_bytes(uint16_t spi, int32_t field, const uint8_t *data, uint16_t len);
extern int32_t sdls_sa_get_bytes(uint16_t spi, int32_t field, uint8_t *data, uint16_t max_len, uint16_t *len);

extern int32_t sdls_config_cryptolib_tc(uint8_t crypto_create_fecf, uint8_t process_sdls_pdus, uint8_t has_pus_hdr,
                                        uint8_t ignore_anti_replay, uint8_t ignore_sa_state,
                                        uint8_t unique_sa_per_mapid,
                                        uint8_t crypto_check_fecf, uint8_t vcid_bitmask,
                                        uint8_t crypto_increment_nontransmitted_iv);

extern int32_t sdls_config_cryptolib_tm(uint8_t crypto_create_fecf, uint8_t ignore_anti_replay,
                                        uint8_t crypto_check_fecf,
                                        uint8_t vcid_bitmask, uint8_t crypto_increment_nontransmitted_iv);

extern int32_t sdls_config_cryptolib_aos(uint8_t crypto_create_fecf, uint8_t ignore_anti_replay,
                                         uint8_t crypto_check_fecf,
                                         uint8_t vcid_bitmask, uint8_t crypto_increment_nontransmitted_iv);

extern int32_t sdls_config_add_gvcid_managed_parameter_tc(uint8_t tfvn, uint16_t scid, uint8_t vcid, uint8_t has_fecf,
                                                          uint8_t has_segmentation_hdr, uint16_t max_tc_frame_size);

extern int32_t sdls_config_add_gvcid_managed_parameter_tm(uint8_t tfvn, uint16_t scid, uint8_t vcid, uint8_t has_fecf,
                                                          uint16_t max_tm_frame_size, uint8_t has_ocf);

extern int32_t sdls_config_add_gvcid_managed_parameter_aos(uint8_t tfvn, uint8_t scid, uint8_t vcid, uint8_t has_fecf,
                                                           uint8_t has_fhec, uint8_t has_iz, uint16_t iz_len,
                                                           uint16_t max_aos_frame_size, uint8_t has_ocf);

extern int32_t sdls_config_mariadb(char *mysql_hostname, char *mysql_database, uint16_t mysql_port,
                                   uint8_t mysql_require_secure_transport, uint8_t mysql_tls_verify_server,
                                   char *mysql_tls_ca, char *mysql_tls_capath, char *mysql_mtls_cert,
                                   char *mysql_mtls_key,
                                   char *mysql_mtls_client_key_password, char *mysql_username, char *mysql_password);

extern int32_t sdls_config_add_gvcid_managed_parameter(uint8_t tfvn, uint16_t scid, uint8_t vcid, uint8_t has_fecf,
                                                       uint8_t has_segmentation_hdr, uint16_t max_tc_frame_size);

extern int32_t sdls_config_kmc_crypto_service(char *protocol, char *kmc_crypto_hostname, uint16_t kmc_crypto_port,
                                              char *kmc_crypto_app, char *kmc_tls_ca_bundle, char *kmc_tls_ca_path,
                                              uint8_t kmc_ignore_ssl_hostname_validation, char *mtls_client_cert_path,
                                              char *mtls_client_cert_type, char *mtls_client_key_path,
                                              char *mtls_client_key_pass, char *mtls_issuer_cert);

extern int32_t sdls_config_cam(uint8_t cam_enabled, char *cookie_file_path, char *keytab_file_path,
                               uint8_t login_method, char *access_manager_uri, char *username, char *cam_home);

extern int32_t sdls_init(void);

extern int32_t sdls_init_with_configs(CryptoConfigGlobal_t *crypto_config_p,
                                      TCGvcidManagedParameters_t *gvcid_managed_parameters_p,
                                      SadbMariaDBConfig_t *sadb_mariadb_config_p,
                                      CryptographyKmcCryptoServiceConfig_t *cryptography_kmc_crypto_config_p);

extern int32_t sdls_init_unit_test(void);

extern int32_t sdls_shutdown(void);

extern int32_t apply_security_tc(const uint8_t *p_in_frame, const uint16_t in_frame_length, uint8_t **pp_enc_frame,
                                 uint16_t *p_enc_frame_len);

extern int32_t process_security_tc(char *sdls_transfer_frame, int *length, TC_t *tc_sdls_processed_frame);

extern int32_t apply_security_tc_cam(const uint8_t *p_in_frame, const uint16_t in_frame_length, uint8_t **pp_enc_frame,
                                     uint16_t *p_enc_frame_len, char *cam_cookies);

extern int32_t process_security_tc_cam(char *sdls_transfer_frame, int *length, TC_t *tc_sdls_processed_frame,
                                       char *cam_cookies);

extern int32_t apply_security_tm(uint8_t *p_in_frame, uint16_t in_frame_length);

extern int32_t process_security_tm(uint8_t *ptBuffer, uint16_t length, TM_t *p_enc_frame, uint16_t *p_enc_frame_len);

extern int32_t apply_security_aos(uint8_t *p_in_frame, uint16_t in_frame_length);

extern int32_t process_security_aos(uint8_t *ptBuffer, uint16_t length, AOS_t *p_enc_frame, uint16_t *p_enc_frame_len);

extern char *sdls_get_error_code_enum_string(int32_t crypto_error_code);

#endif //AMMOS_CRYPTOLIB_KMC_SDLS_H
