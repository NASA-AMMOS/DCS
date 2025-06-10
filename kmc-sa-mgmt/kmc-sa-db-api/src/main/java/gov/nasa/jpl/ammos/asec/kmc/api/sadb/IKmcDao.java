package gov.nasa.jpl.ammos.asec.kmc.api.sadb;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;

import java.util.List;

/**
 * KMC DAO
 *
 */
public interface IKmcDao extends AutoCloseable {
    IDbSession newSession();

    /**
     * Create an SA with provided database session. Initial state is UNKEYED.
     *
     * @param IdbSession database session
     * @param spi        security parameter index
     * @param tvfn       transfer frame version number
     * @param scid       spacecraft id
     * @param vcid       virtual channel id
     * @param mapid      multiplexer access point id
     * @throws KmcException exception
     */
    void createSa(IDbSession IdbSession, Integer spi, Byte tvfn, Short scid, Byte vcid, Byte mapid) throws KmcException;

    /**
     * Create an SA. Initial state is UNKEYED.
     *
     * @param spi   security parameter index
     * @param tvfn  transfer frame version number
     * @param scid  spacecraft id
     * @param vcid  virtual channel id
     * @param mapid multiplexer access point id
     * @return sa
     * @throws KmcException exception
     */
    SecAssn createSa(Integer spi, Byte tvfn, Short scid, Byte vcid, Byte mapid) throws KmcException;

    /**
     * Create an SA
     *
     * @param sa security association
     * @return sa
     * @throws KmcException exception
     */
    SecAssn createSa(SecAssn sa) throws KmcException;

    /**
     * Create an SA with the provided DB session
     *
     * @param IdbSession database session
     * @param sa         security association
     * @throws KmcException exception
     */
    void createSa(IDbSession IdbSession, SecAssn sa) throws KmcException;

    /**
     * Re/key an SA for encryption with the provided database session
     *
     * @param session database session
     * @param id      spi + scid
     * @param ekid    encryption key id
     * @param ecs     encryption cipher suite
     * @param ecsLen  ecs length
     * @throws KmcException exception
     */
    void rekeySaEnc(IDbSession session, SpiScid id, String ekid, byte[] ecs, Short ecsLen) throws KmcException;

    /**
     * Re/key an SA for encryption
     *
     * @param id     spi + scid
     * @param ekid   encryption key id
     * @param ecs    encryption cipher suite
     * @param ecsLen ecs length
     * @return sa
     * @throws KmcException exception
     */
    SecAssn rekeySaEnc(SpiScid id, String ekid, byte[] ecs, Short ecsLen) throws KmcException;

    /**
     * Rekey an SA for authentication with the provided database session
     *
     * @param session database session
     * @param id      spi + scid
     * @param akid    authentication key id
     * @param acs     authentication cipher suite
     * @param acsLen  acs length
     * @throws KmcException exception
     */
    void rekeySaAuth(IDbSession session, SpiScid id, String akid, byte[] acs, Short acsLen) throws KmcException;

    /**
     * Rekey an SA for authentication
     *
     * @param id     spi + scid
     * @param akid   authentication key id
     * @param acs    authentication cipher suite
     * @param acsLen acs length
     * @return sa
     * @throws KmcException exception
     */
    SecAssn rekeySaAuth(SpiScid id, String akid, byte[] acs, Short acsLen) throws KmcException;

    /**
     * Expire an SA with the provided database session
     *
     * @param session database session
     * @param id      spi + scid
     * @throws KmcException exception
     */
    void expireSa(IDbSession session, SpiScid id) throws KmcException;

    /**
     * Expire an SA
     *
     * @param id spi + scid
     * @return sa
     * @throws KmcException exception
     */
    SecAssn expireSa(SpiScid id) throws KmcException;

    /**
     * Start an SA with the provided database session. Sets given SPI SA to ENABLED, all other SAs to KEYED.
     *
     * @param session database session
     * @param id      spi + scid
     * @param force   start SA, stopping other SAs with same GVCID
     * @throws KmcException exception
     */
    void startSa(IDbSession session, SpiScid id, boolean force) throws KmcException;

    /**
     * Start an SA. Sets given SPI SA to ENABLED, all other SAs to KEYED.
     *
     * @param id    spi + scid
     * @param force start SA, stopping other SAs with same GVCID
     * @return sa
     * @throws KmcException exception
     */
    SecAssn startSa(SpiScid id, boolean force) throws KmcException;

    /**
     * Stop an SA with the provided database session. Sets given SPI SA to KEYED.
     *
     * @param session database session
     * @param id      spi + scid
     * @throws KmcException exception
     */
    void stopSa(IDbSession session, SpiScid id) throws KmcException;

    /**
     * Stop an SA. Sets given SPI SA to KEYED.
     *
     * @param id spi + scid
     * @return sa
     * @throws KmcException exception
     */
    SecAssn stopSa(SpiScid id) throws KmcException;

    /**
     * Delete an SA with the provided database session. Removes the SA from the DB.
     *
     * @param session database session
     * @param id      spi + scid
     * @throws KmcException exception
     */
    void deleteSa(IDbSession session, SpiScid id) throws KmcException;

    /**
     * Delete an SA. Removes the SA from the DB.
     *
     * @param id spi + scid
     * @throws KmcException exception
     */
    void deleteSa(SpiScid id) throws KmcException;

    /**
     * Get an SA with the provided database session
     *
     * @param session database session
     * @param id      spi + scid
     * @return security association
     * @throws KmcException exception
     */
    SecAssn getSa(IDbSession session, SpiScid id) throws KmcException;

    /**
     * Get an SA
     *
     * @param id spi + scid
     * @return security association
     * @throws KmcException exception
     */
    SecAssn getSa(SpiScid id) throws KmcException;

    /**
     * Get all SAs with the provided database session
     *
     * @param session database session
     * @return a list of SAs
     * @throws KmcException exception
     */
    List<SecAssn> getSas(IDbSession session) throws KmcException;

    /**
     * Get all SAs
     *
     * @return a list of SAs
     * @throws KmcException exception
     */
    List<SecAssn> getSas() throws KmcException;

    /**
     * Update an SA with the provided database session
     *
     * @param session database session
     * @param sa      security association
     * @throws KmcException exception
     */
    void updateSa(IDbSession session, SecAssn sa) throws KmcException;

    /**
     * Update an SA
     *
     * @param sa security association
     * @return sa
     * @throws KmcException exception
     */
    SecAssn updateSa(SecAssn sa) throws KmcException;

    /**
     * Close the DAO
     *
     * @throws KmcException exception
     */
    void close() throws KmcException;

    /**
     * Initialize the DAO
     *
     * @throws KmcException exception
     */
    void init() throws KmcException;

    /**
     * Returns operational status
     *
     * @return true if operational, false if not
     */
    boolean status();
}
