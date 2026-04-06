package gov.nasa.jpl.ammos.kmc.keyclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientManagerException.KmcKeyOpsManagerErrorCode;

/**
 * The KmcKeyClientManager handles the configuration of the KMC Key Client.
 * KMC Key Client provides functions for creating, retrieving,
 * and changing the state of keys in KMS.
 * The KmcKeyClientManager performs the following functions:
 * <ul>
 * <li> Loads the configuration file for the parameters to be used by the key operations functions.
 * <li> Processes input command-line arguments for setting values of the configuration parameters.
 * <li> Provides getter and setter methods for the configuration parameters.
 * <li> Initializes the logger.
 * </ul>
 *
 * <p>
 * The default configuration directory is defined in kmc.properties
 * It can be changed by the kmc_key_config_dir parameter of KmcKeyClientManager.
 * </p>
 *
 * <p>
 * The default location of the key operations configuration file is
 * <tt>&lt;kmc_key_config_dir&gt;/kmc-key-service.cfg</tt> when it is used by the KMC Key Service,
 * or <tt>&lt;kmc_key_config_dir&gt;/kmc-key-client.cfg</tt> when it is used by the KMC Test.
 * It can also be loaded from CLASSPATH.
 * </p>
 *
 * <p>
 * The default location of the log4j config file is <tt>&lt;kmc_key_config_dir&gt;/kmc-log4j2.xml</tt>,
 * but it can also be loaded from CLASSPATH.  If the log4j config file is loaded from a file location
 * it will be monitored for changes every 60 seconds.
 * </p>
 *
 * The key operations config file can have parameters as listed in the CFG_ constants.
 * Some of these parameters can also be set by command-line arguments passed in to the
 * KmcKeyClientManager constructor.
 * The command-line arguments start with "-", followed by the CFG_ constant,
 * and then followed by "=value", e.g. -key_management_service_uri=https://kms.example.com:8443/kms.
 * Currently only these two parameters can be set:
 * <ul>
 * <li> -kmc_key_config_dir: directory containing the configuration files (default is defined in kmc.properties).
 * <li> -key_management_service_uri: URI of the Key Management Service (default specified in kmc-key-service.cfg).
 * </ul>
 *
 * <p>
 * Many of the parameters in the config file are the default and allowed cryptographic algorithms to be used.
 * These parameters cannot be set by command-line arguments, but the default algorithms can be set using the API.
 * The names of the algorithms should follow the ones used in Java unless otherwise noted.
 * The value of the allowed algorithms is a list of names separated by a colon.
 * </p>
 *
 *
 */
public class KmcKeyClientManager {
    static {
        java.io.InputStream is = KmcKeyClientManager.class.getClassLoader().getResourceAsStream("kmc.properties");
        java.util.Properties p = new Properties();
        try {
            p.load(is);
          
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
        DEFAULT_KMC_HOME = p.getProperty("DEFAULT_KMC_ROOT") + "/kmc-crypto-client";
        DEFAULT_KMC_KEY_SERVICE_CONFIG_DIR = p.getProperty("DEFAULT_KMC_ROOT") + "/key-service/etc";
        DEFAULT_KMC_KEY_CLIENT_CONFIG_DIR = p.getProperty("DEFAULT_KMC_ROOT") +  "/test/etc";
    }
    /**
     * The environment variable for KMC_HOME.
     */
    public static final String ENV_KMC_HOME = "KMC_HOME";
    /**
     * The home directory of the KMC Client software.  Default of KMC_HOME is default is defined in kmc.properties
     */
    public static final String DEFAULT_KMC_HOME;
    /**
     * Default KMC Key Service configuration directory relative to KMC_HOME.
     */
    public static final String DEFAULT_KMC_KEY_SERVICE_CONFIG_DIR;
    /**
     * Default KMC Key Client configuration directory relative to KMC_HOME.
     */
    public static final String DEFAULT_KMC_KEY_CLIENT_CONFIG_DIR;
    /**
     * Default KMC Key Service configuration file name.
     */
    private static final String DEFAULT_KMC_KEY_SERVICE_CONFIG_FILE = "kmc-key-service.cfg";
    /**
     * Default KMC Key Client configuration file name.
     */
    private static final String DEFAULT_KMC_KEY_CLIENT_CONFIG_FILE = "kmc-key-client.cfg";
    /**
     * Default algorithm used for symmetric encryption.  The algorithm string also contains the key length.
     */
    public static final String DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM = "AES-256";
    /**
     * Default algorithm used for asymmetric encryption.  The algorithm string also contains the key length.
     */
    public static final String DEFAULT_ASYMMETRIC_ENCRYPTION_ALGORITHM = "RSA-4096";
    /**
     * Default Message Digest algorithm used for integrity check.
     */
    public static final String DEFAULT_MESSAGE_DIGEST_ALGORITHM = "SHA-256";
    /**
     * Default HMAC (Keyed-Hash Message Authentication Code) algorithm used for integrity check.
     */
    public static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA256";
    /**
     * Default algorithm for digital signature.
     */
    public static final String DEFAULT_DIGITAL_SIGNATURE_ALGORITHM = "SHA256withRSA";
    /**
     * Default transformation for AES cipher used for symmetric encryption.
     */
    public static final String DEFAULT_AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    /**
     * Default transformation for Triple DES cipher used for symmetric encryption.
     */
    public static final String DEFAULT_TRIPLE_DES_TRANSFORMATION = "DESede/CBC/PKCS5Padding";
    /**
     * Default transformation for RSA cipher used for asymmetric encryption.
     */
    public static final String DEFAULT_RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    /**
     * Default character encoding for strings.
     */
    //public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

    /**
     * Config parameter for the configuration directory of the KMC KMS library.
     */
    public static final String CFG_KMC_KEY_CONFIG_DIR = "kmc_key_config_dir";
    /**
     * Config parameter for the URI of the Key Management Service.
     */
    public static final String CFG_KEY_MANAGEMENT_SERVICE_URI = "key_management_service_uri";
    /**
     * Config parameter for the keystore to authenticate with KMS.
     */
    public static final String CFG_KEYSTORE_FILE = "keystore_file";
    /**
     * Config parameter for the password of the keystore.
     */
    public static final String CFG_KEYSTORE_PASSWORD = "keystore_password";
    /**
     * Config parameter for the default algorithm for Message Digest.
     */
    public static final String CFG_DEFAULT_MESSAGE_DIGEST_ALGORITHM = "default_message_digest_algorithm";
    /**
     * Config parameter for the allowed algorithms for Message Digest.
     */
    public static final String CFG_ALLOWED_MESSAGE_DIGEST_ALGORITHMS = "allowed_message_digest_algorithms";
    /**
     * Config parameter for the default algorithm for HMAC.
     */
    public static final String CFG_DEFAULT_HMAC_ALGORITHM = "default_hmac_algorithm";
    /**
     * Config parameter for the allowed algorithms for HMAC.
     */
    public static final String CFG_ALLOWED_HMAC_ALGORITHMS = "allowed_hmac_algorithms";
    /**
     * Config parameter for the default algorithm for digital signature.
     */
    public static final String CFG_DEFAULT_DIGITAL_SIGNATURE_ALGORITHM = "default_digital_signature_algorithm";
    /**
     * Config parameter for the allowed algorithms for digital signature.
     */
    public static final String CFG_ALLOWED_DIGITAL_SIGNATURE_ALGORITHMS = "allowed_digital_signature_algorithms";
    /**
     * Config parameter for the allowed algorithms for symmetric encryption.
     */
    public static final String CFG_DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM = "default_symmetric_encryption_algorithm";
    /**
     * Config parameter for the allowed algorithms for symmetric encryption.
     */
    public static final String CFG_ALLOWED_SYMMETRIC_ENCRYPTION_ALGORITHMS = "allowed_symmetric_encryption_algorithms";
    /**
     * Config parameter for the allowed algorithms for asymmetric encryption.
     */
    public static final String CFG_DEFAULT_ASYMMETRIC_ENCRYPTION_ALGORITHM = "default_asymmetric_encryption_algorithm";
    /**
     * Config parameter for the allowed algorithms for symmetric encryption.
     */
    public static final String CFG_ALLOWED_ASYMMETRIC_ENCRYPTION_ALGORITHMS = "allowed_asymmetric_encryption_algorithms";
    /**
     * Config parameter for the default transformation for AES encryption.
     */
    public static final String CFG_DEFAULT_AES_TRANSFORMATION = "default_AES_transformation";
    /**
     * Config parameter for the allowed transformations for AES encryption.
     */
    public static final String CFG_ALLOWED_AES_TRANSFORMATIONS = "allowed_AES_transformations";
    /**
     * Config parameter for the default transformation for TripleDES encryption.
     */
    public static final String CFG_DEFAULT_TRIPLE_DES_TRANSFORMATION = "default_TripleDES_transformation";
    /**
     * Config parameter for the allowed transformations for TripleDEs encryption.
     */
    public static final String CFG_ALLOWED_TRIPLE_DES_TRANSFORMATIONS = "allowed_TripleDES_transformations";
    /**
     * Config parameter for the default transformation for RSA encryption.
     */
    public static final String CFG_DEFAULT_RSA_TRANSFORMATION = "default_RSA_transformation";
    /**
     * Config parameter for the allowed transformations for RSA encryption.
     */
    public static final String CFG_ALLOWED_RSA_TRANSFORMATIONS = "allowed_RSA_transformations";

    // parameter in config but not in config file
    private static final String CFG_SSO_COOKIE = "sso_cookie";

    private static final String ALLOWED_SEPARATOR = ":";

    private static final Logger logger = LoggerFactory.getLogger(KmcKeyClientManager.class);

    private String configDir;
    private Properties config;
    private Map<String, String> defaultTransformations;
    private String ssoCookie = null;

    /**
     * <p>
     * This constructor initializes the {@link KmcKeyClientManager} based on the parameters
     * in the configuration file and from the input arguments.
     * </p>
     *
     * It accepts the following arguments for overriding the parameters in the configuration file.
     * <ol>
     * <li> -kmc_key_config_dir: directory containing the configuration files.
     * <li> -key_management_service_uri: URI of the KMS for key retrieval
     * </ol>
     *
     * @param args command-line arguments
     * @throws KmcKeyClientManagerException if any error occurred during initialization
     */
    public KmcKeyClientManager(final String[] args) throws KmcKeyClientManagerException {
        String[] argv;
        String configFile;
        InputStream configStream;

        if (args == null) {
            argv = new String[0];
        } else {
            argv = args;
        }

        String inputConfigDir = getInputConfigDir(argv);
        if (inputConfigDir != null) {
            configDir = inputConfigDir;
        } else {
            configDir = DEFAULT_KMC_KEY_CLIENT_CONFIG_DIR;
        }
        configFile = configDir + "/" + DEFAULT_KMC_KEY_SERVICE_CONFIG_FILE;
        if (! new File(configFile).isFile()) {
            configDir = DEFAULT_KMC_KEY_CLIENT_CONFIG_DIR;
            configFile = configDir + "/" + DEFAULT_KMC_KEY_CLIENT_CONFIG_FILE;
        }

        if (loadConfigFile(configFile)) {
            logger.info("Loaded KMC Key Service config file: " + configFile);
        } else {
            // load the config file from CLASSPATH
            ClassLoader loader = KmcKeyClientManager.class.getClassLoader();
            configStream = loader.getResourceAsStream(DEFAULT_KMC_KEY_CLIENT_CONFIG_FILE);
            if (configStream == null) {
                String msg = "KMC config file not found in 1) " + configFile
                        + " and 2) " + DEFAULT_KMC_KEY_CLIENT_CONFIG_DIR + " in CLASSPATH.";
                System.err.println(msg);
                logger.error(msg);
                throw new KmcKeyClientManagerException(
                        KmcKeyOpsManagerErrorCode.CONFIG_FILE_NOT_FOUND, msg, null);
            } else {
                if (loadConfigStream(configStream)) {
                    logger.info("Loaded CLASSPATH config file: " + DEFAULT_KMC_KEY_CLIENT_CONFIG_DIR);
                } else {
                    String msg = "Failed to load config file: " + DEFAULT_KMC_KEY_CLIENT_CONFIG_DIR;
                    logger.error(msg);
                    throw new KmcKeyClientManagerException(
                            KmcKeyOpsManagerErrorCode.CONFIG_FILE_NOT_FOUND, msg, null);
                }
            }
        }
        logger.debug("config file content = " + config);

        for (String arg : argv) {
            String[] keyValue = arg.split("=");
            logger.debug("Process CLI argument: " + arg);
            if (keyValue[0].equals("-" + CFG_KEY_MANAGEMENT_SERVICE_URI)) {
                config.setProperty(CFG_KEY_MANAGEMENT_SERVICE_URI, keyValue[1]);
            } else if (keyValue[0].equals("-" + CFG_KMC_KEY_CONFIG_DIR)) {
                // ignore, should have been processed earlier.
                logger.trace(CFG_KMC_KEY_CONFIG_DIR + " argument have been processed earlier.");
            } else {
                logger.warn("Unexpected CLI argument ignored: " + keyValue[0]);
            }
        }

        checkConfigParameters();

        initDefaultTransformations();
    }

    /**
     * Initializes the {@link KmcKeyClientManager} using the specified config file.
     * It loads the config file and sets up the {@link KmcKeyClientManager}
     * according to its parameter values.
     * @param configPath path to the config file.
     * @return true if loading the config file without error.
     * @throws KmcKeyClientManagerException if error loading config file.
     */
    private boolean loadConfigFile(final String configPath)
            throws KmcKeyClientManagerException {
        try {
            InputStream configStream = new FileInputStream(configPath);
            boolean result = loadConfigStream(configStream);
            configStream.close();
            return result;
        } catch (FileNotFoundException e) {
            logger.warn("Default config file not found in " + configPath);
            return false;
        } catch (IOException e) {
            logger.error("Exception on closing config file stream");
            return false;
        }
    }

    /**
     * Initializes the {@link KmcKeyClientManager} using the config file input stream.
     * It loads the config file and sets up the {@link KmcKeyClientManager}
     * according to its parameter values.
     * @param configStream input stream of the config file.
     * @return true if the config file is loaded without error.
     * @throws KmcKeyClientManagerException if error reading the config file.
     */
    private boolean loadConfigStream(final InputStream configStream)
        throws KmcKeyClientManagerException {
        if (configStream == null) {
            logger.error("Config file stream cannot be null.");
            return false;
        }
        config = new Properties();
        try {
            config.load(configStream);
        } catch (IOException e) {
            logger.error("Failed to load config file stream: " + configStream);
            throw new KmcKeyClientManagerException(KmcKeyOpsManagerErrorCode.CONFIG_FILE_NOT_FOUND,
                    "Config file not found in " + configStream, e.getCause());
        }
        return true;
    }

    /**
     * Check missing config parameters and invalid parameter values.
     * These parameters can be in the config file or passed in at runtime.
     * @throws KmcKeyClientManagerException if error.
     */
    private void checkConfigParameters() throws KmcKeyClientManagerException {
        String[] parameters = new String[] {
                CFG_KEY_MANAGEMENT_SERVICE_URI,
                };
        for (String param : parameters) {
            String value = config.getProperty(param);
            if (value == null) {
                String errorMsg = "Parameter " + param + " not found in config file.";
                logger.error(errorMsg);
                throw new KmcKeyClientManagerException(
                    KmcKeyOpsManagerErrorCode.CONFIG_PARAMETER_NOT_FOUND, errorMsg, null);
            }
            value = value.trim();
            if (value.isEmpty()) {
                String errorMsg = "Missing value in config parameter: " + param;
                logger.error(errorMsg);
                throw new KmcKeyClientManagerException(
                        KmcKeyOpsManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID,
                        errorMsg, null);
            }
            if (param == CFG_KEY_MANAGEMENT_SERVICE_URI) {
                if (!value.startsWith("http") && !value.startsWith("tls")) {
                    String errorMsg = "Invalid KMS URI: " + value;
                    logger.error(errorMsg);
                    throw new KmcKeyClientManagerException(
                            KmcKeyOpsManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID,
                            errorMsg, null);
                }
            }
        }
    }

    /**
     * Puts the default transformations in a HashMap for easy retrieval.
     */
    private void initDefaultTransformations() {
        defaultTransformations = new HashMap<String, String>();
        defaultTransformations.put("AES", DEFAULT_AES_TRANSFORMATION);
        //defaultTransformations.put("TripleDES", DEFAULT_TripleDES_TRANSFORMATION);
        //defaultTransformations.put("RSA", DEFAULT_RSA_TRANSFORMATION);
    }

    /**
     * Returns the path to the KMC configuration directory.
     * @return The path to the KMC configuration directory.
     */
    public final String getKmcConfigPath()  {
        return configDir;
    }

    /**
     * Returns the KMC KMS configuration parameters.
     * @return The KMC KMS configuration parameters.
     */
    public final Properties getConfigParameters()  {
        return config;
    }

    /**
     * Returns the URI of the key management service.
     * @return The URI of the key management service.
     */
    public final String getKeyManagementServiceURI()  {
        return config.getProperty(CFG_KEY_MANAGEMENT_SERVICE_URI);
    }

    /**
     * Sets the URI of the key management service.
     * @param uri the URI of the key management service.
     */
    public final void setKeyManagementServiceURI(final String uri)  {
        config.setProperty(CFG_KEY_MANAGEMENT_SERVICE_URI, uri);
    }

    /**
     * Returns the path to the keystore file.
     * @return The path to the keystore file.
     */
    public final String getKeystoreFile()  {
        return config.getProperty(CFG_KEYSTORE_FILE);
    }

    /**
     * Returns the password of the keystore.
     * @return The password of the keystore.
     */
    public final String getKeystorePassword()  {
        return config.getProperty(CFG_KEYSTORE_PASSWORD);
    }

    /**
     * Creates a {@link KmcKeyClient} which operate keys in the KMS.
     * @return The {@link KmcKeyClient} instance for KMS.
     * @throws KmcKeyClientManagerException if it can't get the URI of the KMS.
     */
    public final KmcKeyClient getKmcKmipKeyClient() throws KmcKeyClientManagerException {
        try {
            return new KmcKmipKeyClient(this);
        } catch (KmcKeyClientException e) {
            String error = "Exception in creating KmcKmipKeyClient instance: " + e;
            logger.error(error);
            throw new KmcKeyClientManagerException(
                    KmcKeyOpsManagerErrorCode.NULL_VALUE, error, null);
        }
    }

    /**
     * Creates a {@link KmcKeyClient} which operates keys in a keystore.
     * The keystorePass or keyPass can be null if not used by the keystore.
     * All keys in the keystore must have the same key password (keyPass) if a key password is used.
     *
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @return The {@link KmcKeyClient} instance for keystore.
     * @throws KmcKeyClientManagerException if keystore cannot be loaded.
     */
    public final KmcKeyClient getKmcKeystoreKeyClient(
            final String keystoreLocation, final String keystorePass, final String keystoreType)
                    throws KmcKeyClientManagerException {
        try {
            return new KmcKeystoreKeyClient(keystoreLocation, keystorePass, keystoreType);
        } catch (KmcKeyClientException e) {
            String error = "Exception in creating KmcKeystoreKeyClient instance: " + e;
            logger.error(error);
            throw new KmcKeyClientManagerException(
                    KmcKeyOpsManagerErrorCode.NULL_VALUE, error, e.getCause());
        }
    }

    /**
     * Returns the default algorithm and key length for symmetric encryption.
     * @return The default algorithm and key length for symmetric encryption.
     */
    public final String getSymmetricEncryptionAlgorithm() {
        String algorithm = config.getProperty(DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM);
        if (algorithm == null) {
            return DEFAULT_SYMMETRIC_ENCRYPTION_ALGORITHM;
        } else {
            return algorithm;
        }
    }

    /**
     * Returns the allowed algorithm and key length for symmetric encryption.
     * @return The allowed algorithm and key length for symmetric encryption
     *         or null if not specified in the config file.
     */
    public final String getAllowedSymmetricEncryptionAlgorithms() {
        return config.getProperty(CFG_ALLOWED_SYMMETRIC_ENCRYPTION_ALGORITHMS);
    }

    /**
     * Returns the default algorithm and key length for asymmetric encryption.
     * @return The default algorithm and key length for asymmetric encryption.
     */
    public final String getAsymmetricEncryptionAlgorithm() {
        String algorithm = config.getProperty(DEFAULT_ASYMMETRIC_ENCRYPTION_ALGORITHM);
        if (algorithm == null) {
            return DEFAULT_ASYMMETRIC_ENCRYPTION_ALGORITHM;
        } else {
            return algorithm;
        }
    }

    /**
     * Returns the allowed algorithm and key length for symmetric encryption.
     * @return The allowed algorithm and key length for symmetric encryption
     *         or null if not specified in the config file.
     */
    public final String getAllowedAsymmetricEncryptionAlgorithms() {
        return config.getProperty(CFG_ALLOWED_ASYMMETRIC_ENCRYPTION_ALGORITHMS);
    }

    /**
     * Sets the Message Digest algorithm for creating integrity check value.
     * @param algorithm A Message Digest algorithm.
     * @throws KmcKeyClientManagerException if the algorithm is not in the allowed list.
     */
    public final void setMessageDigestAlgorithm(final String algorithm)
            throws KmcKeyClientManagerException {
        String error = null;
        String allowed = config.getProperty(CFG_ALLOWED_MESSAGE_DIGEST_ALGORITHMS);

        if (algorithm == null) {
            error = "Null algorithm value.";
        } else if (allowed == null) {
            error = "No allowed_message_digest_algorithms in config.";
        } else {
            String[] algorithms = allowed.split(ALLOWED_SEPARATOR);
            for (String a : algorithms) {
                if (algorithm.equals(a)) {
                    config.setProperty(CFG_DEFAULT_MESSAGE_DIGEST_ALGORITHM, algorithm);
                    return;
                }
            }
            error = "Message Digest algorithm " + algorithm + " is not in the allowed list.";
        }
        logger.error(error);
        throw new KmcKeyClientManagerException(
                KmcKeyOpsManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
    }

    /**
     * Returns the default Message Digest algorithm.
     * @return The default Message Digest algorithm.
     */
    public final String getMessageDigestAlgorithm() {
        String algorithm = config.getProperty(CFG_DEFAULT_MESSAGE_DIGEST_ALGORITHM);
        if (algorithm == null) {
            return DEFAULT_MESSAGE_DIGEST_ALGORITHM;
        } else {
            return algorithm;
        }
    }

    /**
     * Returns the allowed Message Digest algorithms.
     * @return The allowed Message Digest algorithms or null if not specified in the config file.
     */
    public final String getAllowedMessageDigestAlgorithms() {
        return config.getProperty(CFG_ALLOWED_MESSAGE_DIGEST_ALGORITHMS);
    }

    /**
     * Sets the HMAC algorithm for creating integrity check value.
     * @param algorithm A HMAC algorithm.
     * @throws KmcKeyClientManagerException if the algorithm is not in the allowed list.
     */
    public final void setHmacAlgorithm(final String algorithm)
            throws KmcKeyClientManagerException {
        String error = null;
        String allowed = config.getProperty(CFG_ALLOWED_HMAC_ALGORITHMS);
        if (algorithm == null) {
            error = "Null algorithm value.";
        } else if (allowed == null) {
            error = "No allowed_hmac_algorithms in config.";
        } else {
            String[] algorithms = allowed.split(ALLOWED_SEPARATOR);
            for (String a : algorithms) {
                if (algorithm.equals(a)) {
                    config.setProperty(CFG_DEFAULT_HMAC_ALGORITHM, algorithm);
                    return;
                }
            }
            error = "HMAC algorithm " + algorithm + " is not in the allowed list.";
        }
        logger.error(error);
        throw new KmcKeyClientManagerException(
                KmcKeyOpsManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
    }

    /**
     * Returns the default HMAC algorithm.
     * @return The default HMAC algorithm.
     */
    public final String getHmacAlgorithm() {
        String algorithm = config.getProperty(CFG_DEFAULT_HMAC_ALGORITHM);
        if (algorithm == null) {
            return DEFAULT_HMAC_ALGORITHM;
        } else {
            return algorithm;
        }
    }

    /**
     * Returns the allowed Digital Signature algorithms.
     * @return The allowed Digital Signature algorithms or null if not specified in the config file.
     */
    public final String getAllowedDigitalSignatureAlgorithms() {
        return config.getProperty(CFG_ALLOWED_DIGITAL_SIGNATURE_ALGORITHMS);
    }

    /**
     * Returns the default Digital Signature algorithm.
     * @return The default Digital Signature algorithm.
     */
    public final String getDigitalSignatureAlgorithm() {
        String algorithm = config.getProperty(CFG_DEFAULT_HMAC_ALGORITHM);
        if (algorithm == null) {
            return DEFAULT_DIGITAL_SIGNATURE_ALGORITHM;
        } else {
            return algorithm;
        }
    }

    /**
     * Returns the allowed HMAC algorithms.
     * @return The allowed HMAC algorithms or null if not specified in the config file.
     */
    public final String getAllowedHmacAlgorithms() {
        return config.getProperty(CFG_ALLOWED_HMAC_ALGORITHMS);
    }

    /**
     * Sets the cipher transformation for encryption.
     * @param transformation A cipher transformation.
     * @throws KmcKeyClientManagerException if the transformation is not in the allowed list.
     */
    public final void setCipherTransformation(final String transformation)
            throws KmcKeyClientManagerException {
        String error = null;
        if (transformation == null) {
            error = "Input transformation value is null.";
            logger.error(error);
            throw new KmcKeyClientManagerException(
                    KmcKeyOpsManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
        }
        String[] parts = transformation.split("/");
        String allowedParam = "allowed_" + parts[0] + "_transformations";
        String allowed = config.getProperty(allowedParam);
        if (allowed == null) {
            error = "No " + allowedParam + " in config.";
        } else {
            String[] transformations = allowed.split(ALLOWED_SEPARATOR);
            for (String t : transformations) {
                if (transformation.equals(t)) {
                    config.setProperty("default_" + parts[0] + "_transformation", transformation);
                    return;
                }
            }
            error = transformation + " is not in the allowed list.";
        }
        logger.error(error);
        throw new KmcKeyClientManagerException(
                KmcKeyOpsManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
    }

    /**
     * Returns the default cipher transformation of the specified encryption algorithm.
     * @param algorithm The encryption algorithm.
     * @return The default cipher transformation of the specified encryption algorithm.
     */
    public final String getCipherTransformation(final String algorithm) {
        String transformation = config.getProperty("default_" + algorithm + "_transformation");
        if (transformation == null) {
            return defaultTransformations.get(algorithm);
        }
        return transformation;
    }

    /**
     * Returns the allowed AES transformations.
     * @return The allowed AES transformations or null if not specified in the config file.
     */
    public final String getAesTransformations() {
        return config.getProperty(CFG_ALLOWED_AES_TRANSFORMATIONS);
    }

    /**
     * Sets the SSO cookie for authorization to access the server and keys.
     * @param ssoCookie The SSO cookie (name=value) of the authenticated user.
     */
    public final void setSsoCookie(final String ssoCookie) {
        config.setProperty(CFG_SSO_COOKIE, ssoCookie);
        this.ssoCookie = ssoCookie;
    }

    /**
     * Returns the SSO cookie associated with the principal running the application.
     * @return The SSO cookie associated with the principal running the application.
     */
    public final String getSsoCookie() {
        return this.ssoCookie;
    }

    private String getInputConfigDir(final String[] args) throws KmcKeyClientManagerException {
        for (String arg : args) {
            if (arg.startsWith("-" + CFG_KMC_KEY_CONFIG_DIR)) {
                String[] keyValue = arg.split("=");
                if (keyValue.length != 2) {
                    String msg = "Missing value for the " + CFG_KMC_KEY_CONFIG_DIR + " arugment: " + arg;
                    logger.error(msg);
                    throw new KmcKeyClientManagerException(
                            KmcKeyOpsManagerErrorCode.CONFIG_PARAMETER_NOT_FOUND, msg, null);
                } else {
                    String dir = keyValue[1];
                    if (!dir.endsWith("/")) {
                        dir = dir + "/";
                    }
                    return dir;
                }
            }
        }
        return null;
    }

}
