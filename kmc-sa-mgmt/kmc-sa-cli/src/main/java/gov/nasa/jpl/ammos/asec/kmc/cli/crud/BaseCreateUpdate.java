package gov.nasa.jpl.ammos.asec.kmc.cli.crud;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnValidator;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ServiceType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IDbSession;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

/**
 * Base class for creating and updating Security Associations
 */
@CommandLine.Command
public abstract class BaseCreateUpdate extends BaseCliApp {
    /**
     * SPI
     */
    protected Integer     spi;
    /**
     * SCID
     */
    protected Short       scid;
    /**
     * MAP ID
     */
    protected Byte        mapId;
    /**
     * TFVN
     */
    protected Byte        tfvn;
    /**
     * VCID
     */
    protected Byte        vcid;
    /**
     * Update enc bool
     */
    protected boolean     updateEnc;
    /**
     * Update auth bool
     */
    protected boolean     updateAuth;
    /**
     * Update ABM bool
     */
    protected boolean     updateAbm;
    /**
     * Service type
     */
    protected ServiceType st;
    /**
     * ECS bytes
     */
    protected byte[]      ecsBytes;
    /**
     * ARSN bytes
     */
    protected byte[]      arsnBytes;
    /**
     * IV bytes
     */
    protected byte[]      ivBytes;
    /**
     * ABM bytes
     */
    protected byte[]      abmBytes;
    /**
     * ACS bytes
     */
    protected byte[]      acsBytes;
    /**
     * ARSNW
     */
    protected Short       arsnw;
    /**
     * EKID
     */
    protected String      ekid;
    /**
     * AKID
     */
    protected String      akid;
    /**
     * ARSN length
     */
    protected Short       arsnlen;
    /**
     * IV length
     */
    protected Short       ivLen;
    /**
     * ABM length
     */
    protected Integer     abmLen;
    /**
     * SHIVF length
     */
    protected Short       shivfLen;
    /**
     * SHPLF length
     */
    protected Short       shplfLen;
    /**
     * SHSNF length
     */
    protected Short       shsnfLen;
    /**
     * ST MACF length
     */
    protected Short       stmacfLen;
    /**
     * File
     */
    protected File        file;
    /**
     * Mode
     */
    protected Mode        mode = Mode.UNKNOWN;

    /**
     * Username system property
     */
    protected final String user = System.getProperty("user.name");

    /**
     * Mode enum
     */
    protected enum Mode {
        /**
         * Single record
         */
        SINGLE,
        /**
         * Bulk records
         */
        BULK,
        /**
         * Unknown
         */
        UNKNOWN
    }

    /**
     * Bulk arguments
     */
    static class BulkArgs {
        @CommandLine.Option(names = "--file", required = true)
        String file;
    }

    @Override
    public Integer call() throws Exception {
        int exit = 0;
        try {
            checkAndSetArgs();
            switch (mode) {
                case SINGLE:
                    doSingle();
                    break;
                case BULK:
                    doBulk();
                    break;
                default:
                    exit = printHelp();
            }
        } catch (Exception e) {
            error(e.getMessage(), e);
            exit = 1;

        }
        return exit;
    }

    /**
     * Single record create/update
     *
     * @throws KmcException KMC ex
     */
    abstract void doSingle() throws KmcException;

    /**
     * Bulk record create/update
     *
     * @throws IOException  IO ex
     * @throws KmcException KMC ex
     */
    abstract void doBulk() throws IOException, KmcException;

    /**
     * Check service type
     *
     * @param stStr service type string
     * @throws KmcException KMC ex
     */
    protected void checkSt(String stStr) throws KmcException {
        if (stStr != null) {
            try {
                Short parsed = Short.parseShort(stStr);
                st = SecAssnValidator.checkSt(parsed);
            } catch (NumberFormatException e) {
                st = SecAssnValidator.checkSt(stStr);
            }
        }
    }

    /**
     * Update SA
     *
     * @param sa      SA
     * @param dao     DAO
     * @param session Session
     * @throws KmcException KMC ex
     */
    protected void updateSa(final ISecAssn sa, IKmcDao dao, IDbSession session) throws KmcException {
        ISecAssn mutableSa   = sa;
        boolean  needsUpdate = false;
        if (updateEnc) {
            console(getUpdateMessage(user, "encryption key", frameType, mutableSa.getId()));
            dao.rekeySaEnc(session, mutableSa.getId(), ekid, ecsBytes, (short) 1, frameType);
            session.flush();
            mutableSa = dao.getSa(session, mutableSa.getId(), frameType);
        }
        if (updateAuth) {
            console(getUpdateMessage(user, "authentication key", frameType, mutableSa.getId()));
            dao.rekeySaAuth(session, mutableSa.getId(), akid, acsBytes, (short) 1, frameType);
            session.flush();
            mutableSa = dao.getSa(session, mutableSa.getId(), frameType);
        }
        if (arsnBytes != null) {
            console(getUpdateMessage(user, "ARSN", frameType, mutableSa.getId()));
            mutableSa.setArsn(arsnBytes);
            mutableSa.setArsnLen(arsnlen);
            needsUpdate = true;
        }
        if (arsnw != null) {
            console(getUpdateMessage(user, "ARSNW", frameType, mutableSa.getId()));
            mutableSa.setArsnw(arsnw);
            needsUpdate = true;
        }
        if (ivBytes != null) {
            console(getUpdateMessage(user, "IV", frameType, mutableSa.getId()));
            mutableSa.setIv(ivBytes);
            mutableSa.setIvLen(ivLen);
            needsUpdate = true;
        }
        if (updateAbm) {
            console(getUpdateMessage(user, "ABM", frameType, mutableSa.getId()));
            mutableSa.setAbm(abmBytes);
            mutableSa.setAbmLen(abmLen);
            needsUpdate = true;
        }
        if (st != null) {
            console(getUpdateMessage(user, "ST", frameType, mutableSa.getId()));
            mutableSa.setEst(st.getEncryptionType());
            mutableSa.setAst(st.getAuthenticationType());
            needsUpdate = true;
        }
        if (shivfLen != null) {
            console(getUpdateMessage(user, "SHIVF length", frameType, mutableSa.getId()));
            mutableSa.setShivfLen(shivfLen);
            needsUpdate = true;
        }
        if (shplfLen != null) {
            console(getUpdateMessage(user, "SHPLF length", frameType, mutableSa.getId()));
            mutableSa.setShplfLen(shplfLen);
            needsUpdate = true;
        }
        if (shsnfLen != null) {
            console(getUpdateMessage(user, "SHSNF length", frameType, mutableSa.getId()));
            mutableSa.setShsnfLen(shsnfLen);
            needsUpdate = true;
        }
        if (stmacfLen != null) {
            console(getUpdateMessage(user, "STMACF length", frameType, mutableSa.getId()));
            mutableSa.setStmacfLen(stmacfLen);
            needsUpdate = true;
        }
        if (tfvn != null) {
            console(getUpdateMessage(user, "TFVN", frameType, mutableSa.getId()));
            mutableSa.setTfvn(tfvn);
            needsUpdate = true;
        }
        if (vcid != null) {
            console(getUpdateMessage(user, "VCID", frameType, mutableSa.getId()));
            mutableSa.setVcid(vcid);
            needsUpdate = true;
        }
        if (mapId != null) {
            console(getUpdateMessage(user, "MAPID", frameType, mutableSa.getId()));
            mutableSa.setMapid(mapId);
            needsUpdate = true;
        }
        if (needsUpdate) {
            dao.updateSa(session, mutableSa);
            session.flush();
        } else {
            warn(String.format("SA %d/%d nothing to update", sa.getSpi(), sa.getScid()));
        }
    }

    private String getUpdateMessage(String user, String field, FrameType frameType, SpiScid id) {
        return String.format("%s updating %s on %s SA %s/%s", user, field, frameType, id.getSpi(), id.getScid());
    }

    /**
     * Check IV params
     *
     * @param iv       IV
     * @param ivLen    IV length
     * @param stStr    Service type string
     * @param ecsBytes ECS bytes
     * @throws KmcException KMC ex
     */
    protected void checkIvParams(String iv, Short ivLen, String stStr, String ecsBytes) throws KmcException {
        checkSt(stStr);
        Short encryptionType = null;
        if (st != null) {
            encryptionType = st.getEncryptionType();
        }
        this.ivBytes = SecAssnValidator.verifyIv(iv, ivLen, encryptionType, ecsBytes);
        this.ivLen = ivLen;
    }

    /**
     * Check auth params
     *
     * @param akid AKID
     * @param acs  ACS
     * @throws KmcException KMC ex
     */
    protected void checkAuthParams(String akid, String acs) throws KmcException {
        this.acsBytes = SecAssnValidator.verifyAuth(akid, acs);
        this.akid = akid;
        if (acsBytes != null) {
            updateAuth = true;
        }
    }

    /**
     * Check encryption params
     *
     * @param ekid EKID
     * @param ecs  ECS
     * @throws KmcException KMC ex
     */
    protected void checkEncParams(String ekid, String ecs) throws KmcException {
        this.ecsBytes = SecAssnValidator.verifyEnc(ekid, ecs);
        this.ekid = ekid;
        if (ecsBytes != null) {
            updateEnc = true;
        }
    }

    /**
     * Check ARSN params
     *
     * @param arsn    ARSN
     * @param arsnlen ARSN length
     * @throws KmcException KMC ex
     */
    protected void checkArsnParams(String arsn, Short arsnlen) throws KmcException {
        this.arsnBytes = SecAssnValidator.verifyArsn(arsn, arsnlen);
        this.arsnlen = arsnlen;
    }

    /**
     * Check ARSNW params
     *
     * @param arsnw ARSNW
     * @throws KmcException KMC ex
     */
    protected void checkArsnWParams(Short arsnw) throws KmcException {
        SecAssnValidator.verifyArsnw(arsnw);
        this.arsnw = arsnw;
    }

    /**
     * Check ABM params
     *
     * @param abm    ABM
     * @param abmLen ABM length
     * @throws KmcException KMC ex
     */
    protected void checkAbmParams(String abm, Integer abmLen) throws KmcException {
        this.abmBytes = SecAssnValidator.checkAbm(abm, abmLen);
        this.abmLen = abmLen;
        this.updateAbm = abmBytes != null;
    }

    /**
     * Check SHIVF length
     *
     * @param shifvLen SHIVF length
     */
    protected void checkShivfLen(Short shifvLen) {
        this.shivfLen = shifvLen;
    }

    /**
     * Check SHPLF length
     *
     * @param shplfLen SHPLF length
     */
    protected void checkShplfLen(Short shplfLen) {
        this.shplfLen = shplfLen;
    }

    /**
     * Check SHSNF length
     *
     * @param shsnfLen SHSNF length
     */
    protected void checkShsnfLen(Short shsnfLen) {
        this.shsnfLen = shsnfLen;
    }

    /**
     * Check ST MAC field length
     *
     * @param stmacfLen ST MAC field length
     */
    protected void checkStmacfLen(Short stmacfLen) {
        this.stmacfLen = stmacfLen;
    }

    /**
     * Check and set arguments
     *
     * @throws KmcException KMC ex
     */
    abstract void checkAndSetArgs() throws KmcException;

    /**
     * Print help
     *
     * @return exit code
     */
    protected int printHelp() {
        CommandLine.usage(this, System.err);
        return 1;
    }

    /**
     * Check and set SPI
     *
     * @param spi SPI
     */
    protected void checkAndSetSpi(Integer spi) {
        this.spi = spi;
    }

    /**
     * Check and set SCID
     *
     * @param scid SCID
     */
    protected void checkAndSetScid(Short scid) {
        this.scid = scid;
    }

    /**
     * Check and set MAP ID
     *
     * @param mapId MAP ID
     */
    protected void checkAndSetMapId(Byte mapId) {
        this.mapId = mapId;
    }

    /**
     * Check and set TFVN
     *
     * @param tfvn TFVN
     */
    protected void checkAndSetTfvn(Byte tfvn) {
        this.tfvn = tfvn;
    }

    /**
     * Check and set VCID
     *
     * @param vcid VCID
     */
    protected void checkAndSetVcid(Byte vcid) {
        this.vcid = vcid;
    }

}
