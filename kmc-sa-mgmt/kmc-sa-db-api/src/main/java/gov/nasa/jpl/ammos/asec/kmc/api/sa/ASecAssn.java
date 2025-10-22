package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.nasa.jpl.ammos.asec.kmc.api.json.ByteArrayDeserializer;
import gov.nasa.jpl.ammos.asec.kmc.api.json.ByteArraySerializer;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

import java.util.Objects;

/**
 * Abstract Security Association
 */
@MappedSuperclass
abstract class ASecAssn implements ISecAssn {
    private static final ObjectMapper mapper    = new ObjectMapper();
    /**
     * SPI SCID
     */
    @EmbeddedId
    protected            SpiScid      id;
    // transfer frame version number
    private              Byte         tfvn;
    // virtual channel id
    private              Byte         vcid;
    // multiplexer access point id
    private              Byte         mapid;
    // encryption key id (ref)
    @Column(length = 100)
    private              String       ekid;
    // authentication key id (ref)
    @Column(length = 100)
    private              String       akid;
    // sa state
    @Column(name = "sa_state", nullable = false)
    private              Short        saState   = 1;
    // ??
    private              Short        lpid;
    // encryption service type
    private              Short        est       = 0;
    // authentication service type
    private              Short        ast       = 0;
    // security header iv field len
    @Column(name = "shivf_len")
    private              Short        shivfLen  = 12;
    // security header sn field len
    @Column(name = "shsnf_len")
    private              Short        shsnfLen  = 0;
    // security header pl field len
    @Column(name = "shplf_len")
    private              Short        shplfLen  = 0;
    // security trailer mac field len
    @Column(name = "stmacf_len")
    private              Short        stmacfLen = 0;
    // encryption cipher suite len
    @Column(name = "ecs_len")
    private              Short        ecsLen    = 1;
    // encryption cipher suite (algorithm / mode id)
    @JsonSerialize(using = ByteArraySerializer.class)
    @JsonDeserialize(using = ByteArrayDeserializer.class)
    private              byte[]       ecs       = ECSTYPE.AES_GCM.getValue();
    // initialization vector len
    @Column(name = "iv_len")
    private              Short        ivLen     = 12;
    // initialization vector
    @JsonSerialize(using = ByteArraySerializer.class)
    @JsonDeserialize(using = ByteArrayDeserializer.class)
    private              byte[]       iv        = new byte[]{};
    //before AKMC-239, IV would default to: new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    // 0x00, 0x00};
    // authentication cipher suite len
    @Column(name = "acs_len")
    private              Short        acsLen    = 0;
    // authentication cipher suite (algorithm / mode id)
    @JsonSerialize(using = ByteArraySerializer.class)
    @JsonDeserialize(using = ByteArrayDeserializer.class)
    private              byte[]       acs       = new byte[]{0x00};
    // authentication bit mask len
    @Column(name = "abm_len")
    private              Integer      abmLen    = 19;
    // authentication bit mask (primary header through security header)
    @JsonSerialize(using = ByteArraySerializer.class)
    @JsonDeserialize(using = ByteArrayDeserializer.class)
    @Column(name = "abm")
    private              byte[]       abm       = new byte[]{0x00,
            0x00,
            (byte) 0xFC,
            0x00,
            0x00,
            (byte) 0xFF,
            (byte) 0xFF,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00};
    // anti replay counter len
    @Column(name = "arsn_len")
    private              Short        arsnLen   = 20;
    // anti replay counter
    @JsonSerialize(using = ByteArraySerializer.class)
    @JsonDeserialize(using = ByteArrayDeserializer.class)
    private              byte[]       arsn      = new byte[]{0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00};
    // anti replay counter window
    private              Short        arsnw     = 0;

    /**
     * Constructor
     *
     * @param id spi/scid
     */
    public ASecAssn(SpiScid id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("SA [%s, %d, %d, %d, %d]", getType().name(), getSpi(), getScid(), getVcid(), getTfvn());
    }

    @Override
    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setId(SpiScid id) {
        this.id = id;
    }

    @Override
    public SpiScid getId() {
        return id;
    }

    @Override
    public void setAkid(String akid) {
        this.akid = akid;
    }

    @Override
    public String getAkid() {
        return akid;
    }

    @Override
    public Integer getSpi() {
        return id.getSpi();
    }

    @Override
    public void setSpi(Integer spi) {
        this.id.setSpi(spi);
    }

    @Override
    public String getEkid() {
        return ekid;
    }

    @Override
    public void setEkid(String ekid) {
        this.ekid = ekid;
    }

    @Override
    public Short getSaState() {
        return saState;
    }

    @Override
    public void setSaState(Short saState) {
        this.saState = saState;
    }

    @Override
    public Byte getTfvn() {
        return tfvn;
    }

    @Override
    public void setTfvn(Byte tfvn) {
        this.tfvn = tfvn;
    }

    @Override
    public Short getScid() {
        return id.getScid();
    }

    @Override
    public void setScid(Short scid) {
        this.id.setScid(scid);
    }

    @Override
    public Byte getVcid() {
        return this.vcid;
    }

    @Override
    public void setVcid(Byte vcid) {
        this.vcid = vcid;
    }

    @Override
    public Byte getMapid() {
        return this.mapid;
    }

    @Override
    public void setMapid(Byte mapid) {
        this.mapid = mapid;
    }

    @Override
    public Short getLpid() {
        return lpid;
    }

    @Override
    public void setLpid(Short lpid) {
        this.lpid = lpid;
    }

    @Override
    public Short getEst() {
        return est;
    }

    @Override
    public void setEst(Short est) {
        this.est = est;
    }

    @Override
    public Short getAst() {
        return ast;
    }

    @Override
    public void setAst(Short ast) {
        this.ast = ast;
    }

    @Override
    public Short getShivfLen() {
        return shivfLen;
    }

    @Override
    public void setShivfLen(Short shivfLen) {
        this.shivfLen = shivfLen;
    }

    @Override
    public Short getShsnfLen() {
        return shsnfLen;
    }

    @Override
    public void setShsnfLen(Short shsnfLen) {
        this.shsnfLen = shsnfLen;
    }

    @Override
    public Short getShplfLen() {
        return shplfLen;
    }

    @Override
    public void setShplfLen(Short shplfLen) {
        this.shplfLen = shplfLen;
    }

    @Override
    public Short getStmacfLen() {
        return stmacfLen;
    }

    @Override
    public void setStmacfLen(Short stmacfLen) {
        this.stmacfLen = stmacfLen;
    }

    @Override
    public Short getEcsLen() {
        return ecsLen;
    }

    @Override
    public void setEcsLen(Short ecsLen) {
        this.ecsLen = ecsLen;
    }

    @Override
    public byte[] getEcs() {
        return ecs;
    }

    @Override
    public void setEcs(byte[] ecs) {
        this.ecs = ecs;
    }

    @Override
    public Short getIvLen() {
        return ivLen;
    }

    @Override
    public void setIvLen(Short ivLen) {
        this.ivLen = ivLen;
    }

    @Override
    public byte[] getIv() {
        return iv;
    }

    @Override
    public void setIv(byte[] iv) {
        if (iv != null) {
            this.iv = iv;
        }
    }

    @Override
    public Short getAcsLen() {
        return acsLen;
    }

    @Override
    public void setAcsLen(Short acsLen) {
        this.acsLen = acsLen;
    }

    @Override
    public byte[] getAcs() {
        return acs;
    }

    @Override
    public void setAcs(byte[] acs) {
        this.acs = acs;
    }

    @Override
    public Integer getAbmLen() {
        return abmLen;
    }

    @Override
    public void setAbmLen(int abmLen) {
        this.abmLen = abmLen;
    }

    @Override
    public byte[] getAbm() {
        return abm;
    }

    @Override
    public void setAbm(byte[] abm) {
        this.abm = abm;
    }

    @Override
    public Short getArsnLen() {
        return arsnLen;
    }

    @Override
    public void setArsnLen(Short arsnLen) {
        this.arsnLen = arsnLen;
    }

    @Override
    public byte[] getArsn() {
        return arsn;
    }

    @Override
    public void setArsn(byte[] arsn) {
        if (arsn != null) {
            this.arsn = arsn;
        }
    }

    @Override
    public Short getArsnw() {
        return arsnw;
    }

    @Override
    public void setArsnw(Short arsnw) {
        this.arsnw = arsnw;
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.getServiceType(getEst(), getAst());
    }

    @Override
    public void setServiceType(ServiceType serviceType) {
        this.setEst(serviceType.getEncryptionType());
        this.setAst(serviceType.getAuthenticationType());
    }

    @Override
    public void setIv(Short length, byte[] iv) {
        byte[] updateIv = Objects.requireNonNullElseGet(iv, () -> new byte[length]);
        setIvLen(length);
        setIv(updateIv);
    }

    @Override
    public void setAcs(Short length, byte[] acs) {
        byte[] updateAcs = Objects.requireNonNullElseGet(acs, () -> new byte[length]);
        setAcsLen(length);
        setAcs(updateAcs);
    }

    @Override
    public void setEcs(Short length, byte[] ecs) {
        byte[] updateEcs = Objects.requireNonNullElseGet(ecs, () -> new byte[length]);
        setEcsLen(length);
        setEcs(updateEcs);
    }
}
