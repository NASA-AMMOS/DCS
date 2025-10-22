package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnValidator;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.cli.misc.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Scanner;

/**
 * Re/key a security association
 */
@CommandLine.Command(name = "key", description = "Key or rekey a Security Association", mixinStandardHelpOptions =
        true, versionProvider = Version.class)
public class SaKey extends BaseCliApp {

    private static Logger LOG = LoggerFactory.getLogger(SaExpire.class);

    @CommandLine.Option(names = "--spi", required = true, description = "security parameter index")
    private int spi;

    @CommandLine.Option(names = "--scid", required = true, description = "spacecraft id")
    private short scid;

    @CommandLine.ArgGroup(exclusive = false)
    private OptionalArgs optionalArgs;
    private byte[]       ecsBytes;
    private byte[]       acsBytes;
    private boolean      updateAuth;
    private boolean      updateEnc;

    static class OptionalArgs {
        @CommandLine.Option(names = "--ekid", required = false, description = "encryption key ID")
        String ekid;

        @CommandLine.Option(names = "--ecs", required = false, description = "encryption cypher suite (hex string)")
        String ecs;

        @CommandLine.Option(names = "--akid", required = false, description = "authentication key ID")
        String akid;

        @CommandLine.Option(names = "--acs", required = false, description = "authentication cypher suite")
        String acs;

        @CommandLine.Option(names = "-y", description = "Rekey without confirmation")
        Boolean silent;
    }

    @Override
    public Integer call() throws Exception {
        if (optionalArgs == null || (optionalArgs.akid == null && optionalArgs.ekid == null)) {
            throwEx("Missing parameters: Must specify either '--ekid', '--akid', or both");
        } else {
            if (optionalArgs.ekid != null) {
                checkEncParams();
            }
            if (optionalArgs.akid != null) {
                checkAuthParams();
            }
        }
        try (IKmcDao dao = getDao()) {
            ISecAssn sa = dao.getSa(new SpiScid(spi, scid), frameType);
            if (sa == null) {
                throwEx(String.format("Error keying SA, %s %d/%d does not exist", frameType.name(), spi, scid));
            }
            boolean skip = false;
            if (optionalArgs != null && optionalArgs.silent == null) {
                console(String.format("%s updating keys on %s, do you wish to continue? y/n", System.getProperty(
                        "user.name"), sa));
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    try {
                        String confirm = scanner.next();
                        if (confirm.equals("y")) {
                            break;
                        } else if (confirm.equals("n")) {
                            console(String.format("Skipping %s", sa));
                            skip = true;
                            break;
                        }
                    } catch (Exception e) {
                        warn("Unrecognized input");
                    }
                }
            }
            if (!skip) {
                if (updateAuth) {
                    sa.setAkid(optionalArgs.akid);
                    sa.setAcs(acsBytes);
                    sa.setAcsLen((short) 1);
                }
                if (updateEnc) {
                    sa.setEkid(optionalArgs.ekid);
                    sa.setEcs(ecsBytes);
                    sa.setEcsLen((short) 1);
                }
                dao.updateSa(sa);
                console(String.format("%s updated keys on %s", System.getProperty("user.name"),
                        sa));
            }
        }

        return 0;
    }

    private void checkAuthParams() throws KmcException {
        acsBytes = SecAssnValidator.verifyAuth(optionalArgs.akid, optionalArgs.acs);
        updateAuth = acsBytes != null;
    }

    private void checkEncParams() throws KmcException {
        ecsBytes = SecAssnValidator.verifyEnc(optionalArgs.ekid, optionalArgs.ecs);
        updateEnc = ecsBytes != null;
    }

    /**
     * Main
     *
     * @param args args
     */
    public static void main(String... args) {
        int exit = new CommandLine(new SaKey()).execute(args);
        System.exit(exit);
    }
}
