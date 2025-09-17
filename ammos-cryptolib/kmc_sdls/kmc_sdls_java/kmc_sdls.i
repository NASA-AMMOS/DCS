%module kmc_sdls
//%module gov.nasa.jpl.ammos.asec.kmc.kmc_sdls

#define __attribute__(x)
%include stdint.i
%include crypto_config_structs.h
%include crypto_structs.h
%import "crypto_structs.h"
%import "crypto_config_structs.h"

// Need typemaps for char* and uint8_t* http://www.swig.org/Doc3.0/SWIGDocumentation.html#Java_typemaps
// http://www.swig.org/Doc3.0/SWIGDocumentation.html#Typemaps

%inline %{

    #include "kmc_sdls.h"
    #include "crypto_structs.h"
    #include "crypto_config_structs.h"
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
    extern int32_t process_security_aos (uint8_t *ptBuffer, uint16_t length, AOS_t *p_enc_frame, uint16_t *p_enc_frame_len);

    extern char* sdls_get_error_code_enum_string(int32_t crypto_error_code);


    //******************************************************************************************************************
    // Wrapper Support Function for SWIG limitations
    int32_t process_security_tc_uint8t (uint8_t* sdls_transfer_frame, int* length, TC_t* tc_sdls_processed_frame, char* cam_cookies){
        char* tmp_bytearray = malloc(*length * sizeof(char));
        for(int i = 0; i < *length; i++)
        {
            tmp_bytearray[i] = (char)sdls_transfer_frame[i];
        }
        int32_t status = process_security_tc_cam((char*)tmp_bytearray,length,tc_sdls_processed_frame,cam_cookies);
        return status;
    }

    //******************************************************************************************************************
    // Glue functions to handle conversions between non-primitive C and Java types

    uint8_t* hexstring_to_bytearray(char* hex_string,int str_len){
        uint8_t* byte_array = calloc(1,str_len/2 + 1);

        char* line = hex_string;
        char* data = line;
        int offset;
        unsigned int read_byte;
        uint32_t data_len = 0;

        while (sscanf(data, " %02x%n", &read_byte, &offset) == 1)
        {
            byte_array[data_len++] = read_byte;
            data += offset;
        }
        return byte_array;
    }

    char* bytearray_to_hexstring(uint8_t* src_byte_array, int buffer_length){
        if (buffer_length == 0)
        { // Return empty string (with null char!) if buffer is empty
            return "";
        }

        unsigned char* bytes = src_byte_array;
        char* hexstr = malloc(buffer_length * 2 + 1);

        if (src_byte_array == NULL)
            return NULL;

        for (size_t i = 0; i < buffer_length; i++)
        {
            uint8_t nib1 = (bytes[i] >> 4) & 0x0F;
            uint8_t nib2 = (bytes[i]) & 0x0F;
            hexstr[i * 2 + 0] = nib1 < 0xA ? '0' + nib1 : 'A' + nib1 - 0xA;
            hexstr[i * 2 + 1] = nib2 < 0xA ? '0' + nib2 : 'A' + nib2 - 0xA;
        }
        hexstr[buffer_length * 2] = '\0';
        return hexstr;
    }

    char* bytearray_uchar_to_hexstring(unsigned char* src_byte_array, int buffer_length){
        return bytearray_to_hexstring((uint8_t *)src_byte_array,buffer_length);
    }

    char* bytearray_pp_to_hexstring(uint8_t** src_byte_array, int buffer_length){
        return bytearray_to_hexstring(*src_byte_array,buffer_length);
    }


    int get_tfvn(TC_t* tc){
        return tc->tc_header.tfvn;
    }
    int get_bypass(TC_t* tc){
        return tc->tc_header.bypass;
    }
    int get_cc(TC_t* tc){
        return tc->tc_header.cc;
    }
    int get_spare(TC_t* tc){
        return tc->tc_header.spare;
    }
    int get_scid(TC_t* tc){
        return tc->tc_header.scid;
    }
    int get_vcid(TC_t* tc){
        return tc->tc_header.vcid;
    }
    int get_fl(TC_t* tc){
        return tc->tc_header.fl;
    }
    int get_fsn(TC_t* tc){
        return tc->tc_header.fsn;
    }
    int get_sh(TC_t* tc){
        return tc->tc_sec_header.sh;
    }
    int get_spi(TC_t* tc){
        return tc->tc_sec_header.spi;
    }
    int get_iv_field_len(TC_t* tc){
        return tc->tc_sec_header.iv_field_len;
    }
    int get_sn_field_len(TC_t* tc){
        return tc->tc_sec_header.sn_field_len;
    }
    int get_pad_field_len(TC_t* tc){
        return tc->tc_sec_header.pad_field_len;
    }
    int get_tc_pdu_len(TC_t* tc){
        return (int)tc->tc_pdu_len;
    }
    int get_mac_field_len(TC_t* tc){
        return tc->tc_sec_trailer.mac_field_len;
    }
    int get_fecf(TC_t* tc){
        return tc->tc_sec_trailer.fecf;
    }

    // Accessing the byte arrays
    uint8_t* get_iv_ptr(TC_t* tc){
        if(tc->tc_sec_header.iv != NULL)
            return tc->tc_sec_header.iv;
        else
            return NULL;
    }
    uint8_t* get_sn_ptr(TC_t* tc){
        if(tc->tc_sec_header.sn != NULL)
            return tc->tc_sec_header.sn;
        else
            return NULL;
    }
    uint8_t* get_pad_ptr(TC_t* tc){
        if(tc->tc_sec_header.pad != NULL)
            return tc->tc_sec_header.pad;
        else
            return NULL;
    }
    uint8_t* get_pdu_ptr(TC_t* tc){
        if(tc->tc_pdu != NULL)
            return tc->tc_pdu;
        else
            return NULL;
    }
    uint8_t* get_mac_ptr(TC_t* tc){
        if(tc->tc_sec_trailer.mac != NULL)
            return tc->tc_sec_trailer.mac;
        else
            return NULL;
    }

    uint8_t** create_bytearray_pp(){
        uint8_t** uint8_t_ptrptr = calloc(1,sizeof(uint8_t*));
        return uint8_t_ptrptr;
    }

    uint16_t* create_uint16t_ptr(int val){
        uint16_t* ptr = calloc(1,sizeof(uint16_t));
        *ptr = val;
        return ptr;
    }
    int* create_int_ptr(int val){
        int* ptr = calloc(1,sizeof(int));
        *ptr = val;
        return ptr;
    }

    uint16_t deref_uint16t_ptr(uint16_t* ptr){
        return *ptr;
    }

%};

//%pragma(java) jniclasspackage="gov.nasa.jpl.ammos.asec.kmc"