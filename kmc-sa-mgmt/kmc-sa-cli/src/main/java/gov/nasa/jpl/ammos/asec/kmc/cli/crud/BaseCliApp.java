package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.sadb.DaoFactory;
import gov.nasa.jpl.ammos.asec.kmc.sadb.config.Config;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Base class for CLI apps
 */
@CommandLine.Command
abstract class BaseCliApp implements Callable<Integer> {

    static {
        java.io.InputStream  is = BaseCliApp.class.getClassLoader().getResourceAsStream("kmc.properties");
        java.util.Properties p  = new Properties();
        try {
            p.load(is);

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        DEFAULT_KMC_HOME = p.getProperty("DEFAULT_KMC_ROOT") + "/kmc-crypto-client";
    }

    /**
     * The home directory of the KMC Client software.  Default of KMC_HOME is default is defined in kmc.properties
     */
    public static final String DEFAULT_KMC_HOME;

    /**
     * Logger
     */
    protected static final Logger LOG = LoggerFactory.getLogger(BaseCliApp.class);

    /**
     * CLI spec
     */
    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    /**
     * Frame type
     */
    @CommandLine.Option(names = "--type", required = false, defaultValue = "TC", description = "frame type. TC " +
            "(default), TM, AOS, or ALL", converter = FrameTypeConverter.class)
    public FrameType frameType = FrameType.TC;

    /**
     * Constructor
     */
    public BaseCliApp() {

    }

    /**
     * Log message to console
     *
     * @param message message
     */
    public void console(String message) {
        spec.commandLine().getOut().println(message);
        LOG.info(message);
    }

    /**
     * Log message to error
     *
     * @param message message
     */
    public void error(String message) {
        spec.commandLine().getErr().println(message);
        LOG.error(message);
    }

    /**
     * Log exception to error
     *
     * @param message message
     * @param e       exception
     */
    public void error(String message, Exception e) {
        spec.commandLine().getErr().println(message);
        LOG.error(message, e);
    }

    /**
     * Log message to warn
     *
     * @param message message
     */
    public void warn(String message) {
        spec.commandLine().getErr().println(message);
        LOG.warn(message);
    }

    /**
     * Get a KMC DAO using either the configured authentication, or provided credentials
     *
     * @return KMC DAO
     */
    public IKmcDao getDao() {
        try {
            Config  cfg = new Config(DEFAULT_KMC_HOME + "/etc", "kmc-sa-mgmt.properties");
            IKmcDao dao = DaoFactory.getDao(cfg);
            dao.init();
            return dao;
        } catch (Exception e) {
            LOG.error("Error initializing application, please check database settings", e);
            throw new CommandLine.ExecutionException(spec.commandLine(), "Error initializing application, please " +
                    "check database settings");
        }
    }

    /**
     * Convert a Hex string to a byte array
     *
     * @param hexString hex string
     * @param msg       message
     * @return byte array
     * @throws CommandLine.ParameterException param ex
     */
    protected byte[] convertHexToBytes(String hexString, String msg) throws CommandLine.ParameterException {
        try {
            String abm = hexString.toLowerCase().replaceFirst("^0x", "");
            return Hex.decodeHex(abm);
        } catch (DecoderException e) {
            throwEx(msg);
        }
        return null;
    }

    /**
     * Throw a (runtime) parameter exception
     *
     * @param message error message
     * @throws CommandLine.ParameterException param ex
     */
    protected void throwEx(String message) throws CommandLine.ParameterException {
        throw new CommandLine.ParameterException(spec.commandLine(), message);
    }

    private static class FrameTypeConverter implements CommandLine.ITypeConverter<FrameType> {

        @Override
        public FrameType convert(String value) throws Exception {
            FrameType type = FrameType.fromString(value);
            if (type == FrameType.UNKNOWN) {
                throw new KmcException("Unknown frame type: " + value);
            }
            return type;
        }
    }

}
