package gov.nasa.jpl.ammos.kmc.crypto;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;

/**
 * The KmcCryptoManager is the entry point for using the KMC Cryptography.
 * It performs the following functions:
 * <ul>
 * <li> Loads the configuration file for the parameters to be used by the crypto functions.
 * <li> Processes input command-line arguments for setting values of the configuration parameters.
 * <li> Provides getter and setter methods for the configuration parameters.
 * <li> Creates implementation instances of the KMC Cryptography classes.
 * <li> Initializes the logger.
 * </ul>
 *
 * <p>
 * The default configuration directory is defined in the kmc.properties file.  It can be changed
 * by the kmc_config_dir parameter of KmcCryptoManager.
 * </p>
 *
 * <p>
 * The default location of the crypto configuration file is <code>&lt;kmc_config_dir&gt;/kmc-crypto.cfg</code>,
 * but it can also be loaded from CLASSPATH.
 * </p>
 *
 * <p>
 * The default location of the log4j properties file is <code>&lt;kmc_config_dir&gt;/kmc-crypto-log4j2.xml</code>,
 * but it can also be loaded from CLASSPATH.
 * </p>
 *
 * <p>
 * The crypto config file can have parameters as listed in the CFG_ constants.
 * All of these parameters can also be set by command-line arguments passed in to the
 * KmcCryptoManager constructor.
 * The command-line arguments start with "-", followed by the CFG_ constant,
 * and then followed by "=value", e.g. <code>-key_management_service_uri=https://kms.example.com:8443/kmip-service</code>.
 * </p>
 *
 * <p>
 * Many of the parameters in the config file are the default and allowed cryptographic algorithms to be used.
 * These parameters cannot be set by command-line arguments, but the default algorithms can be set using the API.
 * The names of the algorithms should follow the ones used in Java unless otherwise noted.
 * The value of the allowed algorithms is a list of names separated by colon.
 * </p>
 *
 *
 */
public class KmcCryptoManager {

    private static final Logger logger = LoggerFactory.getLogger(KmcCryptoManager.class);

    /**
     * The home directory of the KMC Crypto Client software.  Default of KMC_HOME is set in kmc.properties.
     */
    public static String DEFAULT_KMC_HOME = "/opt/ammos/kmc";
    /**
     * The home directory of the KMC Crypto Service software.  Default of KMC_HOME is set in kmc.properties.
     */
    public static String DEFAULT_KMC_CRYPTO_SERVICE_HOME = DEFAULT_KMC_HOME + "/services/crypto-service";

    static {
        java.io.InputStream is = KmcCryptoManager.class.getClassLoader().getResourceAsStream("kmc.properties");
        if (is == null) {
            logger.warn("KmcCryptoManager: kmc.properties does not exist in classpath");
        } else {
            java.util.Properties p = new Properties();
            try {
                p.load(is);
                DEFAULT_KMC_HOME = p.getProperty("DEFAULT_KMC_ROOT");
                DEFAULT_KMC_CRYPTO_SERVICE_HOME = p.getProperty("DEFAULT_KMC_ROOT")+"/services/crypto-service";
            } catch (IOException e) {
                logger.warn("KmcCryptoManager: Failed to load kmc.properties from classpath: {}", e);
            }
        }
        logger.debug("KmcCryptoManager: DEFAULT_KMC_HOME = {}", DEFAULT_KMC_HOME);
    }

    /**
     * The environment variable for KMC_HOME is for KMC Crypto Client.
     */
    public static final String ENV_KMC_HOME = "KMC_HOME";
    /**
     * The environment variable for KMC_CRYPTO_SERVICE_HOME is for KMC Crypto Service.
     */
    public static final String ENV_KMC_CRYPTO_SERVICE_HOME = "KMC_CRYPTO_SERVICE_HOME";
    /**
     * Default KMC Crypto configuration directory.
     */
    public static final String DEFAULT_CRYPTO_CONFIG_DIR = DEFAULT_KMC_HOME + "/etc";
    /**
     * Default KMC Crypto configuration file name.  Default path is $KMC_HOME/etc/kmc-crypto.cfg
     */
    public static final String DEFAULT_CRYPTO_CONFIG_FILE = "kmc-crypto.cfg";
    /**
     * Default class name of the {@link Encrypter} implementation for local library.
     */
    public static final String ENCRYPTER_LIBRARY_CLASS =
            "gov.nasa.jpl.ammos.kmc.crypto.library.EncrypterLibrary";
    /**
     * Default class name of the {@link Decrypter} implementation for local library.
     */
    public static final String DECRYPTER_LIBRARY_CLASS =
            "gov.nasa.jpl.ammos.kmc.crypto.library.DecrypterLibrary";
    /**
     * Default class name of the {@link IcvCreator} implementation for local library.
     */
    public static final String ICV_CREATOR_LIBRARY_CLASS =
            "gov.nasa.jpl.ammos.kmc.crypto.library.IcvCreatorLibrary";
    /**
     * Default class name of the {@link IcvVerifier} implementation for local library.
     */
    public static final String ICV_VERIFIER_LIBRARY_CLASS =
            "gov.nasa.jpl.ammos.kmc.crypto.library.IcvVerifierLibrary";
    /**
     * Default algorithm used for random number generator.
     */
    public static final String DEFAULT_SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
    /**
     * Default Message Digest algorithm used for integrity check.
     */
    public static final String DEFAULT_MESSAGE_DIGEST_ALGORITHM = "SHA-256";
    /**
     * Default HMAC (Keyed-Hash Message Authentication Code) algorithm used for integrity check.
     */
    public static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA256";
    /**
     * Default CMAC (Cipher-based Message Authentication Code) algorithm used for integrity check.
     */
    public static final String DEFAULT_CMAC_ALGORITHM = "AESCMAC";
    /**
     * Default Digital Signature algorithm used for integrity check.
     */
    public static final String DEFAULT_DIGIAL_SIGNATURE_ALGORITHM = "SHA256withRSA";
    /**
     * Default transformation for AES cipher used for symmetric encryption and decryption.
     */
    public static final String DEFAULT_AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    /**
     * Default transformation for Triple DES cipher used for symmetric encryption and decryption.
     */
    public static final String DEFAULT_TRIPLE_DES_TRANSFORMATION = "DESede/CBC/PKCS5Padding";
    /**
     * Default transformation for RSA cipher used for asymmetric encryption and decryption.
     */
    public static final String DEFAULT_RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    /**
     * The allowed minimum number of bits for truncated MAC.
     * NIST standards: minimum lengths of MAC from 64 bit (CMAC) to 96 bit (GMAC) are recommended.
     */
    public static final int DEFAULT_ALLOWED_MIN_MAC_LENGTH = 32;

    /**
     * The configuration directory of the KMC Cryptographic library.
     */
    public static final String CFG_KMC_CRYPTO_CONFIG_DIR = "kmc_crypto_config_dir";
    /**
     * Config parameter for the URI of the service for retrieving keys and certificates.
     */
    public static final String CFG_KEY_MANAGEMENT_SERVICE_URI = "key_management_service_uri";
    /**
     * Config parameter for the TLS keystore to authenticate with KMS.
     */
    public static final String CFG_TLS_KEYSTORE_FILE = "keystore_file";
    /**
     * Config parameter for the password of the keystore.
     */
    public static final String CFG_TLS_KEYSTORE_PASSWORD = "keystore_password";
    /**
     * Config parameter for the location of the keystore containing keys for cryptography.
     */
    public static final String CFG_CRYPTO_KEYSTORE_LOCATION = "crypto_keystore_location";
    /**
     * Config parameter for the type of the keystore containing keys for cryptography.
     */
    public static final String CFG_CRYPTO_KEYSTORE_TYPE = "crypto_keystore_type";
    /**
     * Config parameter for the password of the keystore containing keys for cryptography.
     */
    public static final String CFG_CRYPTO_KEYSTORE_PASSWORD = "crypto_keystore_password";
    /**
     * Config parameter for the password for retrieving the key in the keystore.
     * PKCS12 keystore does not use key password.
     */
    public static final String CFG_CRYPTO_KEY_PASSWORD = "crypto_key_password";
    /**
     * The SSO cookie for authenticating the crypto user.
     */
    private static final String CFG_SSO_COOKIE = "sso_cookie";
    /**
     * Config parameter for the class name of the {@link Encrypter} implementation.
     */
    //public static final String CFG_ENCRYPTER_CLASS = "encrypter_class";
    /**
     * Config parameter for the class name of the {@link Decrypter} implementation.
     */
    //public static final String CFG_DECRYPTER_CLASS = "decrypter_class";
    /**
     * Config parameter for the class name of the {@link IcvCreator} implementation.
     */
    //public static final String CFG_ICV_CREATOR_CLASS = "icv_creator_class";
    /**
     * Config parameter for the class name of the {@link IcvVerifier} implementation.
     */
    //public static final String CFG_ICV_VERIFIER_CLASS = "icv_verifier_class";

    /**
     * Config parameter for the default algorithm for Secure Random Number Generator.
     */
    public static final String CFG_DEFAULT_SECURE_RANDOM_ALGORITHM = "default_secure_random_algorithm";
    /**
     * Config parameter for the allowed algorithms for Message Digest.
     */
    public static final String CFG_ALLOWED_SECURE_RANDOM_ALGORITHMS = "allowed_secure_random_algorithms";
    /**
     * Config parameter for the allowed symmetric encryption algorithms.
     */
    public static final String CFG_ALLOWED_SYMMETRIC_ENCRYPTION_ALGORITHMS = "allowed_symmetric_encryption_algorithms";
    /**
     * Config parameter for the allowed symmetric encryption algorithms.
     */
    public static final String CFG_ALLOWED_ASYMMETRIC_ENCRYPTION_ALGORITHMS = "allowed_asymmetric_encryption_algorithms";
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
     * Config parameter for the default algorithm for CMAC.
     */
    public static final String CFG_DEFAULT_CMAC_ALGORITHM = "default_cmac_algorithm";
    /**
     * Config parameter for the allowed algorithms for HMAC.
     */
    public static final String CFG_ALLOWED_CMAC_ALGORITHMS = "allowed_cmac_algorithms";
    /**
     * Config parameter for the default digital signature algorithm.
     */
    public static final String CFG_DEFAULT_DIGITAL_SIGNATURE_ALGORITHM = "default_digital_signature_algorithm";
    /**
     * Config parameter for the allowed digital signature algorithms.
     */
    public static final String CFG_ALLOWED_DIGITAL_SIGNATURE_ALGORITHMS = "allowed_digital_signature_algorithms";
    /**
     * Config parameter for the default transformation for AES encryption.
     */
    public static final String CFG_DEFAULT_AES_TRANSFORMATION = "default_AES_transformation";
    /**
     * Config parameter for the allowed transformations for AES encryption.
     */
    public static final String CFG_ALLOWED_AES_TRANSFORMATIONS = "allowed_AES_transformations";
    /**
     * Config parameter for the default transformation for Triple DES encryption.
     */
    public static final String CFG_DEFAULT_TRIPLE_DES_TRANSFORMATION = "default_DESede_transformation";
    /**
     * Config parameter for the allowed transformations for Triple DES encryption.
     */
    public static final String CFG_ALLOWED_TRIPLE_DES_TRANSFORMATIONS = "allowed_DESede_transformations";
    /**
     * Config parameter for the default transformation for RSA encryption.
     */
    public static final String CFG_DEFAULT_RSA_TRANSFORMATION = "default_RSA_transformation";
    /**
     * Config parameter for the allowed transformations for RSA encryption.
     */
    public static final String CFG_ALLOWED_RSA_TRANSFORMATIONS = "allowed_RSA_transformations";
    /**
     * Config parameter for the allowed MAC length.
     */
    public static final String CFG_ALLOWED_MIN_MAC_LENGTH = "allowed_min_MAC_length";
    /**
     * Config parameter for the default MAC length.  0 for algorithm specific full length.
     */
    public static final String CFG_TRUNCATED_MAC_LENGTH = "truncated_MAC_length";
    /**
     * The suffix of crypto algorithm provider.  The config parameter is &lt;algorithm&gt;_provider.
     */
    public static final String ALGORITHM_PROVIDER_SUFFIX = "_provider";
    /**
     * The suffix of crypto algorithm provider class.  The config parameter is &lt;provider&gt;_provider_class.
     */
    public static final String PROVIDER_CLASS_SUFFIX = "_provider_class";
    /**
     * The maximum bytes of data accepted by KMC cryptographic functions.
     */
    public static final int MAX_CRYPTO_SIZE = 100000000;

    private static final String VALUE_SEPARATOR = ":";
    private static final String[] ALL_CFG_ALLOWED_ALGORITHMS = new String[] {
        CFG_ALLOWED_SECURE_RANDOM_ALGORITHMS, CFG_ALLOWED_SYMMETRIC_ENCRYPTION_ALGORITHMS,
        CFG_ALLOWED_ASYMMETRIC_ENCRYPTION_ALGORITHMS, CFG_ALLOWED_MESSAGE_DIGEST_ALGORITHMS,
        CFG_ALLOWED_HMAC_ALGORITHMS, CFG_ALLOWED_CMAC_ALGORITHMS,
        CFG_ALLOWED_DIGITAL_SIGNATURE_ALGORITHMS
    };

    /**
     * Default KMC Crypto configuration directory.
     */
    private String configDir = DEFAULT_CRYPTO_CONFIG_DIR;
    private Properties config;
    private Map<String, String> defaultTransformations;

    /**
     * This constructor initializes the {@link KmcCryptoManager} based on the parameters
     * in the configuration file and from the input arguments.
     *
     * It accepts the following arguments for overriding the parameters in the configuration file.
     * <ol>
     * <li> -kmc_crypto_config_dir: directory containing the configuration files.
     * <li> -key_management_service_uri: URI of the KMS for key retrieval
     * </ol>
     *
     * @param args command-line arguments
     * @throws KmcCryptoManagerException if any error occurred during initialization
     */
    public KmcCryptoManager(final String[] args) throws KmcCryptoManagerException {
        String[] argv;
        String configFilename = DEFAULT_CRYPTO_CONFIG_FILE;
        InputStream configStream;

        if (args == null) {
            argv = new String[0];
        } else {
            argv = args;
        }

        String kmcHome = System.getenv(ENV_KMC_HOME);
        if (kmcHome == null) {
            kmcHome = DEFAULT_KMC_HOME;
        }
        if (kmcHome.endsWith("/")) {
            kmcHome = kmcHome.substring(0, kmcHome.length() - 1);
        }
        configDir = kmcHome + "/etc";

        String inputConfigDir = getInputConfigDir(argv);
        if (inputConfigDir != null) {
            configDir = inputConfigDir;
        }

        String configFile = configDir + "/" + configFilename;
        if (loadConfigFile(configFile)) {
            logger.info("Loaded KMC Crypto config file: " + configFile);
        } else {
            // load the config file from CLASSPATH
            ClassLoader loader = KmcCryptoManager.class.getClassLoader();
            configStream = loader.getResourceAsStream(configFilename);
            if (configStream == null) {
                String msg = "KMC config file not found in 1) " + configFile
                        + " and 2) " + configFilename + " in CLASSPATH.";
                System.err.println(msg);
                logger.error(msg);
                throw new KmcCryptoManagerException(
                        KmcCryptoManagerErrorCode.CONFIG_FILE_NOT_FOUND, msg, null);
            } else {
                if (loadConfigStream(configStream)) {
                    logger.info("Loaded CLASSPATH config file: " + configFilename);
                    String classFile = loader.getResource(configFilename).getFile();
                    configDir = classFile.substring(0, classFile.length() - configFilename.length() - 1);
                } else {
                    String msg = "Failed to load config file: " + configFilename;
                    logger.error(msg);
                    throw new KmcCryptoManagerException(
                            KmcCryptoManagerErrorCode.CONFIG_FILE_NOT_FOUND, msg, null);
                }
            }
        }
        logger.trace("config file content = " + config);

        initDefaultTransformations();

        for (String arg : argv) {
            if (!arg.startsWith("-")) {
                logger.debug("CLI argument not starting with '-' ignored: " + arg);
                continue;
            }
            String[] keyValue = arg.split("=", 2);
            if (keyValue.length != 2) {
                logger.info("CLI argument without '=' ignored: " + arg);
                continue;
            }
            String key = keyValue[0].substring(1);
            String value = keyValue[1];
            logger.trace("Process CLI argument: " + arg);
            if (key.equals(CFG_KMC_CRYPTO_CONFIG_DIR)) {
                logger.trace("Ignored the processed CLI argument: " + keyValue[0]);
            } else if (key.equals(CFG_KEY_MANAGEMENT_SERVICE_URI)) {
                config.setProperty(CFG_KEY_MANAGEMENT_SERVICE_URI, keyValue[1]);
            } else if (key.equals(CFG_CRYPTO_KEYSTORE_LOCATION)) {
                config.setProperty(CFG_CRYPTO_KEYSTORE_LOCATION, keyValue[1]);
            } else if (key.equals(CFG_CRYPTO_KEYSTORE_TYPE)) {
                config.setProperty(CFG_CRYPTO_KEYSTORE_TYPE, keyValue[1]);
            } else if (key.equals(CFG_CRYPTO_KEYSTORE_PASSWORD)) {
                config.setProperty(CFG_CRYPTO_KEYSTORE_PASSWORD, keyValue[1]);
            } else if (key.equals(CFG_CRYPTO_KEY_PASSWORD)) {
                config.setProperty(CFG_CRYPTO_KEY_PASSWORD, keyValue[1]);
            } else if (key.equals(CFG_TLS_KEYSTORE_FILE)) {
                config.setProperty(CFG_TLS_KEYSTORE_FILE, value);
            } else if (key.equals(CFG_TLS_KEYSTORE_PASSWORD)) {
                config.setProperty(CFG_TLS_KEYSTORE_PASSWORD, value);
            } else if (key.equals(CFG_DEFAULT_MESSAGE_DIGEST_ALGORITHM)) {
                config.setProperty(CFG_DEFAULT_MESSAGE_DIGEST_ALGORITHM, value);
            } else if (key.equals(CFG_DEFAULT_HMAC_ALGORITHM)) {
                config.setProperty(CFG_DEFAULT_HMAC_ALGORITHM, value);
            } else if (key.equals(CFG_DEFAULT_DIGITAL_SIGNATURE_ALGORITHM)) {
                config.setProperty(CFG_DEFAULT_DIGITAL_SIGNATURE_ALGORITHM, value);
            } else if (key.equals(CFG_DEFAULT_AES_TRANSFORMATION)) {
                this.setCipherTransformation(keyValue[1]);
            } else if (key.equals(CFG_DEFAULT_TRIPLE_DES_TRANSFORMATION)) {
                this.setCipherTransformation(keyValue[1]);
            } else if (key.equals(CFG_DEFAULT_RSA_TRANSFORMATION)) {
                this.setCipherTransformation(keyValue[1]);
            } else if (key.equals(CFG_ALLOWED_MIN_MAC_LENGTH)) {
                logger.debug("set allowed minimum mac length: " + key + " = " + value);
                this.setAllowedMinMacLength(value);
            } else if (key.equals(CFG_TRUNCATED_MAC_LENGTH)) {
                logger.debug("set mac length: " + key + " = " + value);
                this.setMacLength(value);
            } else if (key.endsWith(ALGORITHM_PROVIDER_SUFFIX)) {
                logger.debug("set algorithm provider: " + key + " = " + value);
                config.setProperty(key, value);
            } else if (keyValue[0].endsWith(PROVIDER_CLASS_SUFFIX)) {
                logger.debug("set provider class: " + key + " = " + value);
                config.setProperty(key, value);
            } else {
                logger.debug("Unknown CLI argument ignored: " + arg);
            }
        }

        checkConfigParameters();
    }

    /**
     * Initializes the {@link KmcCryptoManager} using the specified config file.
     * It loads the config file and sets up the {@link KmcCryptoManager}
     * according to its parameter values.
     * @param configPath path to the config file.
     * @return true if loading the config file without error.
     * @throws KmcCryptoManagerException if error loading config file.
     */
    private boolean loadConfigFile(final String configPath)
            throws KmcCryptoManagerException {
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
     * Initializes the {@link KmcCryptoManager} using the config file input stream.
     * It loads the config file and sets up the {@link KmcCryptoManager}
     * according to its parameter values.
     * @param configStream input stream of the config file.
     * @return true if the config file is loaded without error.
     * @throws KmcCryptoManagerException if error reading the config file.
     */
    private boolean loadConfigStream(final InputStream configStream)
        throws KmcCryptoManagerException {
        if (configStream == null) {
            logger.error("Config file stream cannot be null.");
            return false;
        }
        config = new Properties();
        try {
            config.load(configStream);
        } catch (IOException e) {
            logger.error("Failed to load config file stream: " + configStream);
            throw new KmcCryptoManagerException(KmcCryptoManagerErrorCode.CONFIG_FILE_NOT_FOUND,
                    "Config file not found in " + configStream, e.getCause());
        }
        return true;
    }

    /**
     * Check missing config parameters and invalid parameter values.
     * These parameters can be in the config file or passed in at runtime.
     * @throws KmcCryptoManagerException if error.
     */
    private void checkConfigParameters() throws KmcCryptoManagerException {
        String[] parameters = new String[] {
                CFG_KEY_MANAGEMENT_SERVICE_URI
                };
        for (String param : parameters) {
            String value = config.getProperty(param);
            if (value != null) {
                value = value.trim();
            }
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (CFG_KEY_MANAGEMENT_SERVICE_URI.equals(param)) {
                if (!value.startsWith("http") && !value.startsWith("tls")) {
                    String errorMsg = "Invalid KMS URI: " + value;
                    logger.error(errorMsg);
                    throw new KmcCryptoManagerException(
                            KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID,
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
        defaultTransformations.put("DESede", DEFAULT_TRIPLE_DES_TRANSFORMATION);
        defaultTransformations.put("RSA", DEFAULT_RSA_TRANSFORMATION);
    }

    /**
     * Returns the path to the KMC configuration directory.
     * @return The path to the KMC configuration directory.
     */
    public final String getKmcConfigDir()  {
        return configDir;
    }

    /**
     * Returns the KMC crypto configuration parameters loaded from the config file kmc-config.cfg.
     * @return The KMC crypto configuration parameters
     */
    public final Properties getConfigParameters()  {
        return config;
    }

    /**
     * Returns the URI of the key management service.
     * @return The URI of the key management service.
     */
    public final String getKeyManagementServiceURI()  {
        String kmsURI = config.getProperty(CFG_KEY_MANAGEMENT_SERVICE_URI);
        if (kmsURI == null || kmsURI.isEmpty()) {
            return null;
        } else {
            return kmsURI;
        }
    }

    /**
     * Sets the URI of the key management service.
     * @param uri the URI of the key management service.
     */
    public final void setKeyManagementServiceURI(final String uri)  {
        if (uri == null) {
            config.remove(CFG_KEY_MANAGEMENT_SERVICE_URI);
        } else {
            config.setProperty(CFG_KEY_MANAGEMENT_SERVICE_URI, uri);
        }
    }

    /**
     * Returns the location of the keystore containing keys for cryptography.
     * @return The location of the crypto keystore.
     */
    public final String getCryptoKeystoreLocation()  {
        String keystoreLocation = config.getProperty(CFG_CRYPTO_KEYSTORE_LOCATION);
        if (keystoreLocation == null || keystoreLocation.isEmpty()) {
            return null;
        } else {
            return keystoreLocation;
        }
    }

    /**
     * Sets the location of the keystore containing keys for cryptography.
     * @param path The location of the crypto keystore.
     */
    public void setCryptoKeystoreLocation(final String path)  {
        if (path == null) {
            config.remove(CFG_CRYPTO_KEYSTORE_LOCATION);
        } else {
            config.setProperty(CFG_CRYPTO_KEYSTORE_LOCATION, path);
        }
    }

    /**
     * Returns the type of the keystore containing keys for cryptography.
     * @return The type of the crypto keystore.
     */
    public final String getCryptoKeystoreType()  {
        return config.getProperty(CFG_CRYPTO_KEYSTORE_TYPE);
    }

    /**
     * Sets the type of the keystore containing keys for cryptography.
     * @param type The type of the crypto keystore.
     */
    public void setCryptoKeystoreType(final String type)  {
        config.setProperty(CFG_CRYPTO_KEYSTORE_TYPE, type);
    }

    /**
     * Returns the password of the keystore containing keys for cryptography.
     * @return The password of the crypto keystore.
     */
    public final String getCryptoKeystorePassword()  {
        return config.getProperty(CFG_CRYPTO_KEYSTORE_PASSWORD);
    }

    /**
     * Sets the password of the keystore containing keys for cryptography.
     * @param password The password of the crypto keystore.
     */
    public void setCryptoKeystorePassword(final String password)  {
        config.setProperty(CFG_CRYPTO_KEYSTORE_PASSWORD, password);
    }

    /**
     * Returns the password for retrieving the key in the keystore.
     * PKCS12 keystore does not use key password.
     * @return The password of the crypto keystore.
     */
    public final String getCryptoKeyPassword()  {
        return config.getProperty(CFG_CRYPTO_KEY_PASSWORD);
    }

    /**
     * Sets the password for retrieving the key in the keystore.
     * PKCS12 keystore does not use key password.
     * @param password The password for retrieving the key in the keystore.
     */
    public void setCryptoKeyPassword(final String password)  {
        config.setProperty(CFG_CRYPTO_KEY_PASSWORD, password);
    }

    /**
     * Returns the path to the TLS keystore file.
     * @return The path to the TLS keystore file.
     */
    public final String getKeystoreFile()  {
        return config.getProperty(CFG_TLS_KEYSTORE_FILE);
    }

    /**
     * Returns the password of the TLS keystore.
     * @return The password of the TLS keystore.
     */
    public final String getKeystorePassword()  {
        return config.getProperty(CFG_TLS_KEYSTORE_PASSWORD);
    }

    /**
     * Returns the name of the provider of the specified algorithm.
     * @param algorithm Java standard name of the cryptographic algorithm.
     * @return Name of the provider of the specified algorithm.
     */
    public final String getAlgorithmProvider(final String algorithm)  {
        return config.getProperty(algorithm + ALGORITHM_PROVIDER_SUFFIX);
    }

    /**
     * Sets the provider of the specified algorithm.
     * @param algorithm Java standard name of the cryptographic algorithm.
     * @param provider Java standard name of the cryptographic service provider.
     */
    public final void setAlgorithmProvider(final String algorithm, final String provider)  {
        config.setProperty(algorithm + ALGORITHM_PROVIDER_SUFFIX, provider);
    }

    /**
     * Removes the provider of the specified algorithm.
     * @param algorithm Java standard name of the cryptographic algorithm.
     */
    public final void removeAlgorithmProvider(final String algorithm)  {
        config.remove(algorithm + ALGORITHM_PROVIDER_SUFFIX);
    }

    /**
     * Returns the class name of the crypto algorithm provider.
     * @param provider name of the crypto algorithm provider.
     * @return Name of the crypto algorithm provider.
     */
    public final String getProviderClass(final String provider)  {
        return config.getProperty(provider + PROVIDER_CLASS_SUFFIX);
    }

    /**
     * Sets the class name of the crypto algorithm provider.
     * @param provider name of the crypto algorithm provider.
     * @param providerClass class name of the crypto algorithm provider.
     */
    public final void setProviderClass(final String provider, final String providerClass)  {
        config.setProperty(provider + PROVIDER_CLASS_SUFFIX, providerClass);
    }

    /**
     * Removes the provider class of the specified provider.
     * @param provider name of the crypto algorithm provider.
     */
    public final void removeProviderClass(final String provider)  {
        config.remove(provider + PROVIDER_CLASS_SUFFIX);
    }

    /**
     * Returns true if the algorithm, in the form of algorithm name and key length,
     * is allowed for cryptographic use as configured in kmc-crypto.cfg.
     * @param algorithm The algorithm and key length to be checked.
     * @return true if the algorithm is an allowed for cryptographic use.
     */
    public final boolean isAllowedAlgorithm(final String algorithm)  {
        for (String cfgAllowed : ALL_CFG_ALLOWED_ALGORITHMS) {
            String allowed = VALUE_SEPARATOR + config.getProperty(cfgAllowed) + VALUE_SEPARATOR;
            if (allowed.contains(VALUE_SEPARATOR + algorithm + VALUE_SEPARATOR)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the algorithm, in the form of algorithm name and key length,
     * is allowed for the specified cryptographic use as configured in kmc-crypto.cfg.
     * @param algorithm The algorithm and key length to be checked.
     * @param cryptoUse The cryptographic usage of the algorithm.  The value should be one of the
     *                  allowed algorithms configuration parameter names, e.g. allowed_symmetric_encryption_algorithms.
     * @return true if the algorithm is an allowed for the specified cryptographic use.
     */
    public final boolean isAllowedAlgorithm(final String algorithm, final String cryptoUse)  {
        String allowed = VALUE_SEPARATOR + config.getProperty(cryptoUse) + VALUE_SEPARATOR;
        return allowed.contains(VALUE_SEPARATOR + algorithm + VALUE_SEPARATOR);
    }

    /**
     * Returns the default algorithm for digital signature.
     * @return The default algorithm for digital signature.
     */
    public final String getDefaultDigitalSignatureAlgorithm()  {
        return config.getProperty(CFG_DEFAULT_DIGITAL_SIGNATURE_ALGORITHM);
    }

    /**
     * Creates an {@link Encrypter} which obtains the encryption key from keystore or Key Management Service (KMS).
     * During creation of the instance the encryption key as specified in keyRef is retrieved.
     * The key determines the algorithm and key length that are used for encryption.
     * The cipher transformation should be set in the KmcCryptoManager prior to calling this method
     * if the default mode of operation and padding scheme are not used.
     *
     * @param keyRef A string for identifying the key, i.e. the name of the key.
     * @return The {@link Encrypter} object.
     * @throws KmcCryptoManagerException if error in retrieving the key.
     */
    public final Encrypter createEncrypter(final String keyRef) throws KmcCryptoManagerException {
        return createCryptoObject(ENCRYPTER_LIBRARY_CLASS, keyRef);
    }

    /**
     * Creates an {@link Encrypter} which obtains the encryption key from a keystore.
     * During creation of the instance the encryption key as specified in keyRef is retrieved
     * from the specified keystore.  The key determines the algorithm and key length that are used for encryption.
     * The cipher transformation should be set in the KmcCryptoManager prior to calling this method
     * if the default mode of operation and padding scheme are not used.
     * <p>
     * The keystorePass or keyPass can be null if they are not used by the keystore.
     * All the keys in the keystore must have the same key password (keyPass) if a key password is used.
     * </p>
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @param keyRef A string for identifying the key, also known as alias.
     * @param keyPass The password of the key to be retrieved.
     * @return The {@link Encrypter} object.
     * @throws KmcCryptoManagerException if error in retrieving the key.
     */
    public final Encrypter createEncrypter(final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyRef, final String keyPass)
                    throws KmcCryptoManagerException {
        return createCryptoObject(ENCRYPTER_LIBRARY_CLASS,
                keystoreLocation, keystorePass, keystoreType, keyRef, keyPass);
    }

    /**
     * Creates a {@link Decrypter} which obtains the decryption key
     * from the KMC Key Management Service (KMS).
     *
     * @return The {@link Decrypter} object.
     * @throws KmcCryptoManagerException if error occurred in connecting to KMS.
     */
    public final Decrypter createDecrypter() throws KmcCryptoManagerException {
        return createCryptoObject(DECRYPTER_LIBRARY_CLASS);
    }

    /**
     * Creates a {@link Decrypter} which obtains the decryption key
     * from the specified keystore.
     *
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @param keyPass The password of the key to be retrieved.
     * @return The {@link Decrypter} object.
     * @throws KmcCryptoManagerException if error occurred in loading the keystore.
     */
    public final Decrypter createDecrypter(final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyPass) throws KmcCryptoManagerException {
        return createCryptoObject(DECRYPTER_LIBRARY_CLASS,
                keystoreLocation, keystorePass, keystoreType, null, keyPass);
    }

    /**
     * Creates a {@link IcvCreator} which uses the Message Digest algorithm for generating the ICV.
     * To specify a particular algorithm other than the default, set the Message Digest algorithm in
     * the KmcCryptoManager prior to calling this method.
     *
     * @return The {@link IcvCreator} object.
     * @throws KmcCryptoManagerException if the Message Digest algorithm is invalid.
     */
    public final IcvCreator createIcvCreator() throws KmcCryptoManagerException {
        return createCryptoObject(ICV_CREATOR_LIBRARY_CLASS);
    }

    /**
     * Creates a {@link IcvCreator} which uses the HMAC algorithm for generating the ICV.
     * It obtains the cryptographic key from the keystore or Key Management Service (KMS).
     * The retrieved key determines the algorithm to be used by HMAC.
     *
     * @param keyRef A string for identifying the key, i.e. the name of the key.
     * @return The {@link IcvCreator} object.
     * @throws KmcCryptoManagerException if error in retrieving the key.
     */
    public final IcvCreator createIcvCreator(final String keyRef) throws KmcCryptoManagerException {
        return createCryptoObject(ICV_CREATOR_LIBRARY_CLASS, keyRef);
    }

    /**
     * Creates a {@link IcvCreator} which uses the HMAC algorithm for generating the ICV.
     * During creation of the instance the symmetric key as specified in keyRef is retrieved
     * from the specified keystore.  However, the retrieved key does not determine the HMAC algorithm.
     * To specify a particular algorithm other than the default, set the HMAC algorithm
     * in the KmcCryptoManager prior to calling this constructor.
     * <p>
     * The keystorePass or keyPass can be null if not used by the keystore.
     * All keys in the keystore must have the same key password (keyPass) if a key password is used.
     * </p>
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @param keyRef A string for identifying the key, also known as alias.
     * @param keyPass The password of the key to be retrieved.
     * @return The {@link IcvCreator} object.
     * @throws KmcCryptoManagerException if error in retrieving the key.
     */
    public final IcvCreator createIcvCreator(final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyRef, final String keyPass) throws KmcCryptoManagerException {
        return createCryptoObject(ICV_CREATOR_LIBRARY_CLASS,
                keystoreLocation, keystorePass, keystoreType, keyRef, keyPass);
    }

    /**
     * Creates a {@link IcvVerifier} which does not need a cryptographic key
     * (for example, Message Digest algorithms are used)
     * or obtains the key from the KMC Key Management Service (KMS).
     * The metadata, associated with the data to be verified, provides all the information needed
     * to perform the verification of the data.
     *
     * @return The {@link IcvVerifier} object.
     * @throws KmcCryptoManagerException if error in retrieving the key.
     */
    public final IcvVerifier createIcvVerifier() throws KmcCryptoManagerException {
        return createCryptoObject(ICV_VERIFIER_LIBRARY_CLASS);
    }

    /**
     * Creates a {@link IcvVerifier} which obtains the
     * cryptographic key from the specified keystore.
     *
     * @param keystoreLocation The path to the location of the keystore.
     * @param keystorePass The password of the keystore.
     * @param keystoreType The keystore type.
     * @param keyPass The password of the key to be retrieved.
     * @return The {@link IcvVerifier} object.
     * @throws KmcCryptoManagerException if error in retrieving the key.
     */
    public final IcvVerifier createIcvVerifier(final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyPass) throws KmcCryptoManagerException {
        return createCryptoObject(ICV_VERIFIER_LIBRARY_CLASS,
                keystoreLocation, keystorePass, keystoreType, null, keyPass);
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private <T> T createCryptoObject(final String className) throws KmcCryptoManagerException {
        if (this.config == null) {
            String msg = "KmcCryptoManager has not been initialized.";
            logger.error(msg);
            throw new KmcCryptoManagerException(
                    KmcCryptoManagerErrorCode.NOT_INITIALIZED, msg, null);
        }
        Exception exception;
        Class<T> cryptoClass;
        try {
            cryptoClass = (Class<T>) Class.forName(className);
            Constructor<T> constructor = cryptoClass.getConstructor(KmcCryptoManager.class);
            return constructor.newInstance(this);
        } catch (ClassNotFoundException e) {
            exception = e;
        } catch (NoSuchMethodException e) {
            exception = e;
        } catch (SecurityException e) {
            exception = e;
        } catch (InstantiationException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        } catch (IllegalArgumentException e) {
            exception = e;
        } catch (InvocationTargetException e) {
            exception = e;
        }
        if (exception != null) {
            Throwable cause = exception;
            KmcCryptoManagerErrorCode errorCode = KmcCryptoManagerErrorCode.NULL_VALUE;
            String errorMsg = "Failed to create " + className + " instance: ";
            if (exception.getCause() instanceof KmcCryptoException) {
                KmcCryptoErrorCode code = ((KmcCryptoException) exception.getCause()).getErrorCode();
                if (code == KmcCryptoErrorCode.CRYPTO_KEY_ERROR) {
                    errorCode = KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR;
                } else if (code == KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR) {
                    errorCode = KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR;
                }
                errorMsg = errorMsg + exception.getCause();
                cause = exception.getCause();
            } else {
                if (exception.getCause() == null) {
                    errorMsg = errorMsg + exception;
                } else {
                    errorMsg = errorMsg + exception + ", cause = " + exception.getCause();
                }
            }
            logger.error(errorMsg);
            throw new KmcCryptoManagerException(errorCode, errorMsg, exception);
        }
        // unused but Eclipse has error without it.
        return null;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private <T> T createCryptoObject(final String className, final String keyRef)
            throws KmcCryptoManagerException {
        if (this.config == null) {
            String msg = "KmcCryptoManager has not been initialized.";
            logger.error(msg);
            throw new KmcCryptoManagerException(
                    KmcCryptoManagerErrorCode.NOT_INITIALIZED, msg, null);
        }
        Exception exception;
        Class<T> cryptoClass;
        try {
            cryptoClass = (Class<T>) Class.forName(className);
            Constructor<T> constructor = cryptoClass.getConstructor(KmcCryptoManager.class, String.class);
            return constructor.newInstance(this, keyRef);
        } catch (ClassNotFoundException e) {
            exception = e;
        } catch (NoSuchMethodException e) {
            exception = e;
        } catch (SecurityException e) {
            exception = e;
        } catch (InstantiationException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        } catch (IllegalArgumentException e) {
            exception = e;
        } catch (InvocationTargetException e) {
            exception = e;
        }
        if (exception != null) {
            Throwable cause = exception;
            KmcCryptoManagerErrorCode errorCode = KmcCryptoManagerErrorCode.NULL_VALUE;
            String errorMsg = "Failed to create " + className + " instance: ";
            if (exception.getCause() instanceof KmcCryptoException) {
                KmcCryptoException e2 = ((KmcCryptoException) exception.getCause());
                KmcCryptoErrorCode code = e2.getErrorCode();
                if (code == KmcCryptoErrorCode.CRYPTO_KEY_ERROR) {
                    errorCode = KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR;
                } else if (code == KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR) {
                    errorCode = KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR;
                } else if (code == KmcCryptoErrorCode.INVALID_INPUT_VALUE) {
                    // DS does not support macLength
                    if (e2.getMessage().contains("Digital Signature does not support macLength")) {
                        errorCode = KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR;
                    }
                }
                errorMsg = errorMsg + exception.getCause();
                cause = exception.getCause();
            } else {
                if (exception.getCause() == null) {
                    errorMsg = errorMsg + exception;
                } else {
                    errorMsg = errorMsg + exception + ", cause = " + exception.getCause();
                }
            }
            logger.error(errorMsg);
            throw new KmcCryptoManagerException(errorCode, errorMsg, cause);
        }
        // unused but Eclipse has error without it.
        return null;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private <T> T createCryptoObject(final String className,
            final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyPass)
                    throws KmcCryptoManagerException {
        if (this.config == null) {
            String msg = "KmcCryptoManager has not been initialized.";
            logger.error(msg);
            throw new KmcCryptoManagerException(
                    KmcCryptoManagerErrorCode.NOT_INITIALIZED, msg, null);
        }
        Exception exception;
        Class<T> cryptoClass;
        try {
            cryptoClass = (Class<T>) Class.forName(className);
            Constructor<T> constructor;
            constructor = cryptoClass.getConstructor(KmcCryptoManager.class,
                    String.class, String.class, String.class, String.class);
            return constructor.newInstance(this,
                    keystoreLocation, keystorePass, keystoreType, keyPass);
        } catch (ClassNotFoundException e) {
            exception = e;
        } catch (NoSuchMethodException e) {
            exception = e;
        } catch (SecurityException e) {
            exception = e;
        } catch (InstantiationException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        } catch (IllegalArgumentException e) {
            exception = e;
        } catch (InvocationTargetException e) {
            exception = e;
        }
        if (exception != null) {
            Throwable cause = exception.getCause();
            KmcCryptoManagerErrorCode errorCode = KmcCryptoManagerErrorCode.NULL_VALUE;
            String errorMsg = "Failed to create " + className + " instance: ";
            if (cause == null) {
                errorMsg = errorMsg + exception;
            } else if (cause instanceof KmcCryptoException) {
                KmcCryptoErrorCode code = ((KmcCryptoException) cause).getErrorCode();
                if (code == KmcCryptoErrorCode.CRYPTO_KEY_ERROR) {
                    errorCode = KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR;
                } else if (code == KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR) {
                    errorCode = KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR;
                }
                errorMsg = errorMsg + cause;
            } else {
                errorMsg = errorMsg + exception + ", cause = " + cause;
            }
            logger.error(errorMsg);
            if (cause instanceof KmcCryptoException) {
                throw new KmcCryptoManagerException(errorCode, errorMsg, cause);
            } else {
                throw new KmcCryptoManagerException(errorCode, errorMsg, exception);
            }
        }
        // unused but Eclipse has error without it.
        return null;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private <T> T createCryptoObject(final String className,
            final String keystoreLocation, final String keystorePass,
            final String keystoreType, final String keyRef, final String keyPass)
                    throws KmcCryptoManagerException {
        if (this.config == null) {
            String msg = "KmcCryptoManager has not been initialized.";
            logger.error(msg);
            throw new KmcCryptoManagerException(
                    KmcCryptoManagerErrorCode.NOT_INITIALIZED, msg, null);
        }
        Exception exception;
        Class<T> cryptoClass;
        try {
            cryptoClass = (Class<T>) Class.forName(className);
            Constructor<T> constructor;
            if (keyRef == null) {
                constructor = cryptoClass.getConstructor(KmcCryptoManager.class,
                    String.class, String.class, String.class, String.class);
                return constructor.newInstance(this, keystoreLocation, keystorePass, keystoreType, keyPass);
            } else {
                constructor = cryptoClass.getConstructor(KmcCryptoManager.class,
                    String.class, String.class, String.class, String.class, String.class);
                return constructor.newInstance(this, keystoreLocation, keystorePass, keystoreType, keyRef, keyPass);
            }
        } catch (ClassNotFoundException e) {
            exception = e;
        } catch (NoSuchMethodException e) {
            exception = e;
        } catch (SecurityException e) {
            exception = e;
        } catch (InstantiationException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        } catch (IllegalArgumentException e) {
            exception = e;
        } catch (InvocationTargetException e) {
            exception = e;
        }
        if (exception != null) {
            Throwable cause = exception;
            KmcCryptoManagerErrorCode errorCode = KmcCryptoManagerErrorCode.NULL_VALUE;
            String errorMsg = "Failed to create " + className + " instance: ";
            if (exception.getCause() instanceof KmcCryptoException) {
                KmcCryptoErrorCode code = ((KmcCryptoException) exception.getCause()).getErrorCode();
                if (code == KmcCryptoErrorCode.CRYPTO_KEY_ERROR) {
                    errorCode = KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR;
                } else if (code == KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR) {
                    errorCode = KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR;
                }
                errorMsg = errorMsg + exception.getCause();
                cause = exception.getCause();
            } else {
                if (exception.getCause() == null) {
                    errorMsg = errorMsg + exception;
                } else {
                    errorMsg = errorMsg + exception + ", cause = " + exception.getCause();
                }
            }
            logger.error(errorMsg);
            throw new KmcCryptoManagerException(errorCode, errorMsg, exception);
        }
        // unused but Eclipse has error without it.
        return null;
    }

    /**
     * Sets the Message Digest algorithm for creating integrity check value.
     * @param algorithm A Message Digest algorithm.
     * @throws KmcCryptoManagerException if the algorithm is not in the allowed list.
     */
    public final void setMessageDigestAlgorithm(final String algorithm)
            throws KmcCryptoManagerException {
        String error;
        String allowed = config.getProperty(CFG_ALLOWED_MESSAGE_DIGEST_ALGORITHMS);

        if (algorithm == null) {
            error = "Null algorithm value.";
        } else if (allowed == null) {
            error = "No " + CFG_ALLOWED_MESSAGE_DIGEST_ALGORITHMS + " in config.";
        } else {
            String[] algorithms = allowed.split(VALUE_SEPARATOR);
            for (String a : algorithms) {
                if (algorithm.equals(a)) {
                    config.setProperty(CFG_DEFAULT_MESSAGE_DIGEST_ALGORITHM, algorithm);
                    return;
                }
            }
            error = "Message Digest algorithm " + algorithm + " is not in the allowed list.";
        }
        logger.error(error);
        throw new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
    }

    /**
     * Returns the default Message Digest algorithm.
     * @return The default Message Digest algorithm.
     */
    public final String getMessageDigestAlgorithm() {
        String algorithm = config.getProperty(CFG_DEFAULT_MESSAGE_DIGEST_ALGORITHM);
        if (algorithm == null) {
            algorithm = DEFAULT_MESSAGE_DIGEST_ALGORITHM;
        }
        return algorithm;
    }

    /**
     * Sets the HMAC algorithm for creating integrity check value.
     * @param algorithm A HMAC algorithm.
     * @throws KmcCryptoManagerException if the algorithm is not in the allowed list.
     */
    public final void setHmacAlgorithm(final String algorithm)
            throws KmcCryptoManagerException {
        String error;
        String allowed = config.getProperty(CFG_ALLOWED_HMAC_ALGORITHMS);
        if (algorithm == null) {
            error = "Null algorithm value.";
        } else if (allowed == null) {
            error = "No allowed_hmac_algorithms in config.";
        } else {
            String[] algorithms = allowed.split(VALUE_SEPARATOR);
            for (String a : algorithms) {
                if (algorithm.equals(a)) {
                    config.setProperty(CFG_DEFAULT_HMAC_ALGORITHM, algorithm);
                    return;
                }
            }
            error = "HMAC algorithm " + algorithm + " is not in the allowed list.";
        }
        logger.error(error);
        throw new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
    }

    /**
     * Returns the default HMAC algorithm.
     * @return The default HMAC algorithm.
     */
    public final String getHmacAlgorithm() {
        String algorithm = config.getProperty(CFG_DEFAULT_HMAC_ALGORITHM);
        if (algorithm == null) {
            algorithm = DEFAULT_HMAC_ALGORITHM;
        }
        return algorithm;
    }

    /**
     * Sets the CMAC algorithm for creating integrity check value.
     * @param algorithm A CMAC algorithm.
     * @throws KmcCryptoManagerException if the algorithm is not in the allowed list.
     */
    public final void setCmacAlgorithm(final String algorithm)
            throws KmcCryptoManagerException {
        String error;
        String allowed = config.getProperty(CFG_ALLOWED_CMAC_ALGORITHMS);
        if (algorithm == null) {
            error = "Null algorithm value.";
        } else if (allowed == null) {
            error = "No allowed_cmac_algorithms in config.";
        } else {
            String[] algorithms = allowed.split(VALUE_SEPARATOR);
            for (String a : algorithms) {
                if (algorithm.equals(a)) {
                    config.setProperty(CFG_DEFAULT_CMAC_ALGORITHM, algorithm);
                    return;
                }
            }
            error = "CMAC algorithm " + algorithm + " is not in the allowed list.";
        }
        logger.error(error);
        throw new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
    }

    /**
     * Returns the default CMAC algorithm.
     * @return The default CMAC algorithm.
     */
    public final String getCmacAlgorithm() {
        String algorithm = config.getProperty(CFG_DEFAULT_CMAC_ALGORITHM);
        if (algorithm == null) {
            algorithm = DEFAULT_CMAC_ALGORITHM;
        }
        return algorithm;
    }

    /**
     * Sets the Digital Signature algorithm for creating integrity check value.
     * @param algorithm A Digital Signature algorithm.
     * @throws KmcCryptoManagerException if the algorithm is not in the allowed list.
     */
    public final void setDigitalSignatureAlgorithm(final String algorithm)
            throws KmcCryptoManagerException {
        String error;
        String allowed = config.getProperty(CFG_ALLOWED_DIGITAL_SIGNATURE_ALGORITHMS);
        if (algorithm == null) {
            error = "Null algorithm value.";
        } else if (allowed == null) {
            error = CFG_ALLOWED_DIGITAL_SIGNATURE_ALGORITHMS + " not found in config file " + DEFAULT_CRYPTO_CONFIG_FILE;
        } else {
            String[] algorithms = allowed.split(VALUE_SEPARATOR);
            for (String a : algorithms) {
                if (algorithm.equals(a)) {
                    config.setProperty(CFG_DEFAULT_DIGITAL_SIGNATURE_ALGORITHM, algorithm);
                    return;
                }
            }
            error = "digital signature algorithm " + algorithm + " is not in the allowed list.";
        }
        logger.error(error);
        throw new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
    }

    /**
     * Returns the default Digital Signature algorithm.
     * @return The default Digital Signature algorithm.
     */
    public final String getDigitalSignatureAlgorithm() {
        String algorithm = config.getProperty(CFG_DEFAULT_DIGITAL_SIGNATURE_ALGORITHM);
        if (algorithm == null) {
            algorithm = DEFAULT_DIGIAL_SIGNATURE_ALGORITHM;
        }
        return algorithm;
    }

    /**
     * Sets the cipher transformation for encryption.
     * @param transformation A cipher transformation.
     * @throws KmcCryptoManagerException if the transformation is not in the allowed list.
     */
    public final void setCipherTransformation(final String transformation)
            throws KmcCryptoManagerException {
        String error;
        if (transformation == null) {
            error = "Input transformation value is null.";
            logger.error(error);
            throw new KmcCryptoManagerException(
                    KmcCryptoManagerErrorCode.NULL_VALUE, error, null);
        }
        String[] parts = transformation.split("/");
        String allowedParam = "allowed_" + parts[0] + "_transformations";
        String allowed = config.getProperty(allowedParam);
        if (allowed == null) {
            error = "No " + allowedParam + " in config.";
        } else {
            String[] transformations = allowed.split(VALUE_SEPARATOR);
            for (String t : transformations) {
                if (transformation.equals(t)) {
                    config.setProperty("default_" + parts[0] + "_transformation", transformation);
                    return;
                }
            }
            error = transformation + " is not in the allowed list.";
        }
        logger.error(error);
        throw new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
    }

    /**
     * Returns the default cipher transformation of the specified encryption algorithm.
     * @param algorithm The encryption algorithm.
     * @return The default cipher transformation of the specified encryption algorithm.
     */
    public final String getCipherTransformation(final String algorithm) {
        String transformation = config.getProperty("default_" + algorithm + "_transformation");
        if (transformation == null) {
            transformation = defaultTransformations.get(algorithm);
        }
        return transformation;
    }

    /**
     * Returns the default MAC length.
     * @return The default MAC length for ICV or Tag length for AE.
     */
    public final int getAllowedMinMacLength() {
        String value = config.getProperty(CFG_ALLOWED_MIN_MAC_LENGTH);
        if (value == null || value.isEmpty()) {
            return DEFAULT_ALLOWED_MIN_MAC_LENGTH;
        } else {
            return Integer.parseInt(value);
        }
    }

    /**
     * Sets the allowed minimum MAC length.
     * @param minMacLength The allowed minimum MAC length for ICV or AE.
     *           Input "-1" to remove the minMacLength parameter and use the default.
     * @throws KmcCryptoManagerException if minMacLength is not an integer.
     */
    public final void setAllowedMinMacLength(final String minMacLength) throws KmcCryptoManagerException {
        try {
            setAllowedMinMacLength(Integer.parseInt(minMacLength));
        } catch (NumberFormatException e) {
            String error = "The allowed minimum MAC length (" + minMacLength + ") is not an integer.";
            logger.error(error);
            throw new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
        }
    }

    /**
     * Sets the allowed minimum MAC length in bits.
     * @param minMacLength The allowed minimum MAC length for ICV or AE.
     *           Input -1 to remove the minMacLength parameter and use the default.
     * @throws KmcCryptoManagerException if minMacLength is less than -1.
     */
    public final void setAllowedMinMacLength(final int minMacLength) throws KmcCryptoManagerException {
        if (minMacLength == -1) {
            config.remove(CFG_ALLOWED_MIN_MAC_LENGTH);
        } else if (minMacLength < -1) {
            String error = "The allowed minimum MAC length (" + minMacLength + ") cannot be negative.";
            logger.error(error);
            throw new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
        } else {
            config.setProperty(CFG_ALLOWED_MIN_MAC_LENGTH, String.valueOf(minMacLength));
        }
    }

    /**
     * Returns the requested MAC length in bits.
     * @return The requested MAC length (in bits) for ICV or Tag length for AE.
     *         -1 for the full length according to the crypto algorithm.
     */
    public final int getMacLength() {
        String value = config.getProperty(CFG_TRUNCATED_MAC_LENGTH);
        if (value == null || value.isEmpty()) {
            return -1;
        } else {
            return Integer.parseInt(value);
        }
    }

    /**
     * Sets the length of the MAC (in bits) to be created.
     * Input "-1" to remove the MAC length parameter.
     * @param macLength The MAC length to be created for ICV or Tag length by AE.
     * @throws KmcCryptoManagerException if macLength is less than allowed.
     */
    public final void setMacLength(final String macLength) throws KmcCryptoManagerException {
        try {
            setMacLength(Integer.parseInt(macLength));
        } catch (NumberFormatException e) {
            String error = "The specified MAC length " + macLength + " is not an integer.";
            logger.error(error);
            throw new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
        }
    }

    /**
     * Sets the length of the MAC (in bits) to be created.
     * Input -1 to remove the MAC length parameter.
     * @param macLength The MAC length to be created for ICV or Tag length by AE.
     * @throws KmcCryptoManagerException if macLength is less than allowed.
     */
    public final void setMacLength(final int macLength) throws KmcCryptoManagerException {
        if (macLength == -1) {
            config.remove(CFG_TRUNCATED_MAC_LENGTH);
        } else if (macLength >= getAllowedMinMacLength()) {
            if (macLength % 8 == 0) {
                config.setProperty(CFG_TRUNCATED_MAC_LENGTH, String.valueOf(macLength));
            } else {
                String error = "The requested MAC length (" + macLength
                        + " bits) is not multiple of 8.";
                logger.error(error);
                throw new KmcCryptoManagerException(
                        KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
            }
        } else {
            String error = "The requested MAC length " + macLength
                    + " is less than allowed minimum " + getAllowedMinMacLength();
            logger.error(error);
            throw new KmcCryptoManagerException(
                    KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, error, null);
        }
    }

    /**
     * Sets the SSO cookie for accessing CAM protected resources, such as the
     * KMC Crypto Service.
     * @param ssoCookie The string ssoCookieName=ssoToken obtained from
     *                  the access control manager.
     */
    public final void setSsoCookie(final String ssoCookie) {
        this.config.setProperty(CFG_SSO_COOKIE, ssoCookie);
    }

    /**
     * Returns the SSO cookie for accessing CAM protected resources, such as the
     * KMC Crypto Service.
     * @return A string in the form of ssoCookieName=ssoToken.
     */
    public final String getSsoCookie() {
        return this.config.getProperty(CFG_SSO_COOKIE);
    }

    private String getInputConfigDir(final String[] args) throws KmcCryptoManagerException {
        for (String arg : args) {
            if (arg.startsWith("-" + CFG_KMC_CRYPTO_CONFIG_DIR)) {
                String[] keyValue = arg.split("=");
                if (keyValue.length == 2) {
                    String dir = keyValue[1];
                    if (dir.endsWith("/")) {
                        dir = dir.substring(0, dir.length() - 1);
                    }
                    return dir;
                } else {
                    String msg = "Missing value for the " + CFG_KMC_CRYPTO_CONFIG_DIR + " arugment: " + arg;
                    logger.error(msg);
                    throw new KmcCryptoManagerException(
                            KmcCryptoManagerErrorCode.CONFIG_PARAMETER_NOT_FOUND, msg, null);
                }
            }
        }
        return null;
    }

}
