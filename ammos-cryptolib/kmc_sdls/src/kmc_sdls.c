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
#include <string.h>

extern CryptoConfigGlobal_t crypto_config_global;

int32_t sdls_init(void) {
    return Crypto_Init();
}

int32_t sdls_init_with_configs(CryptoConfigGlobal_t *crypto_config_p,
                               TCGvcidManagedParameters_t *gvcid_managed_parameters_p,
                               SadbMariaDBConfig_t *sadb_mariadb_config_p,
                               CryptographyKmcCryptoServiceConfig_t *cryptography_kmc_crypto_config_p) {
    return Crypto_Init_With_Configs(crypto_config_p, gvcid_managed_parameters_p, sadb_mariadb_config_p,
                                    cryptography_kmc_crypto_config_p);
}

int32_t sdls_init_unit_test(void) {
    return Crypto_Init_TC_Unit_Test();
}

int32_t apply_security_tc(const uint8_t *p_in_frame, const uint16_t in_frame_length,
                          uint8_t **pp_enc_frame, uint16_t *p_enc_frame_len) {
    return Crypto_TC_ApplySecurity(p_in_frame, in_frame_length, pp_enc_frame, p_enc_frame_len);
}

int32_t process_security_tc(char *sdls_transfer_frame, int *length, TC_t *tc_sdls_processed_frame) {
    return Crypto_TC_ProcessSecurity(sdls_transfer_frame, length, tc_sdls_processed_frame);
}

int32_t apply_security_tc_cam(const uint8_t *p_in_frame, const uint16_t in_frame_length, uint8_t **pp_enc_frame,
                              uint16_t *p_enc_frame_len, char *cam_cookies) {
    return Crypto_TC_ApplySecurity_Cam(p_in_frame, in_frame_length, pp_enc_frame, p_enc_frame_len, cam_cookies);
}

int32_t process_security_tc_cam(char *sdls_transfer_frame, int *length, TC_t *tc_sdls_processed_frame,
                                char *cam_cookies) {
    return Crypto_TC_ProcessSecurity_Cam(sdls_transfer_frame, length, tc_sdls_processed_frame, cam_cookies);
}

int32_t apply_security_aos(uint8_t *ptBuffer, uint16_t length) {
    return Crypto_AOS_ApplySecurity(ptBuffer, length);
}

int32_t process_security_aos(uint8_t *ptBuffer, uint16_t length, AOS_t *p_enc_frame, uint16_t *p_enc_frame_len) {
    return Crypto_AOS_ProcessSecurity(ptBuffer, length, p_enc_frame, p_enc_frame_len);
}

int32_t apply_security_tm(uint8_t *ptBuffer, uint16_t length) {
    return Crypto_TM_ApplySecurity(ptBuffer, length);
}

int32_t process_security_tm(uint8_t *ptBuffer, uint16_t length, TM_t *p_enc_frame, uint16_t *p_enc_frame_len) {
    return Crypto_TM_ProcessSecurity(ptBuffer, length, p_enc_frame, p_enc_frame_len);
}

int32_t sdls_config_cryptolib(uint8_t sadb_type, uint8_t cryptography_type) {
    return Crypto_Config_CryptoLib(KEY_TYPE_KMC, MC_TYPE_DISABLED, sadb_type, cryptography_type, IV_INTERNAL);
}

int32_t sdls_config_cryptolib_ex(uint8_t sadb_type, uint8_t cryptography_type, uint8_t key_type) {
#ifndef KMC_SDLS_KEY_INTERNAL
    if (key_type == KEY_TYPE_INTERNAL) {
        return CRYPTO_LIB_ERROR;
    }
#endif
    return Crypto_Config_CryptoLib(key_type, MC_TYPE_DISABLED, sadb_type, cryptography_type, IV_INTERNAL);
}

static int32_t sdls_sa_lookup(uint16_t spi, SecurityAssociation_t **sa_out) {
    if (sa_if == NULL || sa_out == NULL) {
        return CRYPTO_LIB_ERROR;
    }
    *sa_out = NULL;
    int32_t status = sa_if->sa_get_from_spi(spi, sa_out);
    /* The in-memory SADB hands back the pointer before validating the SA contents. A default or half-edited SA can
     * legitimately fail validation, so only report an error when no SA was returned. */
    return (*sa_out != NULL) ? CRYPTO_LIB_SUCCESS : status;
}

int32_t sdls_sa_set_scalar(uint16_t spi, int32_t field, uint32_t value) {
    SecurityAssociation_t *sa;
    int32_t status = sdls_sa_lookup(spi, &sa);
    if (status != CRYPTO_LIB_SUCCESS) {
        return status;
    }
    switch (field) {
        case SDLS_SA_SA_STATE: sa->sa_state = value; break;
        case SDLS_SA_TFVN: sa->gvcid_blk.tfvn = value; break;
        case SDLS_SA_SCID: sa->gvcid_blk.scid = value; break;
        case SDLS_SA_VCID: sa->gvcid_blk.vcid = value; break;
        case SDLS_SA_MAPID: sa->gvcid_blk.mapid = value; break;
        case SDLS_SA_LPID: sa->lpid = value; break;
        case SDLS_SA_EST: sa->est = value; break;
        case SDLS_SA_AST: sa->ast = value; break;
        case SDLS_SA_SHIVF_LEN: sa->shivf_len = value; break;
        case SDLS_SA_SHSNF_LEN: sa->shsnf_len = value; break;
        case SDLS_SA_SHPLF_LEN: sa->shplf_len = value; break;
        case SDLS_SA_STMACF_LEN: sa->stmacf_len = value; break;
        case SDLS_SA_ECS_LEN: sa->ecs_len = value; break;
        case SDLS_SA_ECS: sa->ecs = value; break;
        case SDLS_SA_IV_LEN: sa->iv_len = value; break;
        case SDLS_SA_ACS_LEN: sa->acs_len = value; break;
        case SDLS_SA_ACS: sa->acs = value; break;
        case SDLS_SA_ABM_LEN: sa->abm_len = value; break;
        case SDLS_SA_ARSN_LEN: sa->arsn_len = value; break;
        case SDLS_SA_ARSNW: sa->arsnw = value; break;
        case SDLS_SA_EKID: sa->ekid = value; break;
        case SDLS_SA_AKID: sa->akid = value; break;
        case SDLS_SA_NULL_IV: sa->null_iv = value; break;
        case SDLS_SA_NULL_ARSN: sa->null_arsn = value; break;
        default: return CRYPTO_LIB_ERROR;
    }
    return CRYPTO_LIB_SUCCESS;
}

int32_t sdls_sa_get_scalar(uint16_t spi, int32_t field, uint32_t *value) {
    SecurityAssociation_t *sa;
    int32_t status = sdls_sa_lookup(spi, &sa);
    if (status != CRYPTO_LIB_SUCCESS || value == NULL) {
        return status != CRYPTO_LIB_SUCCESS ? status : CRYPTO_LIB_ERROR;
    }
    switch (field) {
        case SDLS_SA_SA_STATE: *value = sa->sa_state; break;
        case SDLS_SA_TFVN: *value = sa->gvcid_blk.tfvn; break;
        case SDLS_SA_SCID: *value = sa->gvcid_blk.scid; break;
        case SDLS_SA_VCID: *value = sa->gvcid_blk.vcid; break;
        case SDLS_SA_MAPID: *value = sa->gvcid_blk.mapid; break;
        case SDLS_SA_LPID: *value = sa->lpid; break;
        case SDLS_SA_EST: *value = sa->est; break;
        case SDLS_SA_AST: *value = sa->ast; break;
        case SDLS_SA_SHIVF_LEN: *value = sa->shivf_len; break;
        case SDLS_SA_SHSNF_LEN: *value = sa->shsnf_len; break;
        case SDLS_SA_SHPLF_LEN: *value = sa->shplf_len; break;
        case SDLS_SA_STMACF_LEN: *value = sa->stmacf_len; break;
        case SDLS_SA_ECS_LEN: *value = sa->ecs_len; break;
        case SDLS_SA_ECS: *value = sa->ecs; break;
        case SDLS_SA_IV_LEN: *value = sa->iv_len; break;
        case SDLS_SA_ACS_LEN: *value = sa->acs_len; break;
        case SDLS_SA_ACS: *value = sa->acs; break;
        case SDLS_SA_ABM_LEN: *value = sa->abm_len; break;
        case SDLS_SA_ARSN_LEN: *value = sa->arsn_len; break;
        case SDLS_SA_ARSNW: *value = sa->arsnw; break;
        case SDLS_SA_EKID: *value = sa->ekid; break;
        case SDLS_SA_AKID: *value = sa->akid; break;
        case SDLS_SA_NULL_IV: *value = sa->null_iv; break;
        case SDLS_SA_NULL_ARSN: *value = sa->null_arsn; break;
        default: return CRYPTO_LIB_ERROR;
    }
    return CRYPTO_LIB_SUCCESS;
}

static int32_t sdls_sa_byte_field(SecurityAssociation_t *sa, int32_t field, uint8_t **buf, uint16_t *cap) {
    switch (field) {
        case SDLS_SA_BYTES_IV: *buf = sa->iv; *cap = sizeof(sa->iv); break;
        case SDLS_SA_BYTES_ARSN: *buf = sa->arsn; *cap = sizeof(sa->arsn); break;
        case SDLS_SA_BYTES_ABM: *buf = sa->abm; *cap = sizeof(sa->abm); break;
        default: return CRYPTO_LIB_ERROR;
    }
    return CRYPTO_LIB_SUCCESS;
}

int32_t sdls_sa_set_bytes(uint16_t spi, int32_t field, const uint8_t *data, uint16_t len) {
    SecurityAssociation_t *sa;
    uint8_t *buf;
    uint16_t cap;
    int32_t status = sdls_sa_lookup(spi, &sa);
    if (status != CRYPTO_LIB_SUCCESS) {
        return status;
    }
    if (sdls_sa_byte_field(sa, field, &buf, &cap) != CRYPTO_LIB_SUCCESS || data == NULL || len > cap) {
        return CRYPTO_LIB_ERROR;
    }
    memset(buf, 0, cap);
    memcpy(buf, data, len);
    return CRYPTO_LIB_SUCCESS;
}

int32_t sdls_sa_get_bytes(uint16_t spi, int32_t field, uint8_t *data, uint16_t max_len, uint16_t *len) {
    SecurityAssociation_t *sa;
    uint8_t *buf;
    uint16_t cap;
    int32_t status = sdls_sa_lookup(spi, &sa);
    if (status != CRYPTO_LIB_SUCCESS) {
        return status;
    }
    if (sdls_sa_byte_field(sa, field, &buf, &cap) != CRYPTO_LIB_SUCCESS || data == NULL || len == NULL) {
        return CRYPTO_LIB_ERROR;
    }
    *len = cap < max_len ? cap : max_len;
    memcpy(data, buf, *len);
    return CRYPTO_LIB_SUCCESS;
}

int32_t sdls_config_cryptolib_tc(uint8_t crypto_create_fecf, uint8_t process_sdls_pdus, uint8_t has_pus_hdr,
                                 uint8_t ignore_anti_replay, uint8_t ignore_sa_state, uint8_t unique_sa_per_mapid,
                                 uint8_t crypto_check_fecf, uint8_t vcid_bitmask,
                                 uint8_t crypto_increment_nontransmitted_iv) {
    return Crypto_Config_TC(crypto_create_fecf, process_sdls_pdus, has_pus_hdr,
                            ignore_anti_replay, ignore_sa_state, unique_sa_per_mapid,
                            crypto_check_fecf, vcid_bitmask,
                            crypto_increment_nontransmitted_iv);
}

int32_t sdls_config_cryptolib_tm(uint8_t crypto_create_fecf, uint8_t ignore_anti_replay, uint8_t crypto_check_fecf,
                                 uint8_t vcid_bitmask, uint8_t crypto_increment_nontransmitted_iv) {
    return Crypto_Config_TM(crypto_create_fecf, ignore_anti_replay, crypto_check_fecf,
                            vcid_bitmask, crypto_increment_nontransmitted_iv);
}

int32_t sdls_config_cryptolib_aos(uint8_t crypto_create_fecf, uint8_t ignore_anti_replay, uint8_t crypto_check_fecf,
                                  uint8_t vcid_bitmask, uint8_t crypto_increment_nontransmitted_iv) {
    return Crypto_Config_AOS(crypto_create_fecf, ignore_anti_replay, crypto_check_fecf,
                             vcid_bitmask, crypto_increment_nontransmitted_iv);
}

int32_t sdls_config_mariadb(char *mysql_hostname, char *mysql_database, uint16_t mysql_port,
                            uint8_t mysql_require_secure_transport, uint8_t mysql_tls_verify_server,
                            char *mysql_tls_ca, char *mysql_tls_capath, char *mysql_mtls_cert,
                            char *mysql_mtls_key,
                            char *mysql_mtls_client_key_password, char *mysql_username, char *mysql_password) {
    return Crypto_Config_MariaDB(mysql_hostname, mysql_database, mysql_port, mysql_require_secure_transport,
                                 mysql_tls_verify_server,
                                 mysql_tls_ca, mysql_tls_capath, mysql_mtls_cert, mysql_mtls_key,
                                 mysql_mtls_client_key_password, mysql_username, mysql_password);
}

int32_t sdls_config_add_gvcid_managed_parameter(uint8_t tfvn, uint16_t scid, uint8_t vcid, uint8_t has_fecf,
                                                uint8_t has_segmentation_hdr, uint16_t max_tc_frame_size) {
    TCGvcidManagedParameters_t TC_UT_Managed_Parameters = {
        tfvn, scid, vcid, has_fecf, has_segmentation_hdr, max_tc_frame_size, 1
    };
    return Crypto_Config_Add_TC_Gvcid_Managed_Parameters(TC_UT_Managed_Parameters);
}

int32_t sdls_config_add_gvcid_managed_parameter_tc(uint8_t tfvn, uint16_t scid, uint8_t vcid, uint8_t has_fecf,
                                                   uint8_t has_segmentation_hdr, uint16_t max_tc_frame_size) {
    TCGvcidManagedParameters_t TC_UT_Managed_Parameters = {
        tfvn, scid, vcid, has_fecf, has_segmentation_hdr, max_tc_frame_size, 1
    };
    return Crypto_Config_Add_TC_Gvcid_Managed_Parameters(TC_UT_Managed_Parameters);
}

int32_t sdls_config_add_gvcid_managed_parameter_tm(uint8_t tfvn, uint16_t scid, uint8_t vcid, uint8_t has_fecf,
                                                   uint16_t max_tm_frame_size, uint8_t has_ocf) {
    TMGvcidManagedParameters_t TM_UT_Managed_Parameters = {tfvn, scid, vcid, has_fecf, max_tm_frame_size, has_ocf, 1};
    return Crypto_Config_Add_TM_Gvcid_Managed_Parameters(TM_UT_Managed_Parameters);
}

int32_t sdls_config_add_gvcid_managed_parameter_aos(uint8_t tfvn, uint8_t scid, uint8_t vcid, uint8_t has_fecf,
                                                    uint8_t has_fhec, uint8_t has_iz, uint16_t iz_len,
                                                    uint16_t max_aos_frame_size, uint8_t has_ocf) {
    AOSGvcidManagedParameters_t AOS_UT_Managed_Parameters = {
        tfvn, scid, vcid, has_fecf, has_fhec, has_iz, iz_len, max_aos_frame_size, has_ocf, 1
    };
    return Crypto_Config_Add_AOS_Gvcid_Managed_Parameters(AOS_UT_Managed_Parameters);
}

int32_t sdls_config_kmc_crypto_service(char *protocol, char *kmc_crypto_hostname, uint16_t kmc_crypto_port,
                                       char *kmc_crypto_app, char *kmc_tls_ca_bundle, char *kmc_tls_ca_path,
                                       uint8_t kmc_ignore_ssl_hostname_validation, char *mtls_client_cert_path,
                                       char *mtls_client_cert_type, char *mtls_client_key_path,
                                       char *mtls_client_key_pass, char *mtls_issuer_cert) {
    return Crypto_Config_Kmc_Crypto_Service(protocol, kmc_crypto_hostname, kmc_crypto_port, kmc_crypto_app,
                                            kmc_tls_ca_bundle, kmc_tls_ca_path,
                                            kmc_ignore_ssl_hostname_validation, mtls_client_cert_path,
                                            mtls_client_cert_type, mtls_client_key_path, mtls_client_key_pass,
                                            mtls_issuer_cert);
}

int32_t sdls_config_cam(uint8_t cam_enabled, char *cookie_file_path, char *keytab_file_path, uint8_t login_method,
                        char *access_manager_uri, char *username, char *cam_home) {
    return Crypto_Config_Cam(cam_enabled, cookie_file_path, keytab_file_path, login_method, access_manager_uri,
                             username, cam_home);
}

char *sdls_get_error_code_enum_string(int32_t crypto_error_code) {
    return Crypto_Get_Error_Code_Enum_String(crypto_error_code);
}

int32_t sdls_shutdown(void) {
    if (crypto_config_global.init_status == UNINITIALIZED) {
        return CRYPTO_LIB_SUCCESS;
    } else {
        return Crypto_Shutdown();
    }
}
