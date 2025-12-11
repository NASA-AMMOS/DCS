package gov.nasa.jpl.ammos.asec.kmc.sadb;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;
import java.util.HashMap;
import java.util.Map;

/**
 * KMC DAO provider
 */
public class DaoBuilder {

    private static final String CONN_FORMAT = "jdbc:mariadb://%s:%s/%s";
    private static final Logger LOG         = LoggerFactory.getLogger(DaoBuilder.class);

    private final Map<String, String> params = new HashMap<>();

    private boolean useMtls;
    private String  connString;
    private String  host;
    private String  port;
    private String  schema;
    private String  user;
    private String  pass;


    /**
     * Use TLS. The truststore is configured at the JVM level. Either set it before running the application or override
     * it using the flag.
     *
     * @param truststore     absolute path to truststore
     * @param truststorePass truststore password
     * @param overrideJvm    override the JVM's truststore using system properties
     * @return this builder
     * @throws KmcException exception
     */
    public DaoBuilder useTls(final String truststore, final String truststorePass, final boolean overrideJvm) throws KmcException {
        if (truststore == null || truststore.isEmpty() || truststorePass == null || truststorePass.isEmpty()) {
            throw new KmcException("When TLS is enabled, the " + "truststore and truststore password must be " +
                    "configured");
        }
        params.put("sslMode", "verify-full");
        params.put("trustStore", truststore);
        params.put("trustStorePassword", truststorePass);
        if (overrideJvm) {
            System.setProperty("javax.net.ssl.trustStore", truststore);
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePass);

        }
        //Override the JVM trustStoreType option if it is set wrong for our filetype!
        if (truststore.endsWith("p12")) {
            params.put("trustStoreType", "PKCS12");
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");

        } else if (truststore.endsWith("jks")) {
            params.put("trustStoreType", "JKS");
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        }
        return this;
    }

    /**
     * Use Mutual TLS
     *
     * @param keystore     absolute path to keystore
     * @param keystorePass keystore password
     * @return this builder
     * @throws KmcException exception
     */
    public DaoBuilder useMtls(final String keystore, final String keystorePass) throws KmcException {
        if (keystore == null || keystore.isEmpty() || keystorePass == null || keystorePass.isEmpty()) {
            throw new KmcException("When TLS is enabled, the keystore " + "and keystore password must be " +
                    "configured");
        }
        params.put("keyStore", keystore);
        params.put("keyStorePassword", keystorePass);
        if (keystore.endsWith("p12")) {
            params.put("keyStoreType", "PKCS12");
        } else if (keystore.endsWith("jks")) {
            params.put("keyStoreType", "JKS");
        } else if (keystore.endsWith("bcfks")) {
            //Special case, add BCFIPS to the providers to support out bcfks keystore   
            //Set Bouncy Castle FIPS to the first java security provider when this class is loaded, as it has
            // dependencies that require it to be tried first
            LOG.info("DaoBuilder detected we are in FIPS mode, adding BCFIPS to Security provider list");
            Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
            Security.insertProviderAt(new BouncyCastleJsseProvider("fips:BCFIPS"), 2);

            params.put("keyStoreType", "BCFKS");
        }
        this.useMtls = true;
        return this;
    }

    /**
     * Set connection string
     *
     * @param connString connection string
     * @return this builder
     */
    public DaoBuilder setConnString(final String connString) {
        this.connString = connString;
        return this;
    }

    /**
     * Set user
     *
     * @param user username
     * @return this builder
     */
    public DaoBuilder setUser(final String user) {
        this.user = user;
        return this;
    }

    /**
     * Set password
     *
     * @param pass password
     * @return this builder
     */
    public DaoBuilder setPass(final String pass) {
        this.pass = pass;
        return this;
    }

    /**
     * Set host
     *
     * @param host hostname
     * @return this builder
     */
    public DaoBuilder setHost(final String host) {
        this.host = host;
        return this;
    }

    /**
     * Set port
     *
     * @param port port number
     * @return this builder
     */
    public DaoBuilder setPort(final String port) {
        this.port = port;
        return this;
    }

    /**
     * Set schema name
     *
     * @param schema db schema name
     * @return this builder
     */
    public DaoBuilder setSchema(final String schema) {
        this.schema = schema;
        return this;
    }


    /**
     * Create and return a KMC DAO
     *
     * @return KMC DAO
     * @throws KmcException kmc ex
     */
    public IKmcDao build() throws KmcException {
        try {
            String url;
            if (connString != null) {
                url = connString;
            } else {
                if (host == null || port == null || schema == null) {
                    throw new KmcException("Must provide either full connection string, or the host, port, and " +
                            "schema, to connect to database");
                }
                url = String.format(CONN_FORMAT, host, port, schema);
            }

            String pass;
            if (useMtls) {
                pass = "";
            } else {
                pass = this.pass;
            }

            StringBuilder paramBuilder = new StringBuilder();
            boolean       first        = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    paramBuilder.append("&");
                }
                paramBuilder.append(entry.getKey());
                paramBuilder.append("=");
                paramBuilder.append(entry.getValue());
                first = false;
            }
            String paramString = paramBuilder.toString();
            if (!paramString.isEmpty()) {
                url += "?" + paramString;
            }

            return new KmcDao(user, pass, url);
        } catch (Exception e) {
            LOG.error("Error initializing application, please check database settings", e);
            throw new KmcException("Error initializing application, please check database settings", e);
        }
    }
}
