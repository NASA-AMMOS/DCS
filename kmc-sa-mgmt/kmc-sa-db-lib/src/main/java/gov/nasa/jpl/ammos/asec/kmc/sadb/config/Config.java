package gov.nasa.jpl.ammos.asec.kmc.sadb.config;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.BasePathLocationStrategy;
import org.apache.commons.configuration2.io.ClasspathLocationStrategy;
import org.apache.commons.configuration2.io.CombinedLocationStrategy;
import org.apache.commons.configuration2.io.FileLocationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for KMC CLI
 */
public class Config extends CompositeConfiguration {

    public static final String ENV_VAR_FOUND_OVERRIDING = "{} env var found, overriding {}";
    private static boolean sysOverridesEnabled = true;

    /**
     * System overrides enabled
     *
     * @param enabled enabled or disabled
     */
    public static void setSysOverridesEnabled(boolean enabled) {
        sysOverridesEnabled = enabled;
    }

    private static final Logger LOG                = LoggerFactory.getLogger(Config.class);
    // Config keys
    /**
     * DB auth user
     */
    public static final  String DB_AUTH_USER       = "db.auth.user";
    /**
     * DB auth pass
     */
    public static final  String DB_AUTH_PASS       = "db.auth.pass";
    /**
     * DB connection string
     */
    public static final  String DB_CONN_STRING     = "db.conn.string";
    /**
     * DB host
     */
    public static final  String DB_HOST            = "db.host";
    /**
     * DB port
     */
    public static final  String DB_PORT            = "db.port";
    /**
     * DB schema
     */
    public static final  String DB_SCHEMA          = "db.schema";
    /**
     * DB keystore
     */
    public static final  String DB_KEYSTORE        = "db.keystore";
    /**
     * DB keystore pass
     */
    public static final  String DB_KEYSTORE_PASS   = "db.keystore.pass";
    /**
     * DB truststore
     */
    public static final  String DB_TRUSTSTORE      = "db.truststore";
    /**
     * DB truststore pass
     */
    public static final  String DB_TRUSTSTORE_PASS = "db.truststore.pass";
    /**
     * DB TLS
     */
    public static final  String DB_TLS             = "db.tls";
    /**
     * DB mTLS
     */
    public static final  String DB_MTLS            = "db.mtls";

    // Environment variable overrides
    /**
     * DB user override
     */
    public static final String ENV_DB_USER            = "DB_USER";
    /**
     * DB pass override
     */
    public static final String ENV_DB_PASS            = "DB_PASS";
    /**
     * DB connection string override
     */
    public static final String ENV_DB_CONN_STRING     = "DB_CONN_STRING";
    /**
     * DB host override
     */
    public static final String ENV_DB_HOST            = "DB_HOST";
    /**
     * DB port override
     */
    public static final String ENV_DB_PORT            = "DB_PORT";
    /**
     * DB schema override
     */
    public static final String ENV_DB_SCHEMA          = "DB_SCHEMA";
    /**
     * DB keystore override
     */
    public static final String ENV_DB_KEYSTORE        = "DB_KEYSTORE";
    /**
     * DB keystore pass override
     */
    public static final String ENV_DB_KEYSTORE_PASS   = "DB_KEYSTORE_PASS";
    /**
     * DB truststore override
     */
    public static final String ENV_DB_TRUSTSTORE      = "DB_TRUSTSTORE";
    /**
     * DB truststore pass override
     */
    public static final String ENV_DB_TRUSTSTORE_PASS = "DB_TRUSTSTORE_PASS";
    /**
     * DB TLS override
     */
    public static final String ENV_DB_TLS             = "DB_TLS";
    /**
     * DB mTLS override
     */
    public static final String ENV_DB_MTLS            = "DB_MTLS";
    /**
     * None string constant
     */
    public static final String NONE                   = "none";
    /**
     * Config override
     */
    public static final String KMC_OVERRIDE_CONFIG    = "KMC_OVERRIDE_CONFIG";

    private String  user;
    private String  pass;
    private String  conn;
    private String  host;
    private String  port;
    private String  schema;
    private String  keystore;
    private String  keystorePass;
    private String  truststore;
    private String  truststorePass;
    private Boolean useTls;
    private Boolean useMtls;
    private Boolean overrideJvmTruststore = true;

    /**
     * Constructor
     *
     * @param basepath base path
     * @param filename file name
     */
    public Config(final String basepath, final String filename) {
        super();
        Parameters parameters = new Parameters();
        IEnvProvider envProvider = System.getProperty("KMC_UNIT_TEST") != null ? new PropsEnvProvider() : new EnvProvider();

        try {
            List<FileLocationStrategy> strategies = Arrays.asList(new BasePathLocationStrategy(),
                    new ClasspathLocationStrategy());
            FileLocationStrategy strategy = new CombinedLocationStrategy(strategies);
            FileBasedConfigurationBuilder<PropertiesConfiguration> asecConfig =
                    new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                            .configure(
                                    parameters
                                            .properties()
                                            .setLocationStrategy(strategy)
                                            .setBasePath(basepath)
                                            .setFileName(filename));

            FileBasedConfigurationBuilder<PropertiesConfiguration> builderDefaults =
                    new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                            .configure(
                                    parameters
                                            .properties()
                                            .setLocationStrategy(strategy)
                                            .setFileName("etc/defaults.properties"));

            FileBasedConfigurationBuilder<PropertiesConfiguration> builderConfig =
                    new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                            .configure(
                                    parameters
                                            .properties()
                                            .setLocationStrategy(strategy)
                                            .setFileName("etc/" + filename));

            FileBasedConfigurationBuilder<PropertiesConfiguration> overrideConfig =
                    new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                            .configure(
                                    parameters
                                            .properties()
                                            .setLocationStrategy(strategy)
                                            .setFileName(filename));


            if (sysOverridesEnabled) {
                LOG.info("{} env var found, making {} primary config", KMC_OVERRIDE_CONFIG,
                        envProvider.getEnv(KMC_OVERRIDE_CONFIG));
                FileBasedConfigurationBuilder<PropertiesConfiguration> devOverride =
                        new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                                .configure(
                                        parameters
                                                .properties()
                                                .setLocationStrategy(strategy)
                                                .setFileName(envProvider.getEnv(KMC_OVERRIDE_CONFIG)));
                this.addConfiguration(devOverride.getConfiguration());
            }

            if (System.getProperty("KMC_UNIT_TEST") == null) {
                try {
                    LOG.debug("Loading ASEC config under {}/{}", basepath, filename);
                    this.addConfiguration(asecConfig.getConfiguration());
                } catch (ConfigurationException e) {
                    LOG.warn("ASEC config not found under {}/{}", basepath, filename);
                }
            }

            try {
                LOG.debug("Loading config from classpath under ./etc/{}", filename);
                this.addConfiguration(builderConfig.getConfiguration());
            } catch (ConfigurationException e) {
                LOG.warn("Classpath config not found under ./etc/{}", filename);
                try {
                    LOG.debug("Loading config from classpath under ./{}", filename);
                    this.addConfiguration(overrideConfig.getConfiguration());
                } catch (ConfigurationException e1) {
                    LOG.warn("Classpath config not found under ./{}, {}", filename, e.getMessage());
                }
            }
            LOG.debug("Loading classpath default config under ./etc/defaults.properties");
            this.addConfiguration(builderDefaults.getConfiguration());

        } catch (ConfigurationException e) {
            throw new RuntimeException("Configuration error", e);
        }

        this.user = getString(DB_AUTH_USER);
        if (envProvider.getEnv(ENV_DB_USER) != null) {
            LOG.info("{} env var found, overriding {} user", ENV_DB_USER, DB_AUTH_USER);
            this.user = envProvider.getEnv(ENV_DB_USER);
        }
        this.pass = getString(DB_AUTH_PASS);
        if (envProvider.getEnv(ENV_DB_PASS) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_PASS, DB_AUTH_PASS);
            this.pass = envProvider.getEnv(ENV_DB_PASS);
        }
        this.conn = getString(DB_CONN_STRING);
        if (envProvider.getEnv(ENV_DB_CONN_STRING) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_CONN_STRING, DB_CONN_STRING);
            this.conn = envProvider.getEnv(ENV_DB_CONN_STRING);
        }
        this.host = getString(DB_HOST);
        if (envProvider.getEnv(ENV_DB_HOST) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_HOST, DB_HOST);
            this.host = envProvider.getEnv(ENV_DB_HOST);
        }
        this.port = getString(DB_PORT);
        if (envProvider.getEnv(ENV_DB_PORT) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_PORT, DB_PORT);
            this.port = envProvider.getEnv(ENV_DB_PORT);
        }
        this.schema = getString(DB_SCHEMA);
        if (envProvider.getEnv(ENV_DB_SCHEMA) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_SCHEMA, DB_SCHEMA);
            this.schema = envProvider.getEnv(ENV_DB_SCHEMA);
        }
        this.keystore = getString(DB_KEYSTORE);
        if (envProvider.getEnv(ENV_DB_KEYSTORE) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_KEYSTORE, DB_KEYSTORE);
            this.keystore = envProvider.getEnv(ENV_DB_KEYSTORE);
        }
        this.keystorePass = getString(DB_KEYSTORE_PASS);
        if (envProvider.getEnv(ENV_DB_KEYSTORE_PASS) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_KEYSTORE_PASS, DB_KEYSTORE_PASS);
            this.keystorePass = envProvider.getEnv(ENV_DB_KEYSTORE_PASS);
        }
        this.truststore = getString(DB_TRUSTSTORE);
        if (envProvider.getEnv(ENV_DB_TRUSTSTORE) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_TRUSTSTORE, DB_TRUSTSTORE);
            this.keystore = envProvider.getEnv(ENV_DB_TRUSTSTORE);
        }
        this.truststorePass = getString(DB_TRUSTSTORE_PASS);
        if (envProvider.getEnv(ENV_DB_TRUSTSTORE_PASS) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_TRUSTSTORE_PASS, DB_TRUSTSTORE_PASS);
            this.keystore = envProvider.getEnv(ENV_DB_TRUSTSTORE_PASS);
        }
        this.useTls = getBoolean(DB_TLS);
        if (envProvider.getEnv(ENV_DB_TLS) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_TLS, DB_TLS);
            this.useTls = envProvider.getEnv(ENV_DB_TLS).equalsIgnoreCase("true");
        }
        this.useMtls = getBoolean(DB_MTLS);
        if (envProvider.getEnv(ENV_DB_MTLS) != null) {
            LOG.info(ENV_VAR_FOUND_OVERRIDING, ENV_DB_MTLS, DB_MTLS);
            this.useMtls = envProvider.getEnv(ENV_DB_MTLS).equalsIgnoreCase("true");
        }
    }

    /**
     * Get user
     *
     * @return user
     */
    public String getUser() {
        return user;
    }

    /**
     * Set user
     *
     * @param user user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Get pass
     *
     * @return pass
     */
    public String getPass() {
        return pass;
    }

    /**
     * Set pass
     *
     * @param pass pass
     */
    public void setPass(String pass) {
        this.pass = pass;
    }

    /**
     * Get conn string
     *
     * @return conn string
     */
    public String getConn() {
        return conn;
    }

    /**
     * Set conn string
     *
     * @param conn conn string
     */
    public void setConn(String conn) {
        this.conn = conn;
    }

    /**
     * Get host
     *
     * @return host
     */
    public String getHost() {
        return host;
    }

    /**
     * Set host
     *
     * @param host host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get port
     *
     * @return port
     */
    public String getPort() {
        return port;
    }

    /**
     * Set port
     *
     * @param port port
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Get schema
     *
     * @return schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Set schema
     *
     * @param schema schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Get keystore
     *
     * @return keystore
     */
    public String getKeystore() {
        return keystore;
    }

    /**
     * Set keystore
     *
     * @param keystore keystore
     */
    public void setKeystore(String keystore) {
        this.keystore = keystore;
    }

    /**
     * Get keystore pass
     *
     * @return keystore pass
     */
    public String getKeystorePass() {
        return keystorePass;
    }

    /**
     * Set keystore pass
     *
     * @param keystorePass keystore pass
     */
    public void setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
    }

    /**
     * Get trust store
     *
     * @return trust store
     */
    public String getTruststore() {
        return truststore;
    }

    /**
     * Set trust store
     *
     * @param truststore trust store
     */
    public void setTruststore(String truststore) {
        this.truststore = truststore;
    }

    /**
     * Get trust store pass
     *
     * @return trust store pass
     */
    public String getTruststorePass() {
        return truststorePass;
    }

    /**
     * Set trust store pass
     *
     * @param truststorePass trust store pass
     */
    public void setTruststorePass(String truststorePass) {
        this.truststorePass = truststorePass;
    }

    /**
     * Get use TLS
     *
     * @return use TLS
     */
    public Boolean getUseTls() {
        return useTls;
    }

    /**
     * Set use TLS
     *
     * @param useTls use TLS
     */
    public void setUseTls(Boolean useTls) {
        this.useTls = useTls;
    }

    /**
     * Get use mTLS
     *
     * @return use mTLS
     */
    public Boolean getUseMtls() {
        return useMtls;
    }

    /**
     * Set use mTLS
     *
     * @param useMtls use mTLS
     */
    public void setUseMtls(Boolean useMtls) {
        this.useMtls = useMtls;
    }

    /**
     * Get override JVM trust store
     *
     * @return override JVM trust store
     */
    public Boolean getOverrideJvmTruststore() {
        return overrideJvmTruststore;
    }

    /**
     * Set override JVM trust store
     *
     * @param overrideJvmTruststore override JVM trust store
     */
    public void setOverrideJvmTruststore(Boolean overrideJvmTruststore) {
        this.overrideJvmTruststore = overrideJvmTruststore;
    }
}
