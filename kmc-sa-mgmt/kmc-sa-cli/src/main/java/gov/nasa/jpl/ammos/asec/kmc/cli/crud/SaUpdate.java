package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnValidator;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IDbSession;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.opts.OptionalOptions;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.opts.SaUpdateSingle;
import gov.nasa.jpl.ammos.asec.kmc.cli.misc.Version;
import gov.nasa.jpl.ammos.asec.kmc.format.SaCsvInput;
import org.checkerframework.checker.units.qual.K;
import picocli.CommandLine;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Update a Security Assocation
 */
@CommandLine.Command(name = "update", description = "Update an existing Security Association",
        mixinStandardHelpOptions = true, versionProvider = Version.class)
public class SaUpdate extends BaseCreateUpdate {

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private Args args;

    /**
     * Argument grouping for single and bulk, exclusive
     */
    static class Args {
        @CommandLine.ArgGroup(exclusive = false)
        SingleArgs single;
        @CommandLine.ArgGroup(exclusive = false)
        BulkArgs   bulk;
    }

    /**
     * Argument grouping for single args
     */
    static class SingleArgs {
        @CommandLine.ArgGroup(exclusive = false)
        SaUpdateSingle singleArgs;

        @CommandLine.ArgGroup(exclusive = false)
        OptionalOptions optionalArgs;
    }

    protected void doSingle() throws KmcException {
        try (IKmcDao dao = getDao()) {
            SpiScid  id = new SpiScid(spi, scid);
            ISecAssn sa = dao.getSa(id, frameType);
            if (sa == null) {
                throw new KmcException(String.format("%s SA %d/%d doesn't exist, can't update", frameType, id.getSpi(), id.getScid()));
            }
            console(String.format("%s updating %s SA", user, frameType));

            if (tfvn != null) {
                sa.setTfvn(tfvn);
            }
            if (vcid != null) {
                sa.setVcid(vcid);
            }
            if (mapId != null) {
                sa.setMapid(mapId);
            }
            try (IDbSession session = dao.newSession()) {
                session.beginTransaction();
                try {
                    updateSa(sa, dao, session);
                } catch (Exception e) {
                    session.rollback();
                    throw e;
                } finally {
                    if (session.isActive()) {
                        session.commit();
                    }
                }

            } catch (Exception e) {
                throw new KmcException(e);
            }

            console(String.format("%s updated %s SA %s/%s", user, frameType, sa.getId().getSpi(),
                    sa.getId().getScid()));
        }
    }

    protected void doBulk() throws IOException, KmcException {
        SaCsvInput input = new SaCsvInput();
        if (!file.exists()) {
            throwEx(String.format("File does not exist: %s", file));
        }
        try (Reader reader = new FileReader(file)) {
            List<ISecAssn> sas = input.parseCsv(reader, frameType);
            try (IKmcDao dao = getDao()) {
                for (ISecAssn sa : sas) {
                    try {
                        ISecAssn check = dao.getSa(sa.getId(), frameType);
                        if (check == null) {
                            warn(String.format("%s SA %d/%d does not exist, skipping", frameType, sa.getSpi(),
                                    sa.getScid()));
                            continue;
                        }
                        SecAssnValidator.validate(sa);
                        console(String.format("%s updating %s SA %d/%d", user, frameType, sa.getSpi(), sa.getScid()));
                        dao.updateSa(sa);
                    } catch (KmcException e) {
                        error(e.getMessage());
                    }
                    console(String.format("%s updated %s SA %d/%d", user, frameType, sa.getSpi(), sa.getScid()));
                }
            }
        }
    }

    /**
     * Main
     *
     * @param args args
     */
    public static void main(String... args) {
        int exit = new CommandLine(new SaUpdate()).execute(args);
        System.exit(exit);
    }

    @Override
    void checkAndSetArgs() throws KmcException {
        if (args != null) {
            if (args.single != null) {
                checkAndSetSpi(args.single.singleArgs.spi);
                checkAndSetScid(args.single.singleArgs.scid);
                checkAndSetTfvn(args.single.singleArgs.tfvn);
                checkAndSetVcid(args.single.singleArgs.vcid);
                checkAndSetMapId(args.single.singleArgs.mapid);
                this.mode = Mode.SINGLE;
                if (args.single.singleArgs != null) {
                    if (args.single.optionalArgs != null) {
                        try {
                            checkEncParams(args.single.optionalArgs.ekid, args.single.optionalArgs.ecs);
                            checkAuthParams(args.single.optionalArgs.akid, args.single.optionalArgs.acs);
                            checkSt(args.single.optionalArgs.st);
                            checkIvParams(args.single.optionalArgs.iv, args.single.optionalArgs.ivLen,
                                    args.single.optionalArgs.st, args.single.optionalArgs.ecs);
                            checkArsnParams(args.single.optionalArgs.arsn, args.single.optionalArgs.arsnlen);
                            checkArsnWParams(args.single.optionalArgs.arsnw);
                            checkAbmParams(args.single.optionalArgs.abm, args.single.optionalArgs.abmLen);
                            checkShivfLen(args.single.optionalArgs.shivfLen);
                            checkShplfLen(args.single.optionalArgs.shplfLen);
                            checkShsnfLen(args.single.optionalArgs.shsnfLen);
                            checkStmacfLen(args.single.optionalArgs.stmacfLen);
                        } catch (KmcException e) {
                            throw new KmcException(String.format("SA [%s, %d, %d, %d, %d] %s", frameType.name(), spi,
                                    scid, vcid, tfvn, e.getMessage()));
                        }
                    }
                }
            } else if (args.bulk != null) {
                if (args.bulk.file != null) {
                    File file = new File(args.bulk.file);
                    if (!file.exists()) {
                        throwEx(String.format("File does not exist: %s", args.bulk.file));
                    }
                    this.file = file;
                    this.mode = Mode.BULK;
                }
            }
        }
    }
}
