package gov.nasa.jpl.ammos.asec.kmc.cli;

import gov.nasa.jpl.ammos.asec.kmc.format.Utilities;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.SaCreate;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.SaDelete;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.SaExpire;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.SaKey;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.SaList;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.SaStart;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.SaStop;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.SaUpdate;
import gov.nasa.jpl.ammos.asec.kmc.cli.misc.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Main CLI launcher class
 */
@CommandLine.Command(name = "kmc-sa-mgmt", subcommands = {SaList.class,
        SaCreate.class,
        SaUpdate.class,
        SaDelete.class,
        SaKey.class,
        SaStart.class,
        SaStop.class,
        SaExpire.class}, description = "KMC Security Association Management CLI", mixinStandardHelpOptions = true,
        versionProvider = Version.class)
public class Main {

    private static Logger LOG = LoggerFactory.getLogger(Main.class);

    /**
     * Main
     *
     * @param args args
     */
    public static void main(String... args) {
        int exit =
                new CommandLine(new Main()).setExecutionExceptionHandler(new PrintExceptionMessageHandler()).execute(args);
        System.exit(exit);
    }
}

/**
 * Exception handler
 */
class PrintExceptionMessageHandler implements CommandLine.IExecutionExceptionHandler {
    private static Logger LOG = LoggerFactory.getLogger(Main.class);

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult parseResult) throws Exception {
        commandLine.getErr().println(commandLine.getColorScheme().errorText(ex.getMessage()));
        LOG.error(Utilities.sanitizeForLog(ex.getMessage()), ex);
        return 1;
    }
}
