package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.cli.misc.Version;
import gov.nasa.jpl.ammos.asec.kmc.format.IOutput;
import gov.nasa.jpl.ammos.asec.kmc.format.SaCsvOutput;
import gov.nasa.jpl.ammos.asec.kmc.format.SaJsonOutput;
import gov.nasa.jpl.ammos.asec.kmc.format.SaMysqlOutput;
import picocli.CommandLine;

import java.util.List;
import java.util.stream.Collectors;

/**
 * List Security Assocations
 */
@CommandLine.Command(name = "list", description = "List security associations", mixinStandardHelpOptions = true,
        versionProvider = Version.class)
public class SaList extends BaseCliApp {
    @CommandLine.Option(names = "--spi", description = "Filter by security parameter index")
    private Integer spi;
    @CommandLine.Option(names = "--scid", description = "Filter by spacecraft ID")
    private Short   scid;
    @CommandLine.Option(names = "--vcid", description = "Filter by virtual channel ID")
    private Byte    vcid;

    @CommandLine.ArgGroup(exclusive = true)
    private Output output;

    static class Output {
        @CommandLine.Option(names = {"-e", "--extended"}, description = "extended CSV format")
        private boolean extended;
        @CommandLine.Option(names = "--mysql", description = "MySQL-like key-value format")
        private boolean mysql;
        @CommandLine.Option(names = "--json", description = "JSON format")
        private boolean json;
    }

    @CommandLine.ArgGroup
    private ExclusiveActiveInactive filter;

    static class ExclusiveActiveInactive {
        @CommandLine.Option(names = "--active", description = "Only list active SAs")
        private boolean activeOnly;
        @CommandLine.Option(names = "--inactive", description = "Only list inactive SAs")
        private boolean inactiveOnly;
    }

    @Override
    public Integer call() throws Exception {
        try (IKmcDao dao = getDao()) {
            List<ISecAssn> sas = null;
            if (filter != null) {
                if (filter.activeOnly) {
                    sas = dao.getSas(frameType).stream().filter(secAssn -> secAssn.getSaState() == 3).collect(Collectors.toList());
                } else if (filter.inactiveOnly) {
                    sas = dao.getSas(frameType).stream().filter(secAssn -> secAssn.getSaState() != 3).collect(Collectors.toList());
                }
            } else {
                sas = dao.getSas(frameType);
            }
            if (spi != null && sas != null) {
                sas = sas.stream().filter(secAssn -> secAssn.getSpi().equals(spi)).toList();
            }
            if (scid != null && sas != null) {
                sas = sas.stream().filter(secAssn -> secAssn.getScid().equals(scid)).toList();
            }
            if (vcid != null && sas != null) {
                sas = sas.stream().filter(secAssn -> vcid.equals(secAssn.getVcid())).toList();
            }

            IOutput out = getOutput();
            out.print(spec.commandLine().getOut(), sas);
        }

        return 0;
    }

    private IOutput getOutput() {
        if (output != null) {
            if (output.extended) {
                return new SaCsvOutput(true);
            } else if (output.json) {
                return new SaJsonOutput();
            } else if (output.mysql) {
                return new SaMysqlOutput();
            }
        }
        return new SaCsvOutput(false);
    }

    /**
     * Main
     *
     * @param args args
     */
    public static void main(String... args) {
        int exitCode = new CommandLine(new SaList()).execute(args);
        System.exit(exitCode);
    }
}
