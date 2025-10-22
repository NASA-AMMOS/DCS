package gov.nasa.jpl.ammos.asec.kmc.sadb;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcStartException;
import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcStopException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnAos;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnTm;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnValidator;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IDbSession;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * KMC DAO
 */
public class KmcDao implements IKmcDao {
    /**
     * Logger
     */
    private static final Logger LOG                           = LoggerFactory.getLogger(KmcDao.class);
    /**
     * SA Unkeyed constant
     */
    public static final  short  SA_UNKEYED                    = 1;
    /**
     * SA keyed constant
     */
    public static final  short  SA_KEYED                      = 2;
    /**
     * SA expire constant
     */
    public static final  short  SA_EXPIRE                     = 1;
    /**
     * SA operational constant
     */
    public static final  short  SA_OPERATIONAL                = 3;
    /**
     * Hibernate config
     */
    public static final  String ETC_HIBERNATE_CFG_XML         = "etc/hibernate.cfg.xml";
    /**
     * Hibernate connection username key
     */
    public static final  String HIBERNATE_CONNECTION_USERNAME = "hibernate.connection.username";
    /**
     * Hibernate connection pass key
     */
    public static final  String HIBERNATE_CONNECTION_PASSWORD = "hibernate.connection.password";
    /**
     * Hibernate connection URL key
     */
    public static final  String HIBERNATE_CONNECTION_URL      = "hibernate.connection.url";
    /**
     * SQL FROM keyword
     */
    public static final  String SQL_FROM                      = "FROM";

    private final Properties    properties = new Properties();
    private       Configuration config     = new Configuration();

    private SessionFactory factory;

    private boolean hasAos;
    private boolean hasTm;

    /**
     * Constructor
     */
    public KmcDao() {
        config.configure(ETC_HIBERNATE_CFG_XML);
    }

    /**
     * Constructor
     *
     * @param user     user
     * @param password pass
     * @param url      url
     */
    public KmcDao(String user, char[] password, String url) {
        this(user, new String(password), url);
    }

    /**
     * Constructor
     *
     * @param user     user
     * @param password pass
     * @param url      url
     */
    public KmcDao(String user, String password, String url) {
        properties.put(HIBERNATE_CONNECTION_USERNAME, user);
        properties.put(HIBERNATE_CONNECTION_PASSWORD, password);
        properties.put(HIBERNATE_CONNECTION_URL, url);
        configure();
    }

    /**
     * Constructor
     *
     * @param user     user
     * @param password pass
     */
    public KmcDao(String user, String password) {
        properties.put(HIBERNATE_CONNECTION_USERNAME, user);
        properties.put(HIBERNATE_CONNECTION_PASSWORD, password);
        configure();
    }

    private void configure() {
        config.configure(ETC_HIBERNATE_CFG_XML);
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            config.getProperties().setProperty((String) entry.getKey(), (String) entry.getValue());
        }
    }

    @Override
    public void init() throws KmcException {
        try {
            final List<String> tables = new ArrayList<>();
            factory = config.buildSessionFactory();
            try (Session s = factory.openSession()) {

                s.doWork(session -> {
                    try (ResultSet rs = session.getMetaData().getTables(null, null, "%", null)) {
                        while (rs.next()) {
                            tables.add(rs.getString(3));
                        }
                    }
                });
            }
            config = new Configuration();
            for (String table : tables) {
                switch (table.toLowerCase()) {
                    case "security_associations_tm":
                        LOG.info("Initializing security associations tm");
                        config.addAnnotatedClass(SecAssnTm.class);
                        hasTm = true;
                        break;
                    case "security_associations_aos":
                        LOG.info("Initializing security associations aos");
                        config.addAnnotatedClass(SecAssnAos.class);
                        hasAos = true;
                        break;
                    default:
                        break;
                }
            }
            configure();
            factory.close();
            factory = config.buildSessionFactory();
        } catch (HibernateException e) {
            throw new KmcException(e);
        }
    }

    /**
     * Wrapper class for hibernate sessions
     */

    @Override
    public IDbSession newSession() {
        return new DbSession(factory.openSession());
    }

    @Override
    public void createSa(IDbSession dbSession, Integer spi, Byte tvfn, Short scid, Byte vcid, Byte mapid,
                         FrameType type) throws KmcException {
        isReady();
        checkFrameType(type);
        if (spi == null) {
            spi = getNextAvailableSpi(scid, type);
        }
        SpiScid  spiScid = new SpiScid(spi, scid);
        ISecAssn sa;
        switch (type) {
            case TM -> sa = new SecAssnTm(spiScid);
            case AOS -> sa = new SecAssnAos(spiScid);
            default -> sa = new SecAssn(spiScid);
        }
        sa.setTfvn(tvfn);
        sa.setVcid(vcid);
        sa.setMapid(mapid);
        sa.setSaState(SA_UNKEYED);
        LOG.info("Creating SA {}/{}", spi, scid);
        dbSession.persist(sa);
        LOG.info("Creating SA {}/{} done", spi, scid);
    }

    @Override
    public ISecAssn createSa(Integer spi, Byte tvfn, Short scid, Byte vcid, Byte mapid, FrameType type) throws
                                                                                                        KmcException {
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            session.beginTransaction();
            createSa(session, spi, tvfn, scid, vcid, mapid, type);
            session.commit();
        } catch (Exception e) {
            LOG.error("Encountered unexpected error while creating SA with the following parameters: " +
                            "spi {}, tvfn {}, scid {}, vcid {}, mapid {}, type {} : {}", spi, tvfn, scid, vcid,
                    mapid
                    , type,
                    e.getMessage());
            throw new KmcException("Unable to create SA due to error: ", e);
        }
        return getSa(new SpiScid(spi, scid), type);
    }

    private Integer getNextAvailableSpi(Short scid, FrameType type) throws KmcException {
        isReady();
        checkFrameType(type);
        int spi;
        try (Session session = factory.openSession()) {
            Integer maxSpi = session
                    .createQuery("SELECT max(sa.id.spi) " + SQL_FROM + " " + type.toString() + " sa WHERE sa.id" +
                                    ".scid " +
                                    "= :scid",
                            Integer.class)
                    .setParameter("scid", scid)
                    .getSingleResult();
            if (maxSpi == null) {
                maxSpi = 0;
            }
            spi = maxSpi + 1;
        } catch (HibernateException e) {
            LOG.error("Encountered Hibernate error while querying for max SPI for SCID {}: {}", scid,
                    e.getMessage());
            throw new KmcException("Unable to get next available SPI due to Hibernate error: ", e);
        } catch (Exception e) {
            LOG.error("Encountered unexpected error while querying for max SPI for SCID {}: {}", scid,
                    e.getMessage());
            throw new KmcException("Unable to get next available SPI due to unexpected error: ", e);
        }
        return spi;
    }

    @Override
    public ISecAssn createSa(ISecAssn sa) throws KmcException {
        checkFrameType(sa.getType());
        try (DbSession session = new DbSession(factory.openSession())) {
            session.beginTransaction();
            createSa(session, sa);
            session.commit();
        } catch (HibernateException e) {
            LOG.error("Encountered Hibernate error while creating SA {}: {}", sa, e.getMessage());
            throw new KmcException("Unable to create SA due to Hibernate error: ", e);
        } catch (Exception e) {
            LOG.error("Encountered unexpected error while creating SA {}: {}", sa, e.getMessage());
            throw new KmcException("Unable to create SA due to unexpected error: ", e);
        }
        return getSa(sa.getId(), sa.getType());
    }

    @Override
    public void createSa(IDbSession dbSession, ISecAssn sa) throws KmcException {
        isReady();
        checkFrameType(sa.getType());
        try {
            SecAssnValidator.validate(sa);
        } catch (KmcException e) {
            throw new KmcException(String.format("Cannot create SA: %s", e.getMessage()));
        }
        if (sa.getSpi() == null) {
            sa.setSpi(getNextAvailableSpi(sa.getScid(), sa.getType()));
        } else {
            ISecAssn exists = getSa(dbSession, sa.getId(), sa.getType());
            if (exists != null) {
                throw new KmcException(String.format("SA create failed: an SA with the SPI/SCID combination %d/%d" +
                        " " + "already exists", sa.getSpi(), sa.getScid()));
            }
        }
        // validate more
        if (sa.getSaState() == null) {
            sa.setSaState(SA_UNKEYED);
        }

        LOG.info("Creating {}", sa);
        dbSession.persist(sa);
        LOG.info("Created {}", sa);
    }

    @Override
    public void rekeySaEnc(IDbSession session, SpiScid id, String ekid, byte[] ecs, Short ecsLen, FrameType type) throws
                                                                                                                  KmcException {
        isReady();
        checkFrameType(type);
        ISecAssn sa = getSa(session, id, type);
        if (sa == null) {
            throw new KmcException(String.format("SA %s [%d, %d] does not exist, cannot rekey for encryption",
                    type.name(), id.getSpi(), id.getScid()));
        }
        LOG.info("Rekeying SA {} [{}, {}] for encryption to EKID {} with ECS {}", type.name(), id.getSpi(),
                id.getScid(), ekid.replaceAll("\\p{Cntrl}", " "), ecs);
        sa.setEkid(ekid);
        sa.setEcs(ecs);
        sa.setEcsLen(ecsLen);
        sa.setSaState(SA_KEYED);
        session.merge(sa);
        LOG.info("Rekeying SA {} [{}, {}] for encryption complete", type.name(), id.getSpi(), id.getScid());
    }

    @Override
    public ISecAssn rekeySaEnc(SpiScid id, String ekid, byte[] ecs, Short ecsLen, FrameType type) throws
                                                                                                  KmcException {
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            session.beginTransaction();
            rekeySaEnc(session, id, ekid, ecs, ecsLen, type);
            session.commit();
        } catch (Exception e) {
            throw new KmcException(e);
        }
        return getSa(id, type);
    }

    @Override
    public void rekeySaAuth(IDbSession session, SpiScid id, String akid, byte[] acs, Short acsLen, FrameType type) throws
                                                                                                                   KmcException {
        isReady();
        checkFrameType(type);
        ISecAssn sa = getSa(session, id, type);
        if (sa == null) {
            throw new KmcException(String.format("SA %s [%d, %d] does not exist, cannot rekey for authentication",
                    type.name(), id.getSpi(), id.getScid()));
        }
        LOG.info("Rekeying SA {} [{}, {}] for authentication to AKID {} with ACS {}", type.name(), sa.getId(),
                sa.getScid(), akid,
                acs);
        sa.setAkid(akid);
        sa.setAcs(acs);
        sa.setAcsLen(acsLen);
        sa.setSaState(SA_KEYED);
        session.merge(sa);
        LOG.info("Rekeying SA {} [{}, {}] for authentication complete", type.name(), id.getSpi(), id.getScid());
    }

    @Override
    public ISecAssn rekeySaAuth(SpiScid id, String akid, byte[] acs, Short acsLen, FrameType type) throws
                                                                                                   KmcException {
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            session.beginTransaction();
            rekeySaAuth(session, id, akid, acs, acsLen, type);
            session.commit();
        } catch (Exception e) {
            throw new KmcException(e);
        }

        return getSa(id, type);
    }

    @Override
    public void expireSa(IDbSession session, SpiScid id, FrameType type) throws KmcException {
        isReady();
        checkFrameType(type);
        ISecAssn sa = getSa(id, type);
        if (sa == null) {
            throw new KmcException(String.format("SA %s [%d, %d] does not exist, cannot expire", type.name(),
                    id.getSpi(), id.getScid()));
        } else {
            LOG.info("Expiring SA {} [{}, {}]", type.name(), id.getSpi(), id.getScid());
            sa.setSaState(SA_EXPIRE);
            sa.setAkid(null);
            sa.setEkid(null);
            session.merge(sa);
            LOG.info("Expired SA {} [{}, {}]", type.name(), id.getSpi(), id.getScid());
        }
    }

    @Override
    public ISecAssn expireSa(SpiScid id, FrameType type) throws KmcException {
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            session.beginTransaction();
            expireSa(session, id, type);
            session.commit();
        } catch (Exception e) {
            throw new KmcException(e);
        }
        return getSa(id, type);
    }

    @Override
    public void startSa(IDbSession session, SpiScid id, boolean force, FrameType type) throws KmcException {
        isReady();
        checkFrameType(type);
        ISecAssn sa = getSa(id, type);
        if (sa == null) {
            throw new KmcStartException(String.format("SA %s [%d, %d] does not exist, cannot start", type.name(),
                    id.getSpi(),
                    id.getScid()));
        } else if (sa.getSaState() == SA_OPERATIONAL) {
            throw new KmcStartException(String.format("SA %s [%d/%d] is already operational", type.name(), id.getSpi(),
                    id.getScid()));
        } else {
            Query<? extends ISecAssn> q =
                    ((DbSession) session).getSession().createQuery(SQL_FROM + " " + type.toString() +
                            " AS sa WHERE sa" +
                            ".id.scid = :scid" +
                            " AND " +
                            "sa.tfvn = :tfvn" +
                            " AND " +
                            "sa.vcid = :vcid" +
                            " AND " +
                            "sa.mapid = :mapid" +
                            " AND " +
                            "sa.id.spi != :subjectSpi", ISecAssn.class);
            q.setParameter("scid", sa.getId().getScid());
            q.setParameter("tfvn", sa.getTfvn());
            q.setParameter("vcid", sa.getVcid());
            q.setParameter("mapid", sa.getMapid());
            q.setParameter("subjectSpi", sa.getSpi());
            List<? extends ISecAssn> sas = q.list();
            for (ISecAssn s : sas) {
                if (s.getSaState() == SA_OPERATIONAL) {
                    if (force) {
                        stopSa(s.getId(), type);
                        LOG.info("SA {} [{}, {}] is already operational, stop has been forced", type.name(),
                                s.getSpi(), s.getScid());
                    } else {
                        throw new KmcStartException(String.format("SA %s, [%d, %d] is already operational for GVCID " +
                                        "(scid " + "%d, tfvn " + "%d, vcid %d, mapid %d)", type.name(), s.getSpi(),
                                s.getScid(),
                                s.getScid(), s.getTfvn(), s.getVcid(), s.getMapid()));
                    }
                }
            }
            LOG.info("Starting SA {} [{}, {}]", type.name(), id.getSpi(), id.getScid());
            sa.setSaState(SA_OPERATIONAL);
            session.merge(sa);
            LOG.info("Started SA {} [{}, {}]", type.name(), id.getSpi(), id.getScid());
        }
    }

    @Override
    public ISecAssn startSa(SpiScid id, boolean force, FrameType type) throws KmcException {
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            session.beginTransaction();
            startSa(session, id, force, type);
            session.commit();
        } catch (KmcStartException e) {
            throw e;
        } catch (Exception e) {
            throw new KmcException(e);
        }
        return getSa(id, type);
    }

    @Override
    public void stopSa(IDbSession session, SpiScid id, FrameType type) throws KmcException {
        isReady();
        checkFrameType(type);
        ISecAssn sa = getSa(id, type);
        if (sa == null) {
            throw new KmcStopException(String.format("SA %s [%d, %d] does not exist, cannot stop", type.name(),
                    id.getSpi(),
                    id.getScid()));
        } else if (sa.getSaState() != SA_OPERATIONAL) {
            throw new KmcStopException(String.format("SA %s [%d, %d] is not operational, cannot stop", type.name(),
                    id.getSpi(),
                    id.getScid()));
        } else {
            LOG.info("Stopping SA {} [{}, {}]", type.name(), id.getSpi(), id.getScid());
            sa.setSaState(SA_KEYED);
            session.merge(sa);
            LOG.info("Stopped SA {} [{}, {}]", type.name(), id.getSpi(), id.getScid());
        }
    }

    @Override
    public ISecAssn stopSa(SpiScid id, FrameType type) throws KmcException {
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            session.beginTransaction();
            stopSa(session, id, type);
            session.commit();
        } catch (KmcStopException e) {
            throw e;
        } catch (Exception e) {
            throw new KmcException(e);
        }
        return getSa(id, type);
    }

    @Override
    public void deleteSa(IDbSession session, SpiScid id, FrameType type) throws KmcException {
        isReady();
        checkFrameType(type);
        ISecAssn sa = getSa(id, type);
        if (sa == null) {
            throw new KmcException(String.format("SA %s [%d, %d] does not exist, cannot delete", type.name(),
                    id.getSpi(), id.getScid()));
        } else {
            LOG.info("Deleting SA {} [{}, {}]", type.name(), id.getSpi(), id.getScid());
            session.remove(sa);
            LOG.info("Deleted SA {} [{}, {}]", type.name(), id.getSpi(), id.getScid());
        }
    }

    @Override
    public void deleteSa(SpiScid id, FrameType type) throws KmcException {
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            session.beginTransaction();
            deleteSa(session, id, type);
            session.commit();
        } catch (KmcException e) {
            throw e;
        } catch (Exception e) {
            throw new KmcException(e);
        }
    }

    @Override
    public ISecAssn getSa(IDbSession session, SpiScid id, FrameType type) throws KmcException {
        isReady();
        checkFrameType(type);
        return ((DbSession) session).getSession().find(type.getClazz(), id);
    }

    @Override
    public ISecAssn getSa(SpiScid id, FrameType type) throws KmcException {
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            return getSa(session, id, type);
        } catch (KmcException e) {
            throw e;
        } catch (Exception e) {
            throw new KmcException(e);
        }
    }

    @Override
    public List<ISecAssn> getSas(IDbSession session, FrameType type) throws KmcException {
        isReady();
        checkFrameType(type);
        return ((DbSession) session).getSession()
                .createQuery(SQL_FROM + " " + type.toString(), type.getClazz())
                .list().stream().map(o -> (ISecAssn) o).toList();
    }

    @Override
    public List<ISecAssn> getSas(FrameType type) throws KmcException {
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            if (type == FrameType.ALL) {
                List<ISecAssn> sas = new ArrayList<>();
                sas.addAll(getSas(session, FrameType.TC));
                if (hasTm) {
                    sas.addAll(getSas(session, FrameType.TM));
                }
                if (hasAos) {
                    sas.addAll(getSas(session, FrameType.AOS));
                }
                return sas.stream().toList();
            } else {
                return getSas(session, type);
            }
        } catch (KmcException e) {
            throw e;
        } catch (Exception e) {
            throw new KmcException(e);
        }
    }

    private void checkFrameType(FrameType type) throws KmcException {
        if (type == FrameType.TM && !hasTm) {
            throw new KmcException("TM frame type not supported");
        } else if (type == FrameType.AOS && !hasAos) {
            throw new KmcException("AOS frame type not supported");
        }
    }

    /**
     * Get active SAs
     *
     * @param type frame type
     * @return active SAs
     * @throws KmcException exception
     */
    public List<ISecAssn> getActiveSas(FrameType type) throws KmcException {
        isReady();
        checkFrameType(type);
        try (IDbSession session = newSession()) {
            Query<? extends ISecAssn> query = ((DbSession) session).getSession()
                    .createQuery(SQL_FROM + " " + type.toString() + " as sa WHERE sa.saState = :state",
                            type.getClazz());
            query.setParameter("state", SA_OPERATIONAL);
            return query.stream().map(o -> (ISecAssn) o).toList();
        } catch (Exception e) {
            throw new KmcException(e);
        }
    }

    @Override
    public void updateSa(IDbSession session, ISecAssn sa) throws KmcException {
        isReady();
        checkFrameType(sa.getType());
        LOG.info("Updating SA {} [{}, {}]", sa.getType().name(), sa.getId().getSpi(), sa.getId().getScid());
        session.merge(sa);
        LOG.info("Updated SA {} [{}, {}]", sa.getType().name(), sa.getId().getSpi(), sa.getId().getScid());
    }

    @Override
    public ISecAssn updateSa(ISecAssn sa) throws KmcException {
        checkFrameType(sa.getType());
        try (IDbSession session = newSession()) {
            session.beginTransaction();
            updateSa(session, sa);
            session.commit();
        } catch (KmcException e) {
            throw e;
        } catch (Exception e) {
            throw new KmcException(e);
        }
        return getSa(sa.getId(), sa.getType());
    }

    @Override
    public void close() throws KmcException {
        if (factory == null) {
            return;
        }
        factory.close();
    }

    private synchronized boolean isReady() throws KmcException {
        if (factory == null || factory.isClosed()) {
            config = new Configuration();
            configure();
            init();
        }
        return true;
    }

    @Override
    public boolean status() {
        try {
            isReady();
        } catch (KmcException e) {
            return false;
        }

        try (Session session = factory.openSession()) {
            session.doWork(connection -> connection.isValid(1000));
        } catch (HibernateException e) {
            LOG.error("Error validating database connection", e);
            return false;
        }

        return true;
    }
}
