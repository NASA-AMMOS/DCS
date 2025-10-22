package gov.nasa.jpl.ammos.asec.kmc.api.sa;

/**
 * Security Association interface
 */
public interface ISecAssn {

    /**
     * Set SPI/SCID
     *
     * @param id SPI/SCID
     */
    void setId(SpiScid id);

    /**
     * @return SPI/SCID
     */
    SpiScid getId();

    /**
     * Set AKID
     *
     * @param akid AKID
     */
    void setAkid(String akid);

    /**
     * Get AKID
     *
     * @return AKID
     */
    String getAkid();

    /**
     * Get SPI
     *
     * @return SPI
     */
    Integer getSpi();

    /**
     * Set SPI
     *
     * @param spi SPI
     */
    void setSpi(Integer spi);

    /**
     * Get EKID
     *
     * @return EKID
     */
    String getEkid();

    /**
     * Set EKID
     *
     * @param ekid EKID
     */
    void setEkid(String ekid);

    /**
     * Get SA State
     *
     * @return SA State
     */
    Short getSaState();

    /**
     * Set SA State
     *
     * @param saState SA State
     */
    void setSaState(Short saState);

    /**
     * Get TFVN
     *
     * @return TFVN
     */
    Byte getTfvn();

    /**
     * Set TFVN
     *
     * @param tfvn TFVN
     */
    void setTfvn(Byte tfvn);

    /**
     * Get SCID
     *
     * @return SCID
     */
    Short getScid();

    /**
     * Set SCID
     *
     * @param scid SCID
     */
    void setScid(Short scid);

    /**
     * Get VCID
     *
     * @return VCID
     */
    Byte getVcid();

    /**
     * Set VCID
     *
     * @param vcid VCID
     */
    void setVcid(Byte vcid);

    /**
     * Get MAP ID
     *
     * @return MAP ID
     */
    Byte getMapid();

    /**
     * Set MAP ID
     *
     * @param mapid MAP ID
     */
    void setMapid(Byte mapid);

    /**
     * Get LPID
     *
     * @return LPID
     */
    Short getLpid();

    /**
     * Set LPID
     *
     * @param lpid LPID
     */
    void setLpid(Short lpid);

    /**
     * Get EST
     *
     * @return EST
     */
    Short getEst();

    /**
     * Set EST
     *
     * @param est EST
     */
    void setEst(Short est);

    /**
     * Get AST
     *
     * @return AST
     */
    Short getAst();

    /**
     * Set AST
     *
     * @param ast AST
     */
    void setAst(Short ast);

    /**
     * Get SHIVF Length
     *
     * @return SHIVF Length
     */
    Short getShivfLen();

    /**
     * Set SHIVF Length
     *
     * @param shivfLen SHIVF Length
     */
    void setShivfLen(Short shivfLen);

    /**
     * Get SHSNF Length
     *
     * @return SHSNF Length
     */
    Short getShsnfLen();

    /**
     * Set SHSNF Length
     *
     * @param shsnfLen SHSNF Length
     */
    void setShsnfLen(Short shsnfLen);

    /**
     * Get SHPLF Length
     *
     * @return SHPLF Length
     */
    Short getShplfLen();

    /**
     * Set SHPLF Length
     *
     * @param shplfLen SHPLF Length
     */
    void setShplfLen(Short shplfLen);

    /**
     * Get STMACF Length
     *
     * @return STMACF Length
     */
    Short getStmacfLen();

    /**
     * Set STMACF Length
     *
     * @param stmacfLen STMACF Length
     */
    void setStmacfLen(Short stmacfLen);

    /**
     * Get ECS Length
     *
     * @return ECS Length
     */
    Short getEcsLen();

    /**
     * Set ECS Length
     *
     * @param ecsLen ECS Length
     */
    void setEcsLen(Short ecsLen);

    /**
     * Get ECS
     *
     * @return ECS
     */
    byte[] getEcs();

    /**
     * Set ECS
     *
     * @param ecs ECS
     */
    void setEcs(byte[] ecs);

    /**
     * Get IV length
     *
     * @return IV length
     */
    Short getIvLen();

    /**
     * Set IV length
     *
     * @param ivLen IV length
     */
    void setIvLen(Short ivLen);

    /**
     * Get IV
     *
     * @return IV
     */
    byte[] getIv();

    /**
     * Set IV
     *
     * @param iv IV
     */
    void setIv(byte[] iv);

    /**
     * Get ACS length
     *
     * @return ACS length
     */
    Short getAcsLen();

    /**
     * Set ACS length
     *
     * @param acsLen ACS length
     */
    void setAcsLen(Short acsLen);

    /**
     * Get ACS
     *
     * @return ACS
     */
    byte[] getAcs();

    /**
     * Set ACS
     *
     * @param acs ACS
     */
    void setAcs(byte[] acs);

    /**
     * Get ABM length
     *
     * @return ABM length
     */
    Integer getAbmLen();

    /**
     * Set ABM length
     *
     * @param abmLen ABM length
     */
    void setAbmLen(int abmLen);

    /**
     * Get ABM
     *
     * @return ABM
     */
    byte[] getAbm();

    /**
     * Set ABM
     *
     * @param abm ABM
     */
    void setAbm(byte[] abm);

    /**
     * Get ARSN length
     *
     * @return ARSN length
     */
    Short getArsnLen();

    /**
     * Set ARSN length
     *
     * @param arsnLen ARSN length
     */
    void setArsnLen(Short arsnLen);

    /**
     * Get ARSN
     *
     * @return ARSN
     */
    byte[] getArsn();

    /**
     * Set ARSN
     *
     * @param arsn ARSN
     */
    void setArsn(byte[] arsn);

    /**
     * Get ARSNW
     *
     * @return ARSNW
     */
    Short getArsnw();

    /**
     * Set ARSNW
     *
     * @param arsnw ARSNW
     */
    void setArsnw(Short arsnw);

    /**
     * Get service type
     *
     * @return service type
     */
    ServiceType getServiceType();

    /**
     * Set service type
     *
     * @param serviceType service type
     */
    void setServiceType(ServiceType serviceType);

    /**
     * Get frame type
     *
     * @return frame type
     */
    FrameType getType();

    /**
     * Convenience method for setting the IV and IV Length
     *
     * @param length IV length
     * @param iv     IV
     */
    void setIv(Short length, byte[] iv);

    /**
     * Convenience method for setting the ACS and ACS length
     *
     * @param length ACS length
     * @param acs    ACS
     */
    void setAcs(Short length, byte[] acs);

    /**
     * Convenience method for setting the ECS and ECS length
     *
     * @param length ECS length
     * @param ecs    ECS
     */
    void setEcs(Short length, byte[] ecs);

    /**
     * Format object as JSON
     *
     * @return JSON string representing this SA
     */
    String toJson();
}
