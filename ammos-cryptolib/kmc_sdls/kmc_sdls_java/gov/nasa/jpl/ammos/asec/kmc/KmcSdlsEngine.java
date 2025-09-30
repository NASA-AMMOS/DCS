package gov.nasa.jpl.ammos.asec.kmc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class KmcSdlsEngine
{
    kmc_sdls kmcCInterface;
    TC_t tc_ptr;
    int KMC_ENGINE_SUCCESS = 0;
    int engineStatus = KMC_ENGINE_SUCCESS;
    public KmcSdlsEngine()
    {
        System.loadLibrary("kmc_sdls_java");
        this.engineStatus = this.KMC_ENGINE_SUCCESS;
        this.kmcCInterface = new kmc_sdls();
    }
    public KmcSdlsEngine(Properties props) throws Exception
    {
        // Call base KmcSdlsEngine constructor!
        this();

        //Configure KmcSdlsEngine via Properties loading function.
        this.configureKmcSdlsEngineFromProperties(props);

        // Init the KmcSdlsEngine after configuration.
        this.init();
    }
    public KmcSdlsEngine(String configPropertiesFilePath) throws Exception
    {
        // Call base KmcSdlsEngine constructor!
        this();

        Properties props = new Properties();

        try (InputStream input = new FileInputStream(configPropertiesFilePath))
        {
            props.load(input);

        }
        catch (IOException ex)
        {
            throw ex;
        }
        //Configure KmcSdlsEngine via Properties loading function.
        this.configureKmcSdlsEngineFromProperties(props);

        // Init the KmcSdlsEngine after configuration.
        this.init();
    }
    public int getEngineStatus()
    {
        return this.engineStatus;
    }
    public String getCryptoLibErrorCodeEnum(int errorCode)
    {
        return this.kmcCInterface.sdls_get_error_code_enum_string(errorCode);
    }
    private void configureKmcSdlsEngineFromProperties(Properties props) throws Exception
    {
        // configureCryptoLib properties
        String sadbType = props.getProperty("cryptolib.sadb.type");
        sadbType sadbTypeEnum;
        String cryptoType = props.getProperty("cryptolib.crypto.type");
        cryptographyType cryptographyTypeEnum;
        boolean cryptoCreateFecf = Boolean.parseBoolean(props.getProperty("cryptolib.apply_tc.create_ecf"));
        boolean processSdlsPdus = Boolean.parseBoolean(props.getProperty("cryptolib.process_tc.process_pdus"));
        boolean hasPusHdr = Boolean.parseBoolean(props.getProperty("cryptolib.tc.has_pus_header"));
        boolean ignoreSaState = Boolean.parseBoolean(props.getProperty("cryptolib.process_tc.ignore_sa_state"));
        boolean ignoreAntiReplay = Boolean.parseBoolean(props.getProperty("cryptolib.process_tc.ignore_antireplay"));
        boolean uniqueSaPerMapid = Boolean.parseBoolean(props.getProperty("cryptolib.tc.unique_sa_per_mapid"));
        boolean cryptoCheckFecf = Boolean.parseBoolean(props.getProperty("cryptolib.process_tc.check_ecf"));
        String vcidBitmaskHexStr = props.getProperty("cryptolib.tc.vcid_bitmask");
        int vcidBitmaskInt = Integer.decode(vcidBitmaskHexStr);
        boolean cryptoIncrementNontransmittedIv = Boolean.parseBoolean(props.getProperty("cryptolib.tc.on_rollover_increment_nontransmitted_counter"));

        // sadbTypeEnum;
        if (sadbType == null) { //default
            sadbTypeEnum = KmcSdlsEngine.sadbType.MARIADB;
        } else if (sadbType.equalsIgnoreCase("inmemory")) {
            sadbTypeEnum = KmcSdlsEngine.sadbType.INMEMORY;
        } else { // mariadb
            sadbTypeEnum =KmcSdlsEngine. sadbType.MARIADB;
        }

        // cryptographyTypeEnum;
        if(cryptoType == null) { //default
            cryptographyTypeEnum = KmcSdlsEngine.cryptographyType.KMC_CRYPTO_SERVICE;
        } else if (cryptoType.equalsIgnoreCase("libgcrypt")){
            cryptographyTypeEnum = KmcSdlsEngine.cryptographyType.LIBGCRYPT;
        } else { //kmc_crypto_service
            cryptographyTypeEnum = KmcSdlsEngine.cryptographyType.KMC_CRYPTO_SERVICE;
        }

        this.configureCryptoLib(sadbTypeEnum,cryptographyTypeEnum,cryptoCreateFecf,processSdlsPdus,hasPusHdr,ignoreSaState,ignoreAntiReplay,uniqueSaPerMapid,cryptoCheckFecf,vcidBitmaskInt,cryptoIncrementNontransmittedIv);

        // configureMariaDb
        if(sadbTypeEnum == KmcSdlsEngine.sadbType.MARIADB)
        {
            String mysqlHostname = props.getProperty("cryptolib.sadb.mariadb.fqdn");
            if (mysqlHostname == null) { mysqlHostname = "localhost"; }
            String mysqlDatabase = props.getProperty("cryptolib.sadb.mariadb.database_name");
            if(mysqlDatabase == null) { mysqlDatabase = "sadb"; }
            String mysqlPort = props.getProperty("cryptolib.sadb.mariadb.port");
            if(mysqlPort == null) { mysqlPort = "3306"; } // default port
            int mysqlPortInt = Integer.decode(mysqlPort);
            boolean mysqlRequireSecureTransport = Boolean.parseBoolean(props.getProperty("cryptolib.sadb.mariadb.require_secure_transport"));
            boolean mysqlTlsVerifyServer = Boolean.parseBoolean(props.getProperty("cryptolib.sadb.mariadb.tls.verifyserver"));
            String mysqlTlsCa = props.getProperty("cryptolib.sadb.mariadb.tls.cacert");
            String mysqlTlsCapath = props.getProperty("cryptolib.sadb.mariadb.tls.capath");
            String mysqlMtlsCert = props.getProperty("cryptolib.sadb.mariadb.mtls.clientcert");
            String mysqlMtlsKey = props.getProperty("cryptolib.sadb.mariadb.mtls.clientkey");
            String mysqlMtlsClientKeyPassword = props.getProperty("cryptolib.sadb.mariadb.mtls.clientkeypassword");
            String mysqlUsername = props.getProperty("cryptolib.sadb.mariadb.username");
            String mysqlPassword = props.getProperty("cryptolib.sadb.mariadb.password");

            this.configureMariaDb(mysqlHostname,mysqlDatabase,mysqlPortInt,mysqlRequireSecureTransport,mysqlTlsVerifyServer,mysqlTlsCa,mysqlTlsCapath,mysqlMtlsCert,mysqlMtlsKey,mysqlMtlsClientKeyPassword,mysqlUsername,mysqlPassword);
        }

        // configureCryptoService
        if(cryptographyTypeEnum == cryptographyType.KMC_CRYPTO_SERVICE)
        {
            String protocol = props.getProperty("cryptolib.crypto.kmccryptoservice.protocol");
            String kmcCryptoHostname = props.getProperty("cryptolib.crypto.kmccryptoservice.fqdn");
            String kmcCryptoPort = props.getProperty("cryptolib.crypto.kmccryptoservice.port");
            if(kmcCryptoPort == null) { kmcCryptoPort = "8443"; } // default port
            int kmcCryptoPortInt = Integer.decode(kmcCryptoPort);
            String kmcCryptoApp = props.getProperty("cryptolib.crypto.kmccryptoservice.app");
            String kmcTlsCaBundle = props.getProperty("cryptolib.crypto.kmccryptoservice.cacert");
            String kmcTlsCaPath = props.getProperty("cryptolib.crypto.kmccryptoservice.cacertpath");
            boolean kmcIgnoreSslHostnameValidation = Boolean.parseBoolean(props.getProperty("cryptolib.crypto.kmccryptoservice.verifyserver"));
            String mtlsClientCertPath = props.getProperty("cryptolib.crypto.kmccryptoservice.mtls.clientcert");
            String mtlsClientCertType = props.getProperty("cryptolib.crypto.kmccryptoservice.mtls.clientcertformat");
            String mtlsClientKeyPath = props.getProperty("cryptolib.crypto.kmccryptoservice.mtls.clientkey");
            String mtlsClientKeyPass = props.getProperty("cryptolib.crypto.kmccryptoservice.mtls.clientkeypassword");
            String mtlsIssuerCert = props.getProperty("cryptolib.crypto.kmccryptoservice.issuercert");

            // defaults
            if(protocol == null) { protocol = "https"; }
            if(kmcCryptoHostname == null){ kmcCryptoHostname = "localhost"; }
            if(kmcCryptoApp == null){ kmcCryptoApp = "crypto-service"; }
            if(mtlsClientCertType == null){ mtlsClientCertType = "PEM"; }

            this.configureCryptoService(protocol,kmcCryptoHostname,kmcCryptoPortInt,kmcCryptoApp,kmcTlsCaBundle,kmcTlsCaPath,kmcIgnoreSslHostnameValidation,mtlsClientCertPath,mtlsClientCertType,mtlsClientKeyPath,mtlsClientKeyPass,mtlsIssuerCert);
        }

        // configureCam
        boolean camEnabled = Boolean.parseBoolean(props.getProperty("cryptolib.cam.enabled"));
        if(camEnabled)
        {
            String cookieFilePath = props.getProperty("cryptolib.cam.cookie_file");
            String keytabFilePath = props.getProperty("cryptolib.cam.keytab_file");
            String camHome = props.getProperty("cryptolib.cam.cam_home");
            String loginMethodProperty = props.getProperty("cryptolib.cam.login_method");
            String accessManagerUri = props.getProperty("cryptolib.cam.access_manager_uri");
            String username = props.getProperty("cryptolib.cam.username");

            // loginMethodEnum;
            loginMethod loginMethodEnum;
            if (loginMethodProperty == null) { //default
                loginMethodEnum = KmcSdlsEngine.loginMethod.NONE;
            } else if (loginMethodProperty.equalsIgnoreCase("kerberos")) {
                loginMethodEnum = KmcSdlsEngine.loginMethod.KERBEROS;
            } else if (loginMethodProperty.equalsIgnoreCase("keytab_file")) {
                loginMethodEnum = KmcSdlsEngine.loginMethod.KEYTAB_FILE;
            } else { // NONE
                loginMethodEnum = KmcSdlsEngine.loginMethod.NONE;
            }

            this.configureCam(camEnabled, cookieFilePath, keytabFilePath, loginMethodEnum, accessManagerUri, username, camHome);

        }

        // gvcid managed parameters
        Set<String> keys = props.stringPropertyNames();
        String fecfManagedParamsPattern = "cryptolib\\.tc\\.(?<scid>\\d+)\\.(?<vcid>\\d+)\\.(?<tfvn>\\d+)\\.has_ecf";
        for (String key : keys)
        {
            Matcher m = Pattern.compile(fecfManagedParamsPattern).matcher(key);
            if (m.matches())
            {
                String scid = m.group("scid");
                String vcid = m.group("vcid");
                String tfvn = m.group("tfvn");
                int transferFrameVersionNumber = Integer.decode(tfvn);
                int spacecraftId = Integer.decode(scid);
                int virtualChannelID = Integer.decode(vcid);

                String hasEcfProp = String.format("cryptolib.tc.%d.%d.%d.has_ecf",spacecraftId, virtualChannelID, transferFrameVersionNumber);
                String hasSegHdrProp = String.format("cryptolib.tc.%d.%d.%d.has_segmentation_header",spacecraftId, virtualChannelID, transferFrameVersionNumber);
                String maxFrameSizeProp = String.format("cryptolib.tc.%d.%d.%d.max_frame_length",spacecraftId, virtualChannelID, transferFrameVersionNumber);
                boolean hasFecf = Boolean.parseBoolean(props.getProperty(hasEcfProp));
                boolean hasSegmentationHdr = Boolean.parseBoolean(props.getProperty(hasSegHdrProp));
                String maxTcFrameSize = props.getProperty(maxFrameSizeProp);
                if(maxTcFrameSize == null) { maxTcFrameSize = "1024"; } // default max frame size
                int maxTcFrameSizeInt = Integer.decode(maxTcFrameSize);

                this.addGvcidManagedParameter(transferFrameVersionNumber,spacecraftId,virtualChannelID,hasFecf,hasSegmentationHdr,maxTcFrameSizeInt);
            }
            else
            {
                continue;
            }

        }
    }
    public enum sadbType{
        UNINITIALIZED, CUSTOM, INMEMORY, MARIADB
    }
    public enum cryptographyType{
        LIBGCRYPT, KMC_CRYPTO_SERVICE
    }
    public enum loginMethod{
        NONE, KERBEROS, KEYTAB_FILE
    }

    public int configureCryptoLib(sadbType sadbTypeEnum, cryptographyType cryptographyTypeEnum, boolean cryptoCreateFecf, boolean processSdlsPdus,
                                  boolean hasPusHdr, boolean ignoreSaState, boolean ignoreAntiReplay,
                                  boolean uniqueSaPerMapid, boolean cryptoCheckFecf, int vcidBitmask, boolean cryptoIncrementNontransmittedIv) throws Exception
    {
        short sadb_type, cryptography_type, crypto_create_fecf, process_sdls_pdus, has_pus_hdr, ignore_sa_state, ignore_anti_replay,
                unique_sa_per_mapid, crypto_check_fecf, vcid_bitmask, crypto_increment_nontransmitted_iv;

        // Prepare the java parameters for the KMC SDLS JNI call.

        switch(sadbTypeEnum)
        {
            case MARIADB:
                sadb_type = (short)SadbType.SA_TYPE_MARIADB.swigValue();
                break;
            case INMEMORY:
                sadb_type = (short)SadbType.SA_TYPE_INMEMORY.swigValue();
                break;
            default:
                sadb_type = (short)SadbType.SA_TYPE_MARIADB.swigValue();
        }
        switch(cryptographyTypeEnum)
        {
            case LIBGCRYPT:
                cryptography_type = (short)CryptographyType.CRYPTOGRAPHY_TYPE_LIBGCRYPT.swigValue();
                break;
            case KMC_CRYPTO_SERVICE:
                cryptography_type = (short)CryptographyType.CRYPTOGRAPHY_TYPE_KMCCRYPTO.swigValue();
                break;
            default:
                cryptography_type = (short)CryptographyType.CRYPTOGRAPHY_TYPE_KMCCRYPTO.swigValue();
        }
        if(cryptoCreateFecf)
            crypto_create_fecf = (short)CreateFecfBool.CRYPTO_TC_CREATE_FECF_TRUE.swigValue();
        else
            crypto_create_fecf = (short)CreateFecfBool.CRYPTO_TC_CREATE_FECF_FALSE.swigValue();

        if(processSdlsPdus)
            process_sdls_pdus = (short)TcProcessSdlsPdus.TC_PROCESS_SDLS_PDUS_TRUE.swigValue();
        else
            process_sdls_pdus = (short)TcProcessSdlsPdus.TC_PROCESS_SDLS_PDUS_FALSE.swigValue();

        if(hasPusHdr)
            has_pus_hdr = (short)TcPusHdrPresent.TC_HAS_PUS_HDR.swigValue();
        else
            has_pus_hdr = (short)TcPusHdrPresent.TC_NO_PUS_HDR.swigValue();

        if(ignoreSaState)
            ignore_sa_state = (short)TcIgnoreSaState.TC_IGNORE_SA_STATE_TRUE.swigValue();
        else
            ignore_sa_state = (short)TcIgnoreSaState.TC_IGNORE_SA_STATE_FALSE.swigValue();

        if(ignoreAntiReplay)
            ignore_anti_replay = (short)TcIgnoreAntiReplay.TC_IGNORE_ANTI_REPLAY_TRUE.swigValue();
        else
            ignore_anti_replay = (short)TcIgnoreAntiReplay.TC_IGNORE_ANTI_REPLAY_FALSE.swigValue();

        if(uniqueSaPerMapid)
            unique_sa_per_mapid = (short)TcUniqueSaPerMapId.TC_UNIQUE_SA_PER_MAP_ID_TRUE.swigValue();
        else
            unique_sa_per_mapid = (short)TcUniqueSaPerMapId.TC_UNIQUE_SA_PER_MAP_ID_FALSE.swigValue();

        if(cryptoCheckFecf)
            crypto_check_fecf = (short)CheckFecfBool.TC_CHECK_FECF_TRUE.swigValue();
        else
            crypto_check_fecf = (short)CheckFecfBool.TC_CHECK_FECF_FALSE.swigValue();

        if(cryptoIncrementNontransmittedIv)
            crypto_increment_nontransmitted_iv = (short)SaIncrementNonTransmittedIvPortion.SA_INCREMENT_NONTRANSMITTED_IV_TRUE.swigValue();
        else
            crypto_increment_nontransmitted_iv = (short)SaIncrementNonTransmittedIvPortion.SA_INCREMENT_NONTRANSMITTED_IV_FALSE.swigValue();

        vcid_bitmask = (short)vcidBitmask;

        int status = this.kmcCInterface.sdls_config_cryptolib(sadb_type, cryptography_type);
        this.engineStatus = status;

        if(status != KMC_ENGINE_SUCCESS)
            throw new Exception("Unable to Configure CryptoLib Core, Error Code: " + status + ", Error Message: " + this.kmcCInterface.sdls_get_error_code_enum_string(status));

        return status;
    }
    public int addGvcidManagedParameter(int transferFrameVersionNumber, int spacecraftId, int virtualChannelID, boolean hasFecf,boolean hasSegmentationHdr, int maxTcFrameSize)
            throws Exception
    {
        short tfvn, scid, vcid, has_fecf, has_segmentation_hdr, max_tc_frame_size;

        tfvn = (short) transferFrameVersionNumber;
        scid = (short) spacecraftId;
        vcid = (short) virtualChannelID;
        if(hasFecf)
            has_fecf = (short) FecfPresent.TC_HAS_FECF.swigValue();
        else
            has_fecf = (short) FecfPresent.TC_NO_FECF.swigValue();
        if(hasSegmentationHdr)
            has_segmentation_hdr = (short) TcSegmentHdrsPresent.TC_HAS_SEGMENT_HDRS.swigValue();
        else
            has_segmentation_hdr = (short) TcSegmentHdrsPresent.TC_NO_SEGMENT_HDRS.swigValue();
        max_tc_frame_size = (short) maxTcFrameSize;

        int status = this.kmcCInterface.sdls_config_add_gvcid_managed_parameter(tfvn, scid, vcid, has_fecf, has_segmentation_hdr, max_tc_frame_size);
        this.engineStatus = status;

        if(status != KMC_ENGINE_SUCCESS)
            throw new Exception("Unable to Configure CryptoLib GVCID Managed Parameter, Error Code: " + status + ", Error Message: " + this.kmcCInterface.sdls_get_error_code_enum_string(status));

        return status;
    }
    public int configureMariaDb(String mysqlHostname, String mysqlDatabase, int mysqlPort, boolean mysqlRequireSecureTransport, boolean mysqlTlsVerifyServer, String mysqlTlsCa, String mysqlTlsCapath, String mysqlMtlsCert, String mysqlMtlsKey,String mysqlMtlsClientKeyPassword, String mysqlUsername, String mysqlPassword)
            throws Exception
    {
        short mysql_port, mysql_require_secure_transport, mysql_tls_verify_server;
        mysql_port = (short) mysqlPort;
        if(mysqlRequireSecureTransport)
            mysql_require_secure_transport = (short) 1;
        else
            mysql_require_secure_transport = (short) 0;
        if(mysqlTlsVerifyServer)
            mysql_tls_verify_server = (short) 1;
        else
            mysql_tls_verify_server = (short) 0;

        int status = this.kmcCInterface.sdls_config_mariadb(mysqlHostname, mysqlDatabase, mysql_port,mysql_require_secure_transport, mysql_tls_verify_server,mysqlTlsCa, mysqlTlsCapath, mysqlMtlsCert,mysqlMtlsKey,mysqlMtlsClientKeyPassword, mysqlUsername, mysqlPassword);
        this.engineStatus = status;

        if(status != KMC_ENGINE_SUCCESS)
            throw new Exception("Unable to Configure MariaDB, Error Code: " + status + ", Error Message: " + this.kmcCInterface.sdls_get_error_code_enum_string(status));

        return status;

    }
    public int configureCryptoService(String protocol, String kmcCryptoHostname, int kmcCryptoPort,
                                      String kmcCryptoApp, String kmcTlsCaBundle, String kmcTlsCaPath,
                                      boolean kmcIgnoreSslHostnameValidation, String mtlsClientCertPath,
                                      String mtlsClientCertType, String mtlsClientKeyPath,
                                      String mtlsClientKeyPass, String mtlsIssuerCert) throws Exception
    {
        short kmc_crypto_port, kmc_ignore_ssl_hostname_validation;
        kmc_crypto_port = (short) kmcCryptoPort;
        if(kmcIgnoreSslHostnameValidation)
            kmc_ignore_ssl_hostname_validation = (short) 1;
        else
            kmc_ignore_ssl_hostname_validation = (short) 0;

        int status = this.kmcCInterface.sdls_config_kmc_crypto_service(protocol, kmcCryptoHostname, kmc_crypto_port,
                kmcCryptoApp, kmcTlsCaBundle, kmcTlsCaPath, kmc_ignore_ssl_hostname_validation, mtlsClientCertPath,
                mtlsClientCertType, mtlsClientKeyPath,mtlsClientKeyPass, mtlsIssuerCert);
        this.engineStatus = status;

        if(status != KMC_ENGINE_SUCCESS)
            throw new Exception("Unable to Configure KMC Crypto Service, Error Code: " + status + ", Error Message: " + this.kmcCInterface.sdls_get_error_code_enum_string(status));

        return status;
    }

    public int configureCam(boolean camEnabled, String cookieFilePath, String keytabFilePath,
                            loginMethod loginMethodEnum, String accessManagerUri, String username, String camHome) throws Exception
    {
        short cam_enabled, login_method;

        // Prepare the java parameters for the Configure CAM JNI call.

        if(camEnabled)
            cam_enabled = (short)CamEnabledBool.CAM_ENABLED_TRUE.swigValue();
        else
            cam_enabled = (short)CamEnabledBool.CAM_ENABLED_FALSE.swigValue();


        switch(loginMethodEnum)
        {
            case NONE:
                login_method = (short)CamLoginMethod.CAM_LOGIN_NONE.swigValue();
                break;
            case KERBEROS:
                login_method = (short)CamLoginMethod.CAM_LOGIN_KERBEROS.swigValue();
                break;
            case KEYTAB_FILE:
                login_method = (short)CamLoginMethod.CAM_LOGIN_KEYTAB_FILE.swigValue();
                break;
            default:
                login_method = (short)CamLoginMethod.CAM_LOGIN_NONE.swigValue();
                break;
        }

        int status = this.kmcCInterface.sdls_config_cam(cam_enabled, cookieFilePath, keytabFilePath,
                login_method, accessManagerUri, username, camHome);
        this.engineStatus = status;

        if(status != KMC_ENGINE_SUCCESS)
            throw new Exception("Unable to Configure KMC Crypto Service, Error Code: " + status + ", Error Message: " + this.kmcCInterface.sdls_get_error_code_enum_string(status));

        return status;
    }

    public int init() throws Exception
    {
        int status = this.kmcCInterface.sdls_init();
        this.engineStatus = status;

        if(status != KMC_ENGINE_SUCCESS)
            throw new Exception("Unable to Init KMC SDLS Engine, Error Code: " + status + ", Error Message: " + this.kmcCInterface.sdls_get_error_code_enum_string(status));

        return status;
    }
    public void shutdown()
    {
        this.kmcCInterface.sdls_shutdown();
    }

    public String applySecurity(String unencryptedFrameHexbytesString) throws Exception
    {
        return this.applySecurity(unencryptedFrameHexbytesString,null);
    }
    public String applySecurity(String unencryptedFrameHexbytesString, String camCookies) throws Exception
    {
        int bytes_len = unencryptedFrameHexbytesString.length()/2 + unencryptedFrameHexbytesString.length()%2;
        SWIGTYPE_p_unsigned_short len_encrypted_frame = this.kmcCInterface.create_uint16t_ptr(0);

        SWIGTYPE_p_unsigned_char unencrypted_frame = this.kmcCInterface.hexstring_to_bytearray(unencryptedFrameHexbytesString,unencryptedFrameHexbytesString.length());
        SWIGTYPE_p_p_unsigned_char encrypted_frame = this.kmcCInterface.create_bytearray_pp();
        int status = this.kmcCInterface.apply_security_tc_cam(unencrypted_frame, bytes_len,encrypted_frame,len_encrypted_frame,camCookies);
        if(status != KMC_ENGINE_SUCCESS)
            throw new Exception("Unable to Apply Security on TC Frame, Error Code: " + status + ", Error Message: " + this.kmcCInterface.sdls_get_error_code_enum_string(status));

        String encrypted_frame_hexstring = this.kmcCInterface.bytearray_pp_to_hexstring(encrypted_frame,this.kmcCInterface.deref_uint16t_ptr(len_encrypted_frame));

        return encrypted_frame_hexstring;
    }

    public SDLS_TC_TransferFrame processSecurity(String encryptedFrameHexbytesString) throws Exception
    {
        return processSecurity(encryptedFrameHexbytesString,null);
    }
    public SDLS_TC_TransferFrame processSecurity(String encryptedFrameHexbytesString, String camCookies) throws Exception
    {
        int bytes_len = encryptedFrameHexbytesString.length()/2 + encryptedFrameHexbytesString.length()%2;
        SWIGTYPE_p_int len_encrypted_frame = this.kmcCInterface.create_int_ptr(bytes_len);
        SWIGTYPE_p_unsigned_char encrypted_frame = this.kmcCInterface.hexstring_to_bytearray(encryptedFrameHexbytesString,encryptedFrameHexbytesString.length());
        this.tc_ptr = new TC_t();

        int status = this.kmcCInterface.process_security_tc_uint8t(encrypted_frame,len_encrypted_frame,this.tc_ptr,camCookies);
        if(status != KMC_ENGINE_SUCCESS)
            throw new Exception("Unable to Process Security on TC Frame, Error Code: " + status + ", Error Message: " + this.kmcCInterface.sdls_get_error_code_enum_string(status));

        SDLS_TC_TransferFrame sdlsTCFrame = new SDLS_TC_TransferFrame();
        // Parse Primary Header
        sdlsTCFrame.tfvn = this.tc_ptr.getTc_header().getTfvn();
        sdlsTCFrame.bypass = this.tc_ptr.getTc_header().getBypass();
        sdlsTCFrame.cc = this.tc_ptr.getTc_header().getCc();
        sdlsTCFrame.spare = this.tc_ptr.getTc_header().getSpare();
        sdlsTCFrame.scid = this.tc_ptr.getTc_header().getScid();
        sdlsTCFrame.vcid = this.tc_ptr.getTc_header().getVcid();
        sdlsTCFrame.fl = this.tc_ptr.getTc_header().getFl();
        sdlsTCFrame.fsn = this.tc_ptr.getTc_header().getFsn();

        //sdlsTCFrame.tfvn = this.kmcCInterface.get_tfvn(this.tc_ptr);
        //sdlsTCFrame.bypass = this.kmcCInterface.get_bypass(this.tc_ptr);
        //sdlsTCFrame.cc = this.kmcCInterface.get_cc(this.tc_ptr);
        //sdlsTCFrame.spare = this.kmcCInterface.get_spare(this.tc_ptr);
        //sdlsTCFrame.scid = this.kmcCInterface.get_scid(this.tc_ptr);
        //sdlsTCFrame.vcid = this.kmcCInterface.get_vcid(this.tc_ptr);
        //sdlsTCFrame.fl = this.kmcCInterface.get_fl(this.tc_ptr);
        //sdlsTCFrame.fsn = this.kmcCInterface.get_fsn(this.tc_ptr);
        // Parse Security Headear
        sdlsTCFrame.sh = this.tc_ptr.getTc_sec_header().getSh();
        sdlsTCFrame.spi = this.tc_ptr.getTc_sec_header().getSpi();
        sdlsTCFrame.iv_field_len = this.tc_ptr.getTc_sec_header().getIv_field_len();
        SWIGTYPE_p_unsigned_char iv_ptr = this.tc_ptr.getTc_sec_header().getIv();
        sdlsTCFrame.iv = this.kmcCInterface.bytearray_to_hexstring(iv_ptr,sdlsTCFrame.iv_field_len);
        sdlsTCFrame.sn_field_len = this.tc_ptr.getTc_sec_header().getSn_field_len();
        SWIGTYPE_p_unsigned_char sn_ptr = this.tc_ptr.getTc_sec_header().getSn();
        sdlsTCFrame.sn = this.kmcCInterface.bytearray_to_hexstring(sn_ptr,sdlsTCFrame.sn_field_len);
        sdlsTCFrame.pad_field_len = this.tc_ptr.getTc_sec_header().getPad_field_len();
        SWIGTYPE_p_unsigned_char pad_ptr =this.tc_ptr.getTc_sec_header().getPad();
        sdlsTCFrame.pad = this.kmcCInterface.bytearray_to_hexstring(pad_ptr,sdlsTCFrame.pad_field_len);
//        sdlsTCFrame.sh = this.kmcCInterface.get_sh(this.tc_ptr);
//        sdlsTCFrame.spi = this.kmcCInterface.get_spi(this.tc_ptr);
//        sdlsTCFrame.iv_field_len = this.kmcCInterface.get_iv_field_len(this.tc_ptr);
//        SWIGTYPE_p_unsigned_char iv_ptr = this.kmcCInterface.get_iv_ptr(this.tc_ptr);
//        sdlsTCFrame.iv = this.kmcCInterface.bytearray_to_hexstring(iv_ptr,sdlsTCFrame.iv_field_len);
//        sdlsTCFrame.sn_field_len = this.kmcCInterface.get_sn_field_len(this.tc_ptr);
//        SWIGTYPE_p_unsigned_char sn_ptr = this.kmcCInterface.get_sn_ptr(this.tc_ptr);
//        sdlsTCFrame.sn = this.kmcCInterface.bytearray_to_hexstring(sn_ptr,sdlsTCFrame.sn_field_len);
//        sdlsTCFrame.pad_field_len = this.kmcCInterface.get_pad_field_len(this.tc_ptr);
//        SWIGTYPE_p_unsigned_char pad_ptr = this.kmcCInterface.get_pad_ptr(this.tc_ptr);
//        sdlsTCFrame.pad = this.kmcCInterface.bytearray_to_hexstring(pad_ptr,sdlsTCFrame.pad_field_len);
        // Parse Frame Data
        sdlsTCFrame.tc_pdu_len = this.tc_ptr.getTc_pdu_len();
        SWIGTYPE_p_unsigned_char pdu_ptr = this.tc_ptr.getTc_pdu();
        sdlsTCFrame.tc_pdu = this.kmcCInterface.bytearray_to_hexstring(pdu_ptr,sdlsTCFrame.tc_pdu_len);
//        sdlsTCFrame.tc_pdu_len = this.kmcCInterface.get_tc_pdu_len(this.tc_ptr);
//        SWIGTYPE_p_unsigned_char pdu_ptr = this.kmcCInterface.get_pdu_ptr(this.tc_ptr);
//        sdlsTCFrame.tc_pdu = this.kmcCInterface.bytearray_to_hexstring(pdu_ptr,sdlsTCFrame.tc_pdu_len);
        // Parse Security Trailer Fields
        sdlsTCFrame.mac_field_len = this.tc_ptr.getTc_sec_trailer().getMac_field_len();
        SWIGTYPE_p_unsigned_char mac_ptr = this.tc_ptr.getTc_sec_trailer().getMac();
        sdlsTCFrame.mac = this.kmcCInterface.bytearray_to_hexstring(mac_ptr,sdlsTCFrame.mac_field_len);
        sdlsTCFrame.fecf = this.tc_ptr.getTc_sec_trailer().getFecf();
//        sdlsTCFrame.mac_field_len = this.kmcCInterface.get_mac_field_len(this.tc_ptr);
//        SWIGTYPE_p_unsigned_char mac_ptr = this.kmcCInterface.get_mac_ptr(this.tc_ptr);
//        sdlsTCFrame.mac = this.kmcCInterface.bytearray_to_hexstring(mac_ptr,sdlsTCFrame.mac_field_len);
//        sdlsTCFrame.fecf = this.kmcCInterface.get_fecf(this.tc_ptr);

        return sdlsTCFrame;
    }
    public String processSecurityReturnDataOnly(String encryptedFrameHexbytesString) throws Exception
    {
        return processSecurityReturnDataOnly(encryptedFrameHexbytesString, null);
    }
    public String processSecurityReturnDataOnly(String encryptedFrameHexbytesString, String camCookies) throws Exception
    {
        int bytes_len = encryptedFrameHexbytesString.length()/2 + encryptedFrameHexbytesString.length()%2;
        SWIGTYPE_p_int len_encrypted_frame = this.kmcCInterface.create_int_ptr(bytes_len);
        SWIGTYPE_p_unsigned_char encrypted_frame = this.kmcCInterface.hexstring_to_bytearray(encryptedFrameHexbytesString,encryptedFrameHexbytesString.length());
        this.tc_ptr = new TC_t();

        int status = this.kmcCInterface.process_security_tc_uint8t(encrypted_frame,len_encrypted_frame,this.tc_ptr, camCookies);
        if(status != KMC_ENGINE_SUCCESS)
            throw new Exception("Unable to Process Security on TC Frame, Error Code: " + status + ", Error Message: " + this.kmcCInterface.sdls_get_error_code_enum_string(status));

        return this.kmcCInterface.bytearray_to_hexstring(this.tc_ptr.getTc_pdu(),this.tc_ptr.getTc_pdu_len());
    }

}

