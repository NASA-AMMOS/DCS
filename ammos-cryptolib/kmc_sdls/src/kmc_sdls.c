/*
 * Copyright 2021, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology
 * Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. By accepting
 * this software, the user agrees to comply with all applicable U.S.
 * export laws and regulations. User has the responsibility to obtain
 * export licenses, or other export authority as may be required before
 * exporting such information to foreign countries or providing access to
 * foreign persons.
 */

#include "kmc_sdls.h"
#include <crypto.h>

extern CryptoConfig_t crypto_config;

int32_t sdls_init(void)
{
    return Crypto_Init();
}
int32_t sdls_init_with_configs(CryptoConfig_t* crypto_config_p,GvcidManagedParameters_t* gvcid_managed_parameters_p,SadbMariaDBConfig_t* sadb_mariadb_config_p, CryptographyKmcCryptoServiceConfig_t *cryptography_kmc_crypto_config_p)
{
    return Crypto_Init_With_Configs(crypto_config_p,gvcid_managed_parameters_p,sadb_mariadb_config_p,cryptography_kmc_crypto_config_p);
}
int32_t sdls_init_unit_test(void)
{
    return Crypto_Init_TC_Unit_Test();
}
int32_t apply_security_tc(const uint8_t* p_in_frame, const uint16_t in_frame_length,
                        uint8_t **pp_enc_frame, uint16_t *p_enc_frame_len)
{
    return Crypto_TC_ApplySecurity(p_in_frame,in_frame_length,pp_enc_frame,p_enc_frame_len);
}
int32_t process_security_tc (char* sdls_transfer_frame, int* length, TC_t* tc_sdls_processed_frame)
{
    return Crypto_TC_ProcessSecurity(sdls_transfer_frame, length,tc_sdls_processed_frame);
}

int32_t apply_security_tc_cam (const uint8_t* p_in_frame, const uint16_t in_frame_length, uint8_t **pp_enc_frame, uint16_t *p_enc_frame_len,char* cam_cookies)
{
    return Crypto_TC_ApplySecurity_Cam(p_in_frame, in_frame_length, pp_enc_frame, p_enc_frame_len,cam_cookies);
}
int32_t process_security_tc_cam (char* sdls_transfer_frame, int* length, TC_t* tc_sdls_processed_frame, char* cam_cookies)
{
    return Crypto_TC_ProcessSecurity_Cam(sdls_transfer_frame, length, tc_sdls_processed_frame, cam_cookies);
}

int32_t apply_security_aos(uint8_t *ptBuffer, uint16_t length) {
    return Crypto_AOS_ApplySecurity(ptBuffer, length);
}

int32_t process_security_aos(uint8_t *ptBuffer, uint16_t length, AOS_t* p_enc_frame, uint16_t *p_enc_frame_len) {
    return Crypto_AOS_ProcessSecurity(ptBuffer, length, p_enc_frame, p_enc_frame_len);
}

int32_t apply_security_tm(uint8_t *ptBuffer, uint16_t length) {
    return Crypto_TM_ApplySecurity(ptBuffer, length);
}

int32_t process_security_tm(uint8_t *ptBuffer, uint16_t length, TM_t* p_enc_frame, uint16_t *p_enc_frame_len) {
    return Crypto_TM_ProcessSecurity(ptBuffer, length, p_enc_frame, p_enc_frame_len);
}

int32_t sdls_config_cryptolib(uint8_t sadb_type, uint8_t cryptography_type, uint8_t crypto_create_fecf, uint8_t process_sdls_pdus, uint8_t has_pus_hdr, uint8_t ignore_sa_state, uint8_t ignore_anti_replay, uint8_t unique_sa_per_mapid, uint8_t crypto_check_fecf, uint8_t vcid_bitmask, uint8_t crypto_increment_nontransmitted_iv)
{
    return Crypto_Config_CryptoLib(KEY_TYPE_KMC, MC_TYPE_DISABLED, sadb_type, cryptography_type, IV_INTERNAL, crypto_create_fecf, process_sdls_pdus, has_pus_hdr, ignore_sa_state, ignore_anti_replay, unique_sa_per_mapid, crypto_check_fecf, vcid_bitmask, crypto_increment_nontransmitted_iv);
}
int32_t sdls_config_mariadb(char* mysql_hostname, char* mysql_database, uint16_t mysql_port,
                            uint8_t mysql_require_secure_transport, uint8_t mysql_tls_verify_server,
                            char* mysql_tls_ca, char* mysql_tls_capath, char* mysql_mtls_cert,
                            char* mysql_mtls_key,
                            char* mysql_mtls_client_key_password, char* mysql_username, char* mysql_password)
{
    return Crypto_Config_MariaDB(mysql_hostname, mysql_database,mysql_port,mysql_require_secure_transport,mysql_tls_verify_server,
                                 mysql_tls_ca, mysql_tls_capath, mysql_mtls_cert, mysql_mtls_key,mysql_mtls_client_key_password,mysql_username,mysql_password);
}
int32_t sdls_config_add_gvcid_managed_parameter(uint8_t tfvn, uint16_t scid, uint8_t vcid, uint8_t has_fecf, uint8_t has_segmentation_hdr, uint16_t max_tc_frame_size)
{
    GvcidManagedParameters_t TC_UT_Managed_Parameters = {tfvn, scid, vcid, has_fecf, AOS_FHEC_NA, AOS_IZ_NA, 0, has_segmentation_hdr, max_tc_frame_size, TC_OCF_NA, 1};
    return Crypto_Config_Add_Gvcid_Managed_Parameters(TC_UT_Managed_Parameters);
}
int32_t sdls_config_kmc_crypto_service(char* protocol, char* kmc_crypto_hostname, uint16_t kmc_crypto_port,
                                       char* kmc_crypto_app, char* kmc_tls_ca_bundle, char* kmc_tls_ca_path,
                                       uint8_t kmc_ignore_ssl_hostname_validation, char* mtls_client_cert_path,
                                       char* mtls_client_cert_type, char* mtls_client_key_path,
                                       char* mtls_client_key_pass, char* mtls_issuer_cert)
{
    return Crypto_Config_Kmc_Crypto_Service(protocol, kmc_crypto_hostname, kmc_crypto_port, kmc_crypto_app,kmc_tls_ca_bundle,kmc_tls_ca_path,
                                            kmc_ignore_ssl_hostname_validation, mtls_client_cert_path, mtls_client_cert_type,mtls_client_key_path, mtls_client_key_pass,mtls_issuer_cert);
}
int32_t sdls_config_cam(uint8_t cam_enabled, char* cookie_file_path, char* keytab_file_path, uint8_t login_method, char* access_manager_uri, char* username, char* cam_home)
{
    return Crypto_Config_Cam(cam_enabled, cookie_file_path, keytab_file_path, login_method, access_manager_uri, username, cam_home);
}
char* sdls_get_error_code_enum_string(int32_t crypto_error_code)
{
    return Crypto_Get_Error_Code_Enum_String(crypto_error_code);
}
int32_t sdls_shutdown(void)
{
    if (crypto_config.init_status == UNITIALIZED) {
        return CRYPTO_LIB_SUCCESS;
    } else {
        return Crypto_Shutdown();
    }
}
