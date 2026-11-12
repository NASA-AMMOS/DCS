package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.format.Utilities;
import gov.nasa.jpl.ammos.asec.kmc.cli.misc.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Scanner;

/**
 * Delete SAs
 */
@CommandLine.Command(name = "delete", description = "Delete a Security Association", mixinStandardHelpOptions = true,
        versionProvider = Version.class)
public class SaDelete extends BaseCliApp {
    private static Logger LOG = LoggerFactory.getLogger(SaDelete.class);

    @CommandLine.Option(names = "--spi", required = true, description = "security parameter index")
    private int[] spi;

    @CommandLine.Option(names = "--scid", required = true, description = "spacecraft id")
    private short scid;

    @CommandLine.Option(names = "-y", description = "Delete without confirmation")
    private Boolean silent;

    @Override
    public Integer call() throws Exception {
        int exit = 0;
        try (IKmcDao dao = getDao()) {
            for (int s : spi) {
                SpiScid  id = new SpiScid(s, scid);
                ISecAssn sa = dao.getSa(id, frameType);
                if (sa == null) {
                    warn(String.format("Error deleting: SA %d/%d does not exist, skipping", s, scid));
                    exit = 1;
                    continue;
                }
                boolean skip = false;
                if (silent == null) {
                    console(String.format("%s is deleting SA %d/%d, do you wish to continue? y/n", System.getProperty(
                            "user.name"), s, scid));
                    Scanner scanner = new Scanner(System.in);
                    while (true) {
                        try {
                            String confirm = scanner.next();
                            if (confirm.equals("y")) {
                                break;
                            } else if (confirm.equals("n")) {
                                console(String.format("Skipping %d/%d", s, scid));
                                skip = true;
                                break;
                            }
                        } catch (Exception e) {
                            warn("Unrecognized input");
                        }
                    }
                }
                if (!skip) {
                    try {
                        dao.deleteSa(id, frameType);
                    } catch (KmcException e) {
                        LOG.error(Utilities.sanitizeForLog(e.getMessage()));
                        return 1;
                    }
                    console(String.format("%s has deleted SA %d/%d", System.getProperty("user.name"), s, scid));
                }
            }
        }

        return exit;
    }

    /**
     * Main
     *
     * @param args args
     */
    public static void main(String... args) {
        int exit = new CommandLine(new SaDelete()).execute(args);
        System.exit(exit);
    }
}
