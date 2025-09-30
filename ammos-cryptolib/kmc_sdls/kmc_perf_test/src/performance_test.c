/* Copyright (C) 2009 - 2022 National Aeronautics and Space Administration.
   All Foreign Rights are Reserved to the U.S. Government.

   This software is provided "as is" without any warranty of any kind, either expressed, implied, or statutory,
   including, but not limited to, any warranty that the software will conform to specifications, any implied warranties
   of merchantability, fitness for a particular purpose, and freedom from infringement, and any warranty that the
   documentation will conform to the program, or any warranty that the software will be error free.

   In no event shall NASA be liable for any damages, including, but not limited to direct, indirect, special or
   consequential damages, arising out of, resulting from, or in any way connected with the software or its
   documentation, whether or not based upon warranty, contract, tort or otherwise, and whether or not loss was sustained
   from, or arose out of the results of, or use of, the software, documentation or services provided hereunder.

   ITC Team
   NASA IV&V
   jstar-development-team@mail.nasa.gov
*/

#include "crypto.h"
#include "crypto_error.h"
#include "sa_interface.h"

#include "crypto.h"
#include "shared_util.h"
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>

#include <time.h>
#include <unistd.h>

#include <float.h>

#include <kmc_sdls.h>

// CryptoLib Configuration Parameters
static uint8_t sadb_type = SA_TYPE_MARIADB;
static uint8_t cryptography_type = CRYPTOGRAPHY_TYPE_KMCCRYPTO;
static uint8_t create_fecf = 0;
static uint8_t process_sdls_pdus = 0;
static uint8_t has_pus_hdr = 0;
static uint8_t ignore_sa_state = 0;
static uint8_t ignore_anti_replay = 0;
static uint8_t unique_sa_per_mapid = 0;
static uint8_t check_fecf = 0;
static uint8_t vcid_bitmask = 0x3F;
static uint8_t increment_nontransmitted_iv = 0;

// MariaDB Configuration Parameters
static char *mysql_hostname = NULL;
static char *mysql_database = "sadb";
static uint16_t mysql_port = 3306;
static uint8_t mysql_require_secure_transport = 1;
static uint8_t mysql_tls_verify_server = 1;
static char *mysql_tls_ca = "/etc/pki/tls/certs/ammos-ca-bundle.crt";
static char *mysql_tls_ca_path = NULL;
static char *mysql_mtls_cert = "/ammos/kmc-test/tls/ammos-client-cert.pem"; // CLIENT KEYS
static char *mysql_mtls_key = "/ammos/kmc-test/tls/ammos-client-key.pem";
static char *mysql_mtls_client_key_password = NULL;
static char *mysql_username = NULL;
static char *mysql_password = NULL;

// KMC Crypto Service Configuration Parameters
static char *protocol = "https";
static char *kmc_crypto_hostname = NULL;
static uint16_t kmc_crypto_port = 8443;
static char *kmc_crypto_app = "crypto-service";
static char *kmc_tls_ca_bundle = "/etc/pki/tls/certs/ammos-ca-bundle.crt";
static char *kmc_tls_ca_path = NULL;
static uint8_t kmc_ignore_ssl_hostname_validation = 0;
static char *mtls_client_cert_path = "/ammos/kmc-test/tls/ammos-client-cert.pem"; // CLIENT KEYS
static char *mtls_client_cert_type = "PEM";
static char *mtls_client_key_path = "/ammos/kmc-test/tls/ammos-client-key.pem";
static char *mtls_client_key_pass = NULL;
static char *mtls_issuer_cert = NULL;

// GVCID Managed Parameters
static uint8_t tfvn = 0;
static uint16_t scid = 0x002C;
static uint8_t vcid = 1;
static uint8_t has_fecf = 0; // Be sure your test frame does not include an FECF
static uint8_t has_segmentation_hdr = 0;
static uint16_t max_tc_frame_size = 1024; // CAUTION:  No error checking, you can set this incorrectly

// CAM Parameters
static uint8_t cam_enabled = 0;
static char *cam_cookie_path = NULL;
static char *cam_keytab_path = NULL;
static uint8_t cam_login_method = 0;
static char *cam_manager_uri = NULL;
static char *cam_username = NULL;
static char *cam_home = NULL;

static char *frame = NULL;
static int frame_len = 0;
char *frame_b = NULL;

static float total_time = 0.0; // TODO:  Static?

// Flag Values - Used to set true values (get opt does not play well with uint8/16)
static int sadb_type_flag = SA_TYPE_MARIADB;
static int cryptography_type_flag = CRYPTOGRAPHY_TYPE_KMCCRYPTO;
static int create_fecf_flag = 0;
static int process_sdls_pdus_flag = 0;
static int has_pus_hdr_flag = 0;
static int ignore_sa_state_flag = 0;
static int ignore_anti_replay_flag = 1;
static int unique_sa_per_mapid_flag = 0;
static int check_fecf_flag = 0;
static int increment_nontransmitted_iv_flag = 1;
static int mysql_require_secure_transport_flag = 1;
static int mysql_tls_verify_server_flag = 1;
static int kmc_ignore_ssl_hostname_validation_flag = 0;

static int has_fecf_flag = 1;
static int has_segmentation_hdr_flag = 0;

static int num_loops = 1000;
static int apply_or_process = 0;

static int cam_flag = 0;

void test_information();

// Function to loop over Apply Security Functionality
double Apply_Security_Loop(uint8_t *frame, int frame_length, uint8_t *enc_frame, uint16_t *enc_frame_len, int num_loops, double *time_max, double *time_min)
{
    struct timespec begin, end;
    double total_time;
    total_time = 0.0;

    frame = frame;
    frame_length = frame_length;
    enc_frame_len = enc_frame_len;

    int32_t status = CRYPTO_LIB_SUCCESS;

    for (int i = 0; i < num_loops; i++)
    {
        clock_gettime(CLOCK_REALTIME, &begin);
        status = Crypto_TC_ApplySecurity(frame, frame_length, &enc_frame, enc_frame_len);
        clock_gettime(CLOCK_REALTIME, &end);
        free(enc_frame);

        long seconds = end.tv_sec - begin.tv_sec;
        long nanoseconds = end.tv_nsec - begin.tv_nsec;
        double elapsed = seconds + nanoseconds * 1e-9;

        if (status != CRYPTO_LIB_SUCCESS)
        {
            total_time = -1.0;
            printf("ERROR: %d\n", status);
            break;
        }
        if (elapsed > *time_max)
            *time_max = elapsed;
        if (*time_min > elapsed)
            *time_min = elapsed;

        total_time += elapsed;
    }
    return total_time;
}

void Process_Security_Loop(char *data_b, int *data_l, int num_loops)
{
    struct timespec begin, end;
    double total_time;
    double time_max = 0.0;
    double time_min = DBL_MAX;
    total_time = 0.0;

    int32_t status = CRYPTO_LIB_SUCCESS;

    for (int i = 0; i < num_loops; i++)
    {
        TC_t *processed_frame;
        processed_frame = calloc(1, sizeof(uint8_t) * TC_SIZE);
        // printf("LOOP NUMBER: %d\n", i+1);
        clock_gettime(CLOCK_REALTIME, &begin);
        status = Crypto_TC_ProcessSecurity((uint8_t *)data_b, data_l, processed_frame);
        clock_gettime(CLOCK_REALTIME, &end);

        long seconds = end.tv_sec - begin.tv_sec;
        long nanoseconds = end.tv_nsec - begin.tv_nsec;
        double elapsed = seconds + nanoseconds * 1e-9;

        if (status != CRYPTO_LIB_SUCCESS)
        {
            total_time = -1.0;
            printf("ERROR: %d\n", status);
            break;
        }

        if (elapsed > time_max)
            time_max = elapsed;
        if (time_min > elapsed)
            time_min = elapsed;

        total_time += elapsed;
        if (i == (num_loops - 1))
        {
            printf("\nPerformance Test Complete:\n");
            test_information();
            printf("\nPERFORMANCE DATA:\n");
            printf("TC Method: %s\n", (apply_or_process == 0) ? "TC_APPLY" : "TC_PROCESS");
            printf("\tNumber of Frames Sent: %d\n", num_loops);
            printf("\t\tEncrypted Bytes Per Frame: %d\n", processed_frame->tc_pdu_len);
            printf("\t\tTotal Time: %f\n", total_time);
            printf("\tMin Kbps: %f\n", (((processed_frame->tc_pdu_len * 8) / (time_max)) / 1024));
            printf("\tAvg Kbps: %f\n", (((processed_frame->tc_pdu_len * 8 * num_loops) / total_time) / 1024));
            printf("\tMax Kbps: %f\n", (((processed_frame->tc_pdu_len * 8) / (time_min)) / 1024));
            printf("\n");
        }

        free(processed_frame);
    }
}

void help_message()
{
    printf("Options:\n");
    printf("CRYPTO CONFIG FLAGS:\n");
    printf("--mariadb | (DEFAULT) Flags Configuration to use MariaDB SAs\n");
    printf("--inmemory | Flags Configuration to use InMemory SAs\n");
    printf("--kmccrypto | (DEFAULT) Flags Configuration to use KMCCRYPTO Service\n");
    printf("--libgcrypt | Flags Configuration to use LIBGCRYPT\n");
    printf("--fecf_yes | Flags Configuration to CREATE FECF");
    printf("--fecf_no | Flags Configuration to NOT Create FECF\n");
    printf("--sdls_pdu_yes | Flags Configuration to Process SDLS PDUs\n");
    printf("--sdls_pdu_no | Flags Configuration to NOT Process SDLS PDUs\n");
    printf("--has_pus_hdr_yes | Alerts Configuration that frame HAS PUS HEADER\n");
    printf("--has_pus_hdr_no | Alerts Configuration that fame DOES NOT Have PUS HEADER\n");
    printf("--ignore_sa_state_yes | Flags Configuration to IGNORE SA STATE\n");
    printf("--ignore_sa_state_no | Flags Configuration to USE SA STATE\n");
    printf("--ignore_anti_replay_yes | Configures System to IGNORE ANTIREPLAY\n");
    printf("--ignore_anti_replay_no | Configures System to NOT Ignore Antireplay\n");
    printf("--unique_sa_yes | Configures the system to have unique SAs per MAPID\n");
    printf("--unique_sa_no | Configures the system to NOT have unique SAs per MAPID\n");
    printf("--check_fecf_yes | Configures the system to validate the FECF\n");
    printf("--check_fecf_no | Configures the system to NOT validate the FECF\n");
    printf("--increment_nt_iv_yes | Configures the system to increment the non-transmitted portion of the IV\n");
    printf("--{increment_nt_iv_no | Configures the system to NOT increment the non-transmitted portion of the IV\n");
    printf("\nCRYPTO CONFIG ARGS:\n");
    printf("--vcid_bitmask | Sets VCID Bitmask - Requires an argument\n");

    printf("\nMARIA DB CONFIG FLAGS:\n");
    printf("--req_sec_trans_yes | Configures the system to REQUIRE secure transport within MariaDB\n");
    printf("--req_sec_trans_no | Configures the system to NOT Require secure transport within MariaDB\n");
    printf("--verify_tls_yes | Configures the system to verify the TLS Server within MariaDB\n");
    printf("--verify_tls_no | Configures the system to NOT verify the TLS Server within MariaDB\n");
    printf("\nMARIADB CONFIG ARGS:\n");
    printf("--sql_host | Sets the MariaDB Host URL\n");
    printf("--sql_db | Sets the MariaDB Database Name\n");
    printf("--sql_port | Sets the MariaDB access Port\n");
    printf("--sql_tls_ca | Sets the MariaDB TLS Certificate Authority Bundle Location\n");
    printf("--sql_tls_ca_path | Sets the MariaDB Certificate Authority Path\n");
    printf("--sql_mtls_cert | Sets the MariaDB MTLS Certificate Location\n");
    printf("--sql_mtls_key | Sets the MariaDB MTLS Certificate Key Location\n");
    printf("--sql_mtls_client_key_password | Sets the Client Certificate Key Password\n");
    printf("--sql_username | Sets the MariaDB Username\n");
    printf("--sql_password | Sets the MariaDB Password\n");

    printf("\nKMC CONFIG FLAGS:\n");
    printf("--ignore_hostname_validation_yes | Configures the system to IGNORE KMC SSL Hostname Validation\n");
    printf("--ignore_hostname_validation_no | Configures the system to NOT Ignore KMC SSL Hostname Validation\n");
    printf("\nKMC CONFIG ARGS:\n");
    printf("--protocol | Sets the KMC Protocol (https DEFAULT)\n");
    printf("--kmc_crypto_hostname | Sets the KMCCRYPTO Hostname\n");
    printf("--kmc_crypto_port | Sets the KMCCRYPTO Port (8443 DEFAULT)\n");
    printf("--kmc_crypto_app | Sets the KMCCRYPTO App (crypto-service DEFAULT)\n");
    printf("--kmc_tls_ca_bundle | Sets the KMCCRYPTO TLS Certificate Authority Bundle Location\n");
    printf("--kmc_tls_ca_path | Sets the KMC TLS Certificate Authority Path\n");
    printf("--mtls_client_cert_path | Sets the KMC MTLS Client Certificate Path\n");
    printf("--mtls_client_cert_type | Sets the KMC MTLS Client Certificate Type (PEM DEFAULT)\n");
    printf("--mtls_client_key_path | Sets the KMC MTLS Client Certificate Key Path\n");
    printf("--mtls_client_key_pass | Sets the KMC MTLS Client Certificate Key Password\n");
    printf("--mtls_issuer_cert | Sets the KMC MTLS Client Certificate Issuer\n");

    printf("\nGVCID CONFIG FLAGS:\n");
    printf("--has_fecf_yes | Alerts Configuration that frames will HAVE FECF\n");
    printf("--has_fecf_no | Alerts Configuration that frames will NOT Have FECF\n");
    printf("--segmentation_header_yes | Alerts Configuration that frames will HAVE Segmentation Header\n");
    printf("--segmentation_header_no | Alerts Configuration that frames will NOT have Segmentation Header\n");
    printf("\nGVCID CONFIG ARGS:\n");
    printf("--tfvn | Sets the GVCID TFVN (0 DEFAULT)\n");
    printf("--scid | Sets the GVCID SCID (0x002C (44) DEFAULT)\n");
    printf("--vcid | Sets the GVCID VCID\n");
    printf("--max_tc_frame_size | Sets the GVCID Max TC Frame Size (1024 Max)\n");

    printf("\nCAM CONFIG FLAGS:\n");
    printf("--cam_enabled | Alerts configuration that frames WILL utilize CAM.  If this is not set all other configurations will be ignored\n");
    printf("--cam_disabled | Alerts configuration that frames will NOT utilize CMA\n");
    printf("\nCAM CONFIG ARGS:\n");
    printf("--cam_cookie_path | Sets the CAM Cookie path string\n");
    printf("--cam_keytab_path | Sets the CAM Keytab path string\n");
    printf("--cam_login_method | Sets the CAM login uint8\n");
    printf("--cam_manager_uri | Sets the CAM Manager URI\n");
    printf("--cam_username | Sets the CAM username\n");
    printf("--cam_home | Sets the CAM Home string\n");

    printf("\nOTHER:\n");
    printf("--help | Prints this man page\n");
    printf("--numloops | Sets the number of loops in the performance test\n");
    printf("--tc_apply | (DEFAULT) Sets the testing to use TC_APPLY\n");
    printf("--tc_process | Sets testing to use TC_PROCESS\n");

    printf("\nExample Command:\n\nperformance_test --frame \"202C0C6100ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEFF01C\" --sql_host \"atb-ocio-12a.jpl.nasa.gov\" --sql_username \"mcstest\" --sql_tls_ca \"/home/robbrown/jpl_certs/ammos-ca-bundle.crt\" --sql_mtls_cert \"/home/robbrown/jpl_certs/mcstest.crt\" --sql_mtls_key \"/home/robbrown/jpl_certs/mcstest.key\" --kmc_crypto_hostname \"asec-cmdenc-srv1.jpl.nasa.gov\"  --vcid 3 --tc_apply\n");

    printf("\n\nREQUIRED PARAMETERS:\n");
    printf("--frame | Sets the perforance frame to be encrypted - REQUIRED\n");
}

// Function to print out final test setup information upon completion
void test_information()
{
    printf("CRYPTO CONFIG:\n");
    printf(KGRN "\tSADB TYPE:" RESET);
    printf(" %s", (sadb_type == SA_TYPE_MARIADB ? "MARIADB\n" : "INMEMORY\n"));
    printf(KGRN "\tCRYPTOGRAPHY TYPE:" RESET);
    printf(" %s", (cryptography_type == CRYPTOGRAPHY_TYPE_KMCCRYPTO ? "KMCCRYPTO\n" : "LIBGCRYPT\n"));
    printf(KGRN "\tTC Create FECF:" RESET);
    printf(" %s", (create_fecf == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tPROCESS SDLS PDUs:" RESET);
    printf(" %s", (process_sdls_pdus == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tTC HAS PUS HDR:" RESET);
    printf(" %s", (has_pus_hdr == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tTC IGNORE SA STATE:" RESET);
    printf(" %s", (ignore_sa_state == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tTC IGNORE ANTI-REPLAY:" RESET);
    printf(" %s", (ignore_anti_replay == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tTC UNIQUE SA PER MAPID:" RESET);
    printf(" %s", (unique_sa_per_mapid == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tTC CHECK FECF:" RESET);
    printf(" %s", (check_fecf == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tVCID BITMASK:" RESET);
    printf(" 0x%02x\n", vcid_bitmask);
    printf(KGRN "\tINCREMENT NONTRANSMITTED IV:" RESET);
    printf(" %s", (increment_nontransmitted_iv == 1 ? "YES\n" : "NO\n"));

    printf("\nMARIADB CONFIG:\n");
    printf(KGRN "\tMYSQL DATABASE NAME:" RESET);
    printf(" %s\n", mysql_database);
    printf(KGRN "\tMYSQL PORT:" RESET);
    printf(" %d\n", mysql_port);
    printf(KGRN "\tMYSQL Require Secure Transport:" RESET);
    printf(" %s", (mysql_require_secure_transport == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tMYSQL VERIFY TLS SERVER:" RESET);
    printf(" %s", (mysql_tls_verify_server == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tMYSQL TLS CA:" RESET);
    printf(" %s\n", mysql_tls_ca);
    printf(KGRN "\tMYSQL TLS CA PATH:" RESET);
    printf(" %s\n", mysql_tls_ca_path);
    printf(KGRN "\tMYSQL HOSTNAME:" RESET);
    printf(" %s\n", mysql_hostname);
    printf(KGRN "\tMYSQL MTLS CLIENT CERTIFICATE:" RESET);
    printf(" %s\n", mysql_mtls_cert);
    printf(KGRN "\tMYSQL MTLS CLIENT CERTIFICATE KEY:" RESET);
    printf(" %s\n", mysql_mtls_key);
    printf(KGRN "\tMYSQL MTLS CLIENT KEY PASSWORD:" RESET);
    printf(" %s\n", mysql_mtls_client_key_password);
    printf(KGRN "\tMYSQL USERNAME:" RESET);
    printf(" %s\n", mysql_username);
    printf(KGRN "\tMYSQL PASSWORD:" RESET);
    printf(" %s\n", mysql_password);

    printf("\nKMC CONFIG:\n");
    printf(KGRN "\tKMC PROTOCOL:" RESET);
    printf(" %s\n", protocol);
    printf(KGRN "\tKMC CRYPTO HOSTNAME:" RESET);
    printf(" %s\n", kmc_crypto_hostname);
    printf(KGRN "\tKMC CRYPTO PORT:" RESET);
    printf(" %d\n", kmc_crypto_port);
    printf(KGRN "\tKMC CRYPTO APP:" RESET);
    printf(" %s\n", kmc_crypto_app);
    printf(KGRN "\tKMC TLS CA BUNDLE:" RESET);
    printf(" %s\n", kmc_tls_ca_bundle);
    printf(KGRN "\tKMC TLS CA PATH:" RESET);
    printf(" %s\n", kmc_tls_ca_path);
    printf(KGRN "\tKMC IGNORE SSL HOSTNAME VALIDATION:" RESET);
    printf(" %s", (kmc_ignore_ssl_hostname_validation == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tKMC MTLS CLIENT CERT PATH:" RESET);
    printf(" %s\n", mtls_client_cert_path);
    printf(KGRN "\tKMC MTLS CLIENT CERT TYPE:" RESET);
    printf(" %s\n", mtls_client_cert_type);
    printf(KGRN "\tKMC MTLS CLIENT KEY PATH:" RESET);
    printf(" %s\n", mtls_client_key_path);
    printf(KGRN "\tKMC MTLS CLIENT KEY PASSWORD:" RESET);
    printf(" %s\n", mysql_mtls_client_key_password);
    printf(KGRN "\tKMC MTLS ISSUER CERTIFICATE:" RESET);
    printf(" %s\n", mtls_issuer_cert);

    printf("\nCAM CONFIG:\n");
    if (cam_enabled)
    {
        printf(KGRN "\tCAM COOKIE PATH:" RESET);
        printf(" %s\n", cam_cookie_path);
        printf(KGRN "\tCAM KEYTAB PATH:" RESET);
        printf(" %s\n", cam_keytab_path);
        printf(KGRN "\tCAM LOGIN METHOD:" RESET);
        printf(" %s\n", cam_login_method);
        printf(KGRN "\tCAM MANAGER URI:" RESET);
        printf(" %s\n", cam_manager_uri);
        printf(KGRN "\tCAM USERNAME:" RESET);
        printf(" %s\n", cam_username);
        printf(KGRN "\tCAM HOME:" RESET);
        printf(" %s\n", cam_home);
    }
    else
    {
        printf(KRED "\tCAM DISABLED\n" RESET);
    }

    printf("\nGVCID CONFIG:\n");
    printf(KGRN "\tGVCID TFVN:" RESET);
    printf(" %d\n", tfvn);
    printf(KGRN "\tGVCID SCID:" RESET);
    printf(" 0x%04x\n", scid);
    printf(KGRN "\tGVCID VCID:" RESET);
    printf(" %d\n", vcid);
    printf(KGRN "\tGVCID HAS FECF:" RESET);
    printf(" %s", (has_fecf == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tGVCID HAS SEGMENTATION HEADER:" RESET);
    printf(" %s", (has_segmentation_hdr == 1 ? "YES\n" : "NO\n"));
    printf(KGRN "\tGVCID MAX TC FRAME SIZE:" RESET);
    printf(" %d\n", max_tc_frame_size);

    printf(KRED "\nFRAME:\n" RESET);
    printf(" %s\n\n", frame);
}

int main(int argc, char **argv)
{
    int option_index = 0;
    int c;

    while (1)
    {
        static struct option long_options[] =
            {
                // Flag Setting Options (Cryptolib Config)
                {"mariadb", no_argument, &sadb_type_flag, SA_TYPE_MARIADB},
                {"inmemory", no_argument, &sadb_type_flag, SA_TYPE_INMEMORY},

                {"kmccrypto", no_argument, &cryptography_type_flag, CRYPTOGRAPHY_TYPE_KMCCRYPTO},
                {"libgcrypt", no_argument, &cryptography_type_flag, CRYPTOGRAPHY_TYPE_LIBGCRYPT},

                {"fecf_yes", no_argument, &create_fecf_flag, 1},
                {"fecf_no", no_argument, &create_fecf_flag, 0},

                {"sdls_pdu_yes", no_argument, &process_sdls_pdus_flag, 1},
                {"sdls_pdu_no", no_argument, &process_sdls_pdus_flag, 0},

                {"has_pus_hdr_yes", no_argument, &has_pus_hdr_flag, 1},
                {"has_pus_hdr_no", no_argument, &has_pus_hdr_flag, 0},

                {"ignore_sa_state_yes", no_argument, &ignore_sa_state_flag, 1},
                {"ignore_sa_state_no", no_argument, &ignore_sa_state_flag, 0},

                {"ignore_anti_replay_yes", no_argument, &ignore_anti_replay_flag, 1},
                {"ignore_anti_replay_no", no_argument, &ignore_anti_replay_flag, 0},

                {"unique_sa_yes", no_argument, &unique_sa_per_mapid_flag, 1},
                {"unique_sa_no", no_argument, &unique_sa_per_mapid_flag, 0},

                {"check_fecf_yes", no_argument, &check_fecf_flag, 1},
                {"check_fecf_no", no_argument, &check_fecf_flag, 0},

                {"increment_nt_iv_yes", no_argument, &increment_nontransmitted_iv_flag, 1},
                {"increment_nt_iv_no", no_argument, &increment_nontransmitted_iv_flag, 0},

                // Flag Setting Options (MariaDB Config)
                {"req_sec_trans_yes", no_argument, &mysql_require_secure_transport_flag, 1},
                {"req_sec_trans_no", no_argument, &mysql_require_secure_transport_flag, 0},

                {"verify_tls_yes", no_argument, &mysql_tls_verify_server_flag, 1},
                {"verify_tls_no", no_argument, &mysql_tls_verify_server_flag, 0},

                // Flag Setting Options (KMC Config)
                {"ignore_hostname_validation_yes", no_argument, &kmc_ignore_ssl_hostname_validation_flag, 1},
                {"ignore_hostname_validation_no", no_argument, &kmc_ignore_ssl_hostname_validation_flag, 0},

                // Flag Setting Options GVCID
                {"has_fecf_yes", no_argument, &has_fecf_flag, 1},
                {"has_fecf_no", no_argument, &has_fecf_flag, 0},

                {"segmentation_header_yes", no_argument, &has_segmentation_hdr_flag, 1},
                {"segmentation_header_no", no_argument, &has_segmentation_hdr_flag, 0},

                {"tc_apply", no_argument, &apply_or_process, 0},
                {"tc_process", no_argument, &apply_or_process, 1},

                // Flag Settings for CAM
                {"cam_enabled", no_argument, &cam_flag, 1},
                {"cam_disabled", no_argument, &cam_flag, 0},

                {"vcid_bitmask", required_argument, 0, 'a'},
                {"sql_host", required_argument, 0, 'b'},
                {"sql_db", required_argument, 0, 'c'},
                {"sql_port", required_argument, 0, 'd'},
                {"sql_tls_ca", required_argument, 0, 'e'},
                {"sql_tls_ca_path", required_argument, 0, 'f'},
                {"sql_mtls_cert", required_argument, 0, 'g'},
                {"sql_mtls_key", required_argument, 0, 'h'},
                {"sql_mtls_client_key_password", required_argument, 0, 'i'},
                {"sql_username", required_argument, 0, 'j'},
                {"sql_password", required_argument, 0, 'k'},
                {"protocol", required_argument, 0, 'l'},
                {"kmc_crypto_hostname", required_argument, 0, 'm'},
                {"kmc_crypto_port", required_argument, 0, 'n'},
                {"kmc_crypto_app", required_argument, 0, 'o'},
                {"kmc_tls_ca_bundle", required_argument, 0, 'p'},
                {"kmc_tls_ca_path", required_argument, 0, 'q'},
                {"mtls_client_cert_path", required_argument, 0, 'r'},
                {"mtls_client_cert_type", required_argument, 0, 's'},
                {"mtls_client_key_path", required_argument, 0, 't'},
                {"mtls_client_key_pass", required_argument, 0, 'u'},
                {"mtls_issuer_cert", required_argument, 0, 'v'},
                {"tfvn", required_argument, 0, 'w'},
                {"scid", required_argument, 0, 'x'},
                {"vcid", required_argument, 0, 'y'},
                {"max_tc_frame_size", required_argument, 0, 'z'},
                {"frame", required_argument, 0, '1'},
                {"help", no_argument, 0, '2'},
                {"numloops", required_argument, 0, '3'},
                {"cam_cookie_path", required_argument, 0, '4'},
                {"cam_keytab_path", required_argument, 0, '5'},
                {"cam_login_method", required_argument, 0, '6'},
                {"cam_manager_uri", required_argument, 0, '7'},
                {"cam_username", required_argument, 0, '8'},
                {"cam_home", required_argument, 0, '9'},
                {0, 0, 0, 0}

            };

        c = getopt_long(argc, argv, "a:b:c:d:e:f:g:h:i:j:k:l:m:n:o:p:q:r:s:t:u:v:w:x:y:z:1:2:3:4:5:6:7:8:9:", long_options, &option_index);

        if (c == -1)
            break;

        char *temp;

        switch (c)
        {
        case 0:
            if (long_options[option_index].flag != 0)
            {
                // printf("A Flag was set!\n");
            }
            break;
        case 'a': // vcid_bitmask
            vcid_bitmask = (uint8_t)strtol(optarg, NULL, 0);
            printf("vcid_bitmask set: %02x | %s\n", vcid_bitmask, optarg);
            break;
        case 'b': // mysql_hostname
            mysql_hostname = strdup(optarg);
            printf("mysql_hostname set: %s | %s\n", mysql_hostname, optarg);
            break;
        case 'c': // mysql_database
            mysql_database = strdup(optarg);
            printf("mysql_database set: %s | %s\n", mysql_database, optarg);
            break;
        case 'd': // mysql_port
            mysql_port = (uint16_t)strtol(optarg, &temp, 10);
            printf("mysql_port set: %d | %s\n", mysql_port, optarg);
            break;
        case 'e': // mysql_tls_ca
            mysql_tls_ca = strdup(optarg);
            printf("mysql_tls_ca set: %s | %s\n", mysql_tls_ca, optarg);
            break;
        case 'f': // mysql_tls_ca_path
            mysql_tls_ca_path = strdup(optarg);
            printf("mysql_tls_ca_path set: %s | %s\n", mysql_tls_ca_path, optarg);
            break;
        case 'g': // mysql_mtls_cert
            mysql_mtls_cert = strdup(optarg);
            printf("mysql_mtls_cert set: %s | %s\n", mysql_mtls_cert, optarg);
            break;
        case 'h': // mysql_mtls_key
            mysql_mtls_key = strdup(optarg);
            printf("mysql_mtls_key set: %s | %s\n", mysql_mtls_key, optarg);
            break;
        case 'i': // mysql_mtls_client_key_password
            mysql_mtls_client_key_password = strdup(optarg);
            printf("mysql_mtls_client_key_password set: %s | %s\n", mysql_mtls_client_key_password, optarg);
            break;
        case 'j': // mysql_username
            mysql_username = strdup(optarg);
            printf("mysql_username set: %s| %s\n", mysql_username, optarg);
            break;
        case 'k': // mysql_password
            mysql_password = strdup(optarg);
            printf("mysql_password set: %s | %s\n", mysql_password, optarg);
            break;
        case 'l': // protocol
            protocol = strdup(optarg);
            printf("protocol set: %s | %s\n", protocol, optarg);
            break;
        case 'm': // kmc_crypto_hostname
            kmc_crypto_hostname = strdup(optarg);
            printf("kmc_crypto_hostname set: %s | %s\n", kmc_crypto_hostname, optarg);
            break;
        case 'n':                                                  // kmc_crypto_port
            kmc_crypto_port = (uint16_t)strtol(optarg, &temp, 10); // TODO:  Test This
            printf("kmc_crypto_port set: %d | %s\n", kmc_crypto_port, optarg);
            break;
        case 'o': // kmc_crypto_app
            kmc_crypto_app = strdup(optarg);
            printf("kmc_crypto_app set: %s | %s\n", kmc_crypto_app, optarg);
            break;
        case 'p': // kmc_tls_ca_bundle
            kmc_tls_ca_bundle = strdup(optarg);
            printf("kmc_tls_ca_bundle set: %s | %s\n", kmc_tls_ca_bundle, optarg);
            break;
        case 'q': // kmc_tls_ca_path
            kmc_tls_ca_path = strdup(optarg);
            printf("kmc_tls_ca_path set: %s | %s\n", kmc_tls_ca_path, optarg);
            break;
        case 'r': // mtls_client_cert_path
            kmc_tls_ca_path = strdup(optarg);
            printf("kmc_tls_ca_path set: %s | %s\n", kmc_tls_ca_path, optarg);
            break;
        case 's': // mtls_client_cert_type
            mtls_client_cert_type = strdup(optarg);
            printf("mtls_client_cert_type set: %s | %s\n", mtls_client_cert_type, optarg);
            break;
        case 't': // mtls_client_key_path
            mtls_client_key_path = strdup(optarg);
            printf("mtls_client_key_path set: %s | %s\n", mtls_client_key_path, optarg);
            break;
        case 'u': // mtls_client_key_pass
            mtls_client_key_pass = strdup(optarg);
            printf("mtls_client_key_pass set: %s | %s\n", mtls_client_key_pass, optarg);
            break;
        case 'v': // mtls_issuer_cert
            mtls_issuer_cert = strdup(optarg);
            printf("mtls_issuer_cert set: %s | %s\n", mtls_issuer_cert, optarg);
            break;
        case 'w': // tfvn
            tfvn = (uint8_t)strtol(optarg, NULL, 10);
            printf("tfvn set: %02x | %s\n", tfvn, optarg);
            break;
        case 'x':                                      // scid
            scid = (uint16_t)strtol(optarg, &temp, 0); // NOTE:  This requires the string to be in hex format
            printf("scid set: %04x | %s\n", scid, optarg);
            break;
        case 'y': // vcid
            vcid = (uint8_t)strtol(optarg, NULL, 10);
            printf("vcid set: %02x | %s\n", vcid, optarg);
            break;
        case 'z': // max_tc_frame_size
            max_tc_frame_size = (uint16_t)strtol(optarg, &temp, 10);
            printf("max_tc_frame_size set: %d | %s\n", max_tc_frame_size, optarg);
            // TODO:  Error Check?  Can't be greater than 1024
            break;
        case '1': // frame
            frame = strdup(optarg);
            printf("frame set: %s | %s\n", frame, optarg);
            break;
        case '2': // HELP Message
            help_message();
            exit(0);
            break;
        case '3':
            num_loops = (int)atoi(optarg);
            printf("Number of loops changed to: %d\n", num_loops);
            break;
        case '4':
            cam_cookie_path = strdup(optarg);
            printf("CAM cookie path set to: %s\n", cam_cookie_path);
            break;
        case '5':
            cam_keytab_path = strdup(optarg);
            printf("CAM keytab path set to: %s\n", cam_keytab_path);
            break;
        case '6':
            cam_login_method = (uint8_t)strtol(optarg, NULL, 10);
            printf("CAM Login: %d\n", cam_login_method);
            break;
        case '7':
            cam_manager_uri = strdup(optarg);
            printf("CAM Manger URI set to: %s\n", cam_manager_uri);
            break;
        case '8':
            cam_username = strdup(optarg);
            printf("CAM Username set to: %s\n", cam_username);
            break;
        case '9':
            cam_home = strdup(optarg);
            printf("CAM Home set to: %s\n", cam_home);
            break;
        case '?': // error
            printf("INVALID OPTION: %c\n", optopt);
            exit(0);
            break;

        default:
            abort();
        }
    }

    // Handle Flag Arguments if Set
    if (sadb_type_flag)
    {
        printf("System Configured for: MariaDB\n");
        sadb_type = SA_TYPE_MARIADB;
    }
    else
    {
        printf("System Configured for: Internal SA\n");
    }

    if (cryptography_type_flag)
    {
        printf("System Configured for: KMCCRYPTO\n");
        cryptography_type = CRYPTOGRAPHY_TYPE_KMCCRYPTO;
    }
    else
    {
        printf("System Configured for: LIBGCRYPT\n");
    }

    if (create_fecf_flag)
    {
        printf("System Configured for: CREATE FECF\n");
        create_fecf = 1;
    }
    else
    {
        printf("System Configured for: NO - CREATE FECF\n");
    }

    if (process_sdls_pdus_flag)
    {
        printf("System Configured for: PROCESS SDLS PDUS\n");
        process_sdls_pdus = 1;
    }
    else
    {
        printf("System Configured for: NO - PROCESS SDLS PDUS\n");
    }

    if (has_pus_hdr_flag)
    {
        printf("System Configured for: HAS PUS HDR\n");
        has_pus_hdr = 1;
    }
    else
    {
        printf("System Configured for: NO - HAS PUS HDR\n");
    }

    if (ignore_sa_state_flag)
    {
        printf("System Configured for: IGNORE SA STATE\n");
        ignore_sa_state = 1;
    }
    else
    {
        printf("System Configured for: NO - IGNORE SA STATE\n");
    }

    if (ignore_anti_replay_flag)
    {
        printf("System Configured for: IGNORE ANTI REPLAY\n");
        ignore_anti_replay = 1;
    }
    else
    {
        printf("System Configured for: NO - IGNORE ANTI REPLAY\n");
    }

    if (unique_sa_per_mapid_flag)
    {
        printf("System Configured for: UNIQUE SA PER MAPID\n");
        unique_sa_per_mapid = 1;
    }
    else
    {
        printf("System Configured for: NO - UNIQUE SA PER MAPID\n");
    }

    if (check_fecf_flag)
    {
        printf("System Configured for: CHECK FECF\n");
        check_fecf = 1;
    }
    else
    {
        printf("System Configured for: NO - CHECK FECF\n");
    }

    if (increment_nontransmitted_iv_flag)
    {
        printf("System Configured for: INCREMENT NONTRANSMITTED IV\n");
        increment_nontransmitted_iv = 1;
    }
    else
    {
        printf("System Configured for: NO - INCREMENT NONTRANSMITTED IV\n");
    }

    if (mysql_require_secure_transport_flag)
    {
        printf("System Configured for: MYSQL REQUIRE SECURE TRANSPORT\n");
        mysql_require_secure_transport = 1;
    }
    else
    {
        printf("System Configured for: NO - MYSQL REQUIRE SECURE TRANSPORT\n");
    }

    if (mysql_tls_verify_server_flag)
    {
        printf("System Configured for: MYSQL TLS VERIFY SERVER\n");
        mysql_tls_verify_server = 1;
    }
    else
    {
        printf("System Configured for: NO - MYSQL TLS VERIFY SERVER\n");
    }

    if (kmc_ignore_ssl_hostname_validation_flag)
    {
        printf("System Configured for: KMC IGNORE SSL HOSTNAME VALIDATION\n");
        kmc_ignore_ssl_hostname_validation = 1;
    }
    else
    {
        printf("System Configured for: NO - KMC IGNORE SSL HOSTNAME VALIDATION\n");
    }

    if (has_fecf_flag)
    {
        printf("System Configured for: HAS FECF\n");
        has_fecf = 1;
    }
    else
    {
        printf("System Configured for: NO - HAS FECF\n");
    }

    if (has_segmentation_hdr_flag)
    {
        printf("System Configured for: HAS SEGMENTATION HDR\n");
        has_segmentation_hdr = 1;
    }
    else
    {
        printf("System Configured for: NO - HAS SEGMENTATION HDR\n");
    }

    if (cam_flag)
    {
        printf("System Configured for: CAM Enabled\n");
        cam_enabled = 1;
    }
    else
    {
        printf("System Configured for: CAM Disabled\n");
        cam_enabled = 0;
    }

    // Setup & Initialize CryptoLib

    sdls_config_cryptolib(sadb_type, cryptography_type);
    sdls_config_mariadb(mysql_hostname, mysql_database, mysql_port, mysql_require_secure_transport, mysql_tls_verify_server, mysql_tls_ca, mysql_tls_ca_path, mysql_mtls_cert, mysql_mtls_key, mysql_mtls_client_key_password, mysql_username, mysql_password);
    sdls_config_kmc_crypto_service(protocol, kmc_crypto_hostname, kmc_crypto_port, kmc_crypto_app, kmc_tls_ca_bundle, kmc_tls_ca_path, kmc_ignore_ssl_hostname_validation, mtls_client_cert_path, mtls_client_cert_type, mtls_client_key_path, mtls_client_key_pass, mtls_issuer_cert);
    sdls_config_add_gvcid_managed_parameter(tfvn, scid, vcid, has_fecf, has_segmentation_hdr, max_tc_frame_size);

    if (cam_enabled)
    {
        sdls_config_cam(cam_enabled, cam_cookie_path, cam_keytab_path, cam_login_method, cam_manager_uri, cam_username, cam_home);
    }

    sdls_init();

    if (apply_or_process == 0)
    {
        uint8_t *ptr_enc_frame = NULL;
        uint16_t enc_frame_len = 0;

        enc_frame_len = enc_frame_len;
        ptr_enc_frame = ptr_enc_frame;
        frame_b = frame_b;
        frame_len = frame_len;

        double max_time = 0;
        double min_time = DBL_MAX;

        if (frame == NULL)
        {
            help_message();

            printf("\n\nERROR:\nA frame MUST be included using the --frame \"xxx\" command!\n\n\n");

            exit(0);
        }
        hex_conversion(frame, &frame_b, &frame_len);

        printf("\nBeginning Performance test:\n");
        total_time = 0.0;
        total_time = Apply_Security_Loop((uint8_t *)frame_b, frame_len, ptr_enc_frame, &enc_frame_len, num_loops, &max_time, &min_time);

        printf("\nPerformance Test Complete:\n");
        test_information();
        printf("\nPERFORMANCE DATA:\n");
        printf("TC Method: %s\n", (apply_or_process == 0) ? "TC_APPLY" : "TC_PROCESS");
        printf("\tNumber of Frames Sent: %d\n", num_loops);
        printf("\t\tEncrypted Bytes Per Frame: %d\n", enc_frame_len);
        printf("\t\tTotal Time: %f\n", total_time);
        printf("\tMin Kbps: %f\n", (((enc_frame_len * 8) / max_time) / 1024));
        printf("\tAvg Kbps: %f\n", (((enc_frame_len * 8 * num_loops) / total_time) / 1024));
        printf("\tMax Kbps: %f\n", (((enc_frame_len * 8) / min_time) / 1024));
        printf("\n");

        sdls_shutdown();
    }

    else
    {
        char *frame_b = NULL;
        int frame_l = 0;

        // Convert hex to binary
        hex_conversion(frame, &frame_b, &frame_l);

        printf("\nBeginning Performance test:\n");
        Process_Security_Loop(frame_b, &frame_l, num_loops);

        sdls_shutdown();
    }
}
