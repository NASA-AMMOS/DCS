package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnFactory;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnValidator;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IDbSession;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.opts.OptionalOptions;
import gov.nasa.jpl.ammos.asec.kmc.cli.crud.opts.SaCreateSingle;
import gov.nasa.jpl.ammos.asec.kmc.cli.misc.Version;
import gov.nasa.jpl.ammos.asec.kmc.format.SaCsvInput;
import picocli.CommandLine;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Create a Security Assocation
 */
@CommandLine.Command(name = "create", description = "Create a new Security Association", mixinStandardHelpOptions =
        true, versionProvider = Version.class)
public class SaCreate extends BaseCreateUpdate {

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
        SaCreateSingle singleArgs;

        @CommandLine.ArgGroup(exclusive = false)
        OptionalOptions optionalArgs;
    }

    protected void doBulk() throws IOException, KmcException {
        SaCsvInput input = new SaCsvInput();
        if (!file.exists()) {
            throwEx(String.format("File does not exist: %s", file));
        }
        try (Reader reader = new FileReader(file)) {
            List<ISecAssn> sas = input.parseCsv(reader, frameType);
            try (IKmcDao dao = getDao(); IDbSession session = dao.newSession()) {
                for (ISecAssn sa : sas) {
                    try {
                        session.beginTransaction();
                        SecAssnValidator.validate(sa);
                        console(String.format("%s creating %s", user, sa));
                        dao.createSa(session, sa);
                        console(String.format("%s created %s", user, sa));
                    } catch (KmcException e) {
                        console(String.format("%s creation FAILED", sa));
                        error(e.getMessage());
                    } finally {
                        session.commit();
                    }
                }
            } catch (KmcException e) {
                throw e;
            } catch (Exception e) {
                throw new KmcException(e);
            }
        }
    }

    protected void doSingle() throws KmcException {
        console(String.format("%s creating SA", user));
        try (IKmcDao dao = getDao()) {
            SpiScid  id = new SpiScid(spi, scid);
            ISecAssn sa = dao.getSa(id, frameType);
            if (sa != null) {
                throwEx(String.format("Create error, %s already exists", sa));
            }
            sa = SecAssnFactory.createSecAssn(id, frameType);

            sa.setTfvn(tfvn);
            sa.setVcid(vcid);
            sa.setMapid(mapId);
            try (IDbSession session = dao.newSession()) {
                session.beginTransaction();
                try {
                    dao.createSa(session, sa);
                    console(String.format("%s created SA %s/%s", user, sa.getId().getSpi(), sa.getId().getScid()));
                    sa = dao.getSa(session, sa.getId(), frameType);
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

        }
    }

    /**
     * Main
     *
     * @param args args
     */
    public static void main(String... args) {
        int exit = new CommandLine(new SaCreate()).execute(args);
        System.exit(exit);
    }

    @Override
    void checkAndSetArgs() throws KmcException {
        if (args != null) {
            if (args.single != null) {
                this.mode = Mode.SINGLE;
                if (args.single.singleArgs != null) {
                    checkAndSetSpi(args.single.singleArgs.spi);
                    checkAndSetScid(args.single.singleArgs.scid);
                    checkAndSetTfvn(args.single.singleArgs.tfvn);
                    checkAndSetVcid(args.single.singleArgs.vcid);
                    checkAndSetMapId(args.single.singleArgs.mapid);
                    if (args.single.optionalArgs != null) {
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
                    }
                }
            } else if (args.bulk != null) {
                this.mode = Mode.BULK;
                if (args.bulk.file != null) {
                    file = new File(args.bulk.file);
                    if (!file.exists()) {
                        throwEx(String.format("File does not exist: %s", args.bulk.file));
                    }
                }
            }
        }
    }
}

