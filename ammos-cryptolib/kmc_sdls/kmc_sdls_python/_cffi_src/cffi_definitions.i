typedef struct
{
    uint8_t tfvn : 4;
    uint16_t scid : 16;
    uint16_t vcid : 6;
    uint8_t mapid : 6;
} crypto_gvcid_t;

typedef struct
{
    uint16_t spi;
    uint16_t ekid;
    uint16_t akid;
    char ek_ref[250];
    char ak_ref[250];
    uint8_t sa_state : 2;
    crypto_gvcid_t gvcid_blk;
    uint8_t lpid;
    uint8_t est : 1;
    uint8_t ast : 1;
    uint8_t shivf_len : 6;
    uint8_t shsnf_len : 6;
    uint8_t shplf_len : 2;
    uint8_t stmacf_len : 8;
    uint8_t ecs;
    uint8_t ecs_len : 8;
    uint8_t iv[16];
    uint8_t iv_len;
    uint8_t acs_len : 8;
    uint8_t acs;
    uint16_t abm_len : 16;
    uint8_t abm[1786];
    uint8_t arsn_len : 8;
    uint8_t arsn[20];
    uint8_t arsnw_len : 8;
    uint16_t arsnw;
} SecurityAssociation_t;

typedef struct
{
    uint8_t cwt : 1;
    uint8_t vnum : 3;
    uint8_t af : 1;
    uint8_t bsnf : 1;
    uint8_t bmacf : 1;
    uint8_t ispif : 1;
    uint16_t lspiu : 16;
    uint8_t snval : 8;
} SDLS_FSR_t;

typedef struct
{
    uint8_t type : 1;
    uint8_t uf : 1;
    uint8_t sg : 2;
    uint8_t pid : 4;
    uint16_t pdu_len;
} SDLS_TLV_Hdr_t;

typedef struct
{
    SDLS_TLV_Hdr_t hdr;
    uint8_t data[494];
} SDLS_TLV_t;

typedef struct
{
    uint16_t ekid;
    uint8_t ek[512];
} SDLS_EKB_t;

typedef struct
{
    uint16_t mkid;
    uint8_t iv[16];
    SDLS_EKB_t EKB[30];
    uint8_t mac[64];
} SDLS_OTAR_t;

typedef struct
{
    uint16_t kid : 16;
} SDLS_KEY_t;

typedef struct
{
    SDLS_KEY_t kblk[98];
} SDLS_KEY_BLK_t;

typedef struct
{
    uint16_t kid_first : 16;
    uint16_t kid_last : 16;
} SDLS_KEY_INVENTORY_CMD_t;

typedef struct
{
    uint16_t kid : 16;
    uint16_t key_state : 8;
} SDLS_KEY_INVENTORY_RPLY_t;

typedef struct
{
    uint16_t kid : 16;
    uint8_t challenge[16];
} SDLS_KEYV_CMD_BLK_t;

typedef struct
{
    SDLS_KEYV_CMD_BLK_t blk[29];
} SDLS_KEYV_CMD_t;

typedef struct
{
    uint16_t kid : 16;
    uint8_t iv[12];
    uint8_t challenged[16];
    uint8_t mac[64];
} SDLS_KEYV_RPLY_BLK_t;

typedef struct
{
    SDLS_KEYV_RPLY_BLK_t blk[29];
} SDLS_KEYV_RPLY_t;

typedef struct
{
    uint16_t kid : 16;
    uint8_t challenged[10];
} SDLS_KEYDB_CMD_t;

typedef struct
{
    uint16_t kid : 16;
    uint8_t iv[16];
    uint8_t challenged[10];
    uint8_t cmac[10];
} SDLS_KEYDB_RPLY_t;

typedef struct
{
    uint16_t spi : 16;
    uint8_t lpid : 8;
} SDLS_SA_STATUS_RPLY_t;

typedef struct
{
    uint16_t spi : 16;
    uint8_t arsn[20];
} SDLS_SA_READ_ARSN_RPLY_t;

typedef struct
{
    uint16_t num_se;
    uint16_t rs;
} SDLS_MC_LOG_RPLY_t;

typedef struct
{
    uint8_t emt : 8;
    uint16_t em_len : 16;
    uint8_t emv[4];
} SDLS_MC_DUMP_RPLY_t;

typedef struct
{
    SDLS_MC_DUMP_RPLY_t blk[50];
} SDLS_MC_DUMP_BLK_RPLY_t;

typedef struct
{
    uint8_t str : 8;
} SDLS_MC_ST_RPLY_t;

typedef struct
{
    uint8_t snv[16];
} SDLS_MC_SN_RPLY_t;

typedef struct
{
    uint8_t tfvn : 2;
    uint8_t bypass : 1;
    uint8_t cc : 1;
    uint8_t spare : 2;
    uint16_t scid : 10;
    uint8_t vcid : 6;
    uint16_t fl : 10;
    uint8_t fsn : 8;
} TC_FramePrimaryHeader_t;

typedef struct
{
    uint8_t sh : 8;
    uint16_t spi;
    uint8_t iv[16];
    uint8_t iv_field_len;
    uint8_t sn[16];
    uint8_t sn_field_len;
    uint8_t pad[2];
    uint8_t pad_field_len;
} TC_FrameSecurityHeader_t;

typedef struct
{
    uint8_t mac[64];
    uint8_t mac_field_len;
    uint16_t fecf;
} TC_FrameSecurityTrailer_t;

typedef struct
{
    TC_FramePrimaryHeader_t tc_header;
    TC_FrameSecurityHeader_t tc_sec_header;
    uint8_t tc_pdu[1019];
    uint16_t tc_pdu_len;
    TC_FrameSecurityTrailer_t tc_sec_trailer;
} TC_t;

typedef struct
{
    uint8_t shf : 1;
    uint8_t pusv : 3;
    uint8_t ack : 4;
    uint8_t st : 8;
    uint8_t sst : 8;
    uint8_t sid : 4;
    uint8_t spare : 4;
} ECSS_PUS_t;

typedef struct
{
    uint8_t pvn : 3;
    uint8_t type : 1;
    uint8_t shdr : 1;
    uint16_t appID : 11;
    uint8_t seq : 2;
    uint16_t pktid : 14;
    uint16_t pkt_length : 16;
} CCSDS_SPP_HDR_t;

typedef struct
{
    CCSDS_SPP_HDR_t hdr;
    ECSS_PUS_t pus;
    SDLS_TLV_t tlv_pdu;
} CCSDS_t;

typedef struct
{
    uint8_t cwt : 1;
    uint8_t cvn : 2;
    uint8_t sf : 3;
    uint8_t cie : 2;
    uint8_t vci : 6;
    uint8_t spare0 : 2;
    uint8_t nrfaf : 1;
    uint8_t nblf : 1;
    uint8_t lof : 1;
    uint8_t waitf : 1;
    uint8_t rtf : 1;
    uint8_t fbc : 2;
    uint8_t spare1 : 1;
    uint8_t rv : 8;
} Telemetry_Frame_Ocf_Clcw_t;

typedef struct
{
    uint8_t cwt : 1;
    uint8_t fvn : 3;
    uint8_t af : 1;
    uint8_t bsnf : 1;
    uint8_t bmacf : 1;
    uint8_t bsaf : 1;
    uint16_t lspi : 16;
    uint8_t snval : 8;
} Telemetry_Frame_Ocf_Fsr_t;

typedef struct
{
    uint8_t tfvn : 2;
    uint16_t scid : 10;
    uint8_t vcid : 3;
    uint8_t ocff : 1;
    uint8_t mcfc : 8;
    uint8_t vcfc : 8;
    uint8_t tfsh : 1;
    uint8_t sf : 1;
    uint8_t pof : 1;
    uint8_t slid : 2;
    uint16_t fhp : 11;
} TM_FramePrimaryHeader_t;

typedef struct
{
    uint16_t spi;
    uint8_t iv[16];
    uint8_t iv_field_len;
    uint8_t sn[16];
    uint8_t sn_field_len;
    uint16_t pad;
    uint8_t pad_field_len;
} TM_FrameSecurityHeader_t;

typedef struct
{
    uint8_t mac[64];
    uint8_t mac_field_len;
    uint8_t ocf[4];
    uint8_t ocf_field_len;
    uint16_t fecf;
} TM_FrameSecurityTrailer_t;

typedef struct
{
    TM_FramePrimaryHeader_t tm_header;
    TM_FrameSecurityHeader_t tm_sec_header;
    uint8_t tm_pdu[1786];
    uint16_t tm_pdu_len;
    TM_FrameSecurityTrailer_t tm_sec_trailer;
} TM_t;

typedef struct
{
    uint8_t tfvn : 2;
    uint16_t scid : 8;
    uint8_t vcid : 6;
    long vcfc : 24;
    uint8_t rf : 1;
    uint8_t sf : 1;
    uint8_t spare : 2;
    uint8_t vfcc : 4;
    uint16_t fhecf : 16;
} AOS_FramePrimaryHeader_t;

typedef struct
{
    uint8_t iz[32];
    uint16_t spi;
    uint8_t iv[16];
    uint8_t iv_field_len;
    uint8_t sn[16];
    uint8_t sn_field_len;
    uint16_t pad;
    uint8_t pad_field_len;
} AOS_FrameSecurityHeader_t;

typedef struct
{
    uint8_t mac[64];
    uint8_t mac_field_len;
    uint8_t ocf[4];
    uint8_t ocf_field_len;
    uint16_t fecf;
} AOS_FrameSecurityTrailer_t;

typedef struct
{
    AOS_FramePrimaryHeader_t aos_header;
    AOS_FrameSecurityHeader_t aos_sec_header;
    uint8_t aos_pdu[1786];
    uint16_t aos_pdu_len;
    AOS_FrameSecurityTrailer_t aos_sec_trailer;
} AOS_t;

typedef enum
{
    UNITIALIZED = 0,
    INITIALIZED
} InitStatus;

typedef enum
{
    KEY_TYPE_UNITIALIZED = 0,
    KEY_TYPE_CUSTOM,
    KEY_TYPE_INTERNAL,
    KEY_TYPE_KMC
} KeyType;

typedef enum
{
    MC_TYPE_UNITIALIZED = 0,
    MC_TYPE_CUSTOM,
    MC_TYPE_DISABLED,
    MC_TYPE_INTERNAL
} McType;

typedef enum
{
    SA_TYPE_UNITIALIZED = 0,
    SA_TYPE_CUSTOM,
    SA_TYPE_INMEMORY,
    SA_TYPE_MARIADB
} SadbType;

typedef enum
{
    CRYPTOGRAPHY_TYPE_UNITIALIZED = 0,
    CRYPTOGRAPHY_TYPE_LIBGCRYPT,
    CRYPTOGRAPHY_TYPE_KMCCRYPTO,
    CRYPTOGRAPHY_TYPE_WOLFSSL,
    CRYPTOGRAPHY_TYPE_CUSTOM
} CryptographyType;

typedef enum
{
    IV_INTERNAL,
    IV_CRYPTO_MODULE
} IvType;

typedef enum
{
    TC_NO_FECF,
    TC_HAS_FECF,
    TM_NO_FECF,
    TM_HAS_FECF,
    AOS_NO_FECF,
    AOS_HAS_FECF
} FecfPresent;

typedef enum
{
    CRYPTO_TC_CREATE_FECF_FALSE,
    CRYPTO_TC_CREATE_FECF_TRUE,
    CRYPTO_TM_CREATE_FECF_FALSE,
    CRYPTO_TM_CREATE_FECF_TRUE,
    CRYPTO_AOS_CREATE_FECF_FALSE,
    CRYPTO_AOS_CREATE_FECF_TRUE
} CreateFecfBool;

typedef enum
{
    AOS_FHEC_NA = 0,
    AOS_NO_FHEC,
    AOS_HAS_FHEC
} AosFhecPresent;

typedef enum
{
    AOS_IZ_NA,
    AOS_NO_IZ,
    AOS_HAS_IZ
} AosInsertZonePresent;

typedef enum
{
    TC_CHECK_FECF_FALSE,
    TC_CHECK_FECF_TRUE,
    TM_CHECK_FECF_FALSE,
    TM_CHECK_FECF_TRUE,
    AOS_CHECK_FECF_FALSE,
    AOS_CHECK_FECF_TRUE
} CheckFecfBool;

typedef enum
{
    AOS_NO_OCF,
    AOS_HAS_OCF,
    TC_OCF_NA,
    TM_NO_OCF,
    TM_HAS_OCF
} OcfPresent;

typedef enum
{
    TC_NO_SEGMENT_HDRS,
    TC_HAS_SEGMENT_HDRS,
    TM_SEGMENT_HDRS_NA,
    AOS_SEGMENT_HDRS_NA
} TcSegmentHdrsPresent;

typedef enum
{
    TC_PROCESS_SDLS_PDUS_FALSE,
    TC_PROCESS_SDLS_PDUS_TRUE
} TcProcessSdlsPdus;

typedef enum
{
    TC_NO_PUS_HDR,
    TC_HAS_PUS_HDR
} TcPusHdrPresent;

typedef enum
{
    TC_IGNORE_SA_STATE_FALSE,
    TC_IGNORE_SA_STATE_TRUE
} TcIgnoreSaState;

typedef enum
{
    TC_IGNORE_ANTI_REPLAY_FALSE,
    TC_IGNORE_ANTI_REPLAY_TRUE
} TcIgnoreAntiReplay;

typedef enum
{
    TC_UNIQUE_SA_PER_MAP_ID_FALSE,
    TC_UNIQUE_SA_PER_MAP_ID_TRUE
} TcUniqueSaPerMapId;

typedef enum
{
    SA_INCREMENT_NONTRANSMITTED_IV_FALSE,
    SA_INCREMENT_NONTRANSMITTED_IV_TRUE
} SaIncrementNonTransmittedIvPortion;

typedef enum
{
    TM_NO_SECONDARY_HDR,
    TM_HAS_SECONDARY_HDR
} TmSecondaryHdrPresent;

typedef enum
{
    CAM_ENABLED_FALSE,
    CAM_ENABLED_TRUE
} CamEnabledBool;

typedef enum
{
    CAM_LOGIN_NONE,
    CAM_LOGIN_KERBEROS,
    CAM_LOGIN_KEYTAB_FILE
} CamLoginMethod;

typedef enum
{
    CRYPTO_MAC_NONE,
    CRYPTO_MAC_CMAC_AES256,
    CRYPTO_MAC_HMAC_SHA256,
    CRYPTO_MAC_HMAC_SHA512,
    CRYPTO_ACS_MAX = 3
} AuthCipherSuite;

typedef enum
{
    CRYPTO_CIPHER_NONE,
    CRYPTO_CIPHER_AES256_GCM,
    CRYPTO_CIPHER_AES256_CBC,
    CRYPTO_CIPHER_AES256_CBC_MAC,
    CRYPTO_CIPHER_AES256_CCM,
    CRYPTO_CIPHER_AES256_GCM_SIV
} EncCipherSuite;

typedef struct
{
    InitStatus init_status;
    KeyType key_type;
    McType mc_type;
    SadbType sa_type;
    CryptographyType cryptography_type;
    IvType iv_type;
    CreateFecfBool crypto_create_fecf;
    TcProcessSdlsPdus process_sdls_pdus;
    TcPusHdrPresent has_pus_hdr;
    TcIgnoreSaState ignore_sa_state;
    TcIgnoreAntiReplay ignore_anti_replay;
    TcUniqueSaPerMapId unique_sa_per_mapid;
    CheckFecfBool crypto_check_fecf;
    uint8_t vcid_bitmask;
    uint8_t crypto_increment_nontransmitted_iv;
} CryptoConfig_t;

typedef struct _GvcidManagedParameters_t GvcidManagedParameters_t;
struct _GvcidManagedParameters_t
{
    uint8_t tfvn : 4;
    uint16_t scid : 10;
    uint8_t vcid : 6;
    FecfPresent has_fecf;
    AosFhecPresent aos_has_fhec;
    AosInsertZonePresent aos_has_iz;
    uint16_t aos_iz_len;
    TcSegmentHdrsPresent has_segmentation_hdr;
    uint16_t max_frame_size;
    OcfPresent has_ocf;
    int set_flag;
};

typedef struct
{
    char *mysql_username;
    char *mysql_password;
    char *mysql_hostname;
    char *mysql_database;
    uint16_t mysql_port;
    char *mysql_mtls_cert;
    char *mysql_mtls_key;
    char *mysql_mtls_ca;
    char *mysql_mtls_capath;
    uint8_t mysql_tls_verify_server;
    char *mysql_mtls_client_key_password;
    uint8_t mysql_require_secure_transport;
} SadbMariaDBConfig_t;

typedef struct
{
    char *kmc_crypto_hostname;
    char *protocol;
    uint16_t kmc_crypto_port;
    char *kmc_crypto_app_uri;
    char *mtls_client_cert_path;
    char *mtls_client_cert_type;
    char *mtls_client_key_path;
    char *mtls_client_key_pass;
    char *mtls_ca_bundle;
    char *mtls_ca_path;
    char *mtls_issuer_cert;
    uint8_t ignore_ssl_hostname_validation;
} CryptographyKmcCryptoServiceConfig_t;

typedef struct
{
    uint8_t cam_enabled;
    char *cookie_file_path;
    char *keytab_file_path;
    char *access_manager_uri;
    char *username;
    char *cam_home;
    uint8_t login_method;
} CamConfig_t;

extern int32_t sdls_config_cryptolib(uint8_t sadb_type, uint8_t cryptography_type, uint8_t crypto_create_fecf, uint8_t process_sdls_pdus, uint8_t has_pus_hdr, uint8_t ignore_sa_state, uint8_t ignore_anti_replay, uint8_t unique_sa_per_mapid, uint8_t crypto_check_fecf, uint8_t vcid_bitmask, uint8_t crypto_increment_nontransmitted_iv);

extern int32_t sdls_config_mariadb(char* mysql_hostname, char* mysql_database, uint16_t mysql_port,
                                   uint8_t mysql_require_secure_transport, uint8_t mysql_tls_verify_server,
                                   char* mysql_tls_ca, char* mysql_tls_capath, char* mysql_mtls_cert,
                                   char* mysql_mtls_key,
                                   char* mysql_mtls_client_key_password, char* mysql_username, char* mysql_password);

extern int32_t sdls_config_add_gvcid_managed_parameter(uint8_t tfvn, uint16_t scid, uint8_t vcid, uint8_t has_fecf, uint8_t has_segmentation_hdr, uint16_t max_tc_frame_size);

extern int32_t sdls_config_kmc_crypto_service(char* protocol, char* kmc_crypto_hostname, uint16_t kmc_crypto_port,
                                              char* kmc_crypto_app, char* kmc_tls_ca_bundle, char* kmc_tls_ca_path,
                                              uint8_t kmc_ignore_ssl_hostname_validation, char* mtls_client_cert_path,
                                              char* mtls_client_cert_type, char* mtls_client_key_path,
                                              char* mtls_client_key_pass, char* mtls_issuer_cert);

extern int32_t sdls_config_cam(uint8_t cam_enabled, char* cookie_file_path, char* keytab_file_path, uint8_t login_method, char* access_manager_uri, char* username, char* cam_home);

extern int32_t sdls_init(void);

extern int32_t sdls_init_with_configs(CryptoConfig_t* crypto_config_p,GvcidManagedParameters_t* gvcid_managed_parameters_p,SadbMariaDBConfig_t* sadb_mariadb_config_p, CryptographyKmcCryptoServiceConfig_t *cryptography_kmc_crypto_config_p);

extern int32_t sdls_init_unit_test(void);

extern int32_t sdls_shutdown(void);

extern int32_t apply_security_tc (const uint8_t* p_in_frame, const uint16_t in_frame_length, uint8_t **pp_enc_frame, uint16_t *p_enc_frame_len);

extern int32_t process_security_tc (char* sdls_transfer_frame, int* length, TC_t* tc_sdls_processed_frame);

extern int32_t apply_security_tc_cam (const uint8_t* p_in_frame, const uint16_t in_frame_length, uint8_t **pp_enc_frame, uint16_t *p_enc_frame_len,char* cam_cookies);

extern int32_t process_security_tc_cam (char* sdls_transfer_frame, int* length, TC_t* tc_sdls_processed_frame, char* cam_cookies);

extern int32_t apply_security_tm (uint8_t* p_in_frame, uint16_t in_frame_length);

extern int32_t process_security_tm (uint8_t *ptBuffer, uint16_t length, TM_t* p_enc_frame, uint16_t *p_enc_frame_len);

extern int32_t apply_security_aos (uint8_t* p_in_frame, uint16_t in_frame_length);

extern int32_t process_security_aos (uint8_t *ptBuffer, uint16_t length, AOS_t* p_enc_frame, uint16_t *p_enc_frame_len);

extern char* sdls_get_error_code_enum_string(int32_t crypto_error_code);

