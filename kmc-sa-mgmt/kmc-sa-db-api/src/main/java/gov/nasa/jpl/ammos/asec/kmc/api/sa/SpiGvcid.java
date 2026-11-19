package gov.nasa.jpl.ammos.asec.kmc.api.sa;


import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import tools.jackson.core.JacksonException;

import java.io.Serializable;

/**
 * A globally unique identifier comprised of the Security Parameter Index (SPI) and the Global VCID (GVCID)
 */
@Embeddable
public class SpiGvcid implements Serializable {

    private static final ObjectMapper mapper = JsonMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    /**
     * SPI
     */
    private Integer spi;
    /**
     * GVCID
     */
    @Embedded
    @JsonIgnore
    private Gvcid   gvcid;

    /**
     * Constructor
     */
    public SpiGvcid() {
        this.gvcid = new Gvcid();
    }

    /**
     * Constructor
     *
     * @param spi   security parameter index
     * @param tfvn  transfer frame version number
     * @param scid  spacecraft id
     * @param vcid  virtual channel id
     * @param mapid multiplexer access id
     */
    public SpiGvcid(Integer spi, Byte tfvn, Short scid, Byte vcid, Byte mapid) {
        this(spi, new Gvcid(tfvn, scid, vcid, mapid));
    }

    /**
     * Constructor
     *
     * @param spi   security parameter index
     * @param gvcid global vcid
     */
    public SpiGvcid(Integer spi, Gvcid gvcid) {
        this.spi = spi;
        this.gvcid = gvcid;
    }

    @Override
    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the global vcid
     *
     * @return gvcid
     */
    public Gvcid getGvcid() {
        return gvcid;
    }

    /**
     * Set the global vcid
     *
     * @param gvcid gvcid
     */
    public void setGvcid(Gvcid gvcid) {
        this.gvcid = gvcid;
    }

    /**
     * Get the security parameter index
     *
     * @return spi
     */
    public Integer getSpi() {
        return spi;
    }

    /**
     * Set the security parameter index
     *
     * @param spi spi
     */
    public void setSpi(Integer spi) {
        this.spi = spi;
    }

    /**
     * Get the transfer frame version number
     *
     * @return tfvn
     */
    public Byte getTfvn() {
        return this.gvcid.getTfvn();
    }

    /**
     * Set the transfer frame version number
     *
     * @param tfvn tfvn
     */
    public void setTfvn(Byte tfvn) {
        this.gvcid.setTfvn(tfvn);
    }

    /**
     * Get the spacecraft id
     *
     * @return scid
     */
    public Short getScid() {
        return this.gvcid.getScid();
    }

    /**
     * Set the spacecraft id
     *
     * @param scid scid
     */
    public void setScid(Short scid) {
        this.gvcid.setScid(scid);
    }

    /**
     * Get the virtual channel id
     *
     * @return vcid
     */
    public Byte getVcid() {
        return this.gvcid.getVcid();
    }

    /**
     * Set the virtual channel id
     *
     * @param vcid vcid
     */
    public void setVcid(Byte vcid) {
        this.gvcid.setVcid(vcid);
    }

    /**
     * Get the multiplexer access id
     *
     * @return mapid
     */
    public Byte getMapid() {
        return this.gvcid.getMapid();
    }

    /**
     * Set the multiplexer access id
     *
     * @param mapid mapid
     */
    public void setMapid(Byte mapid) {
        this.gvcid.setMapid(mapid);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SpiGvcid)) {
            return false;
        }

        SpiGvcid id  = (SpiGvcid) o;
        boolean  ret = this.spi == id.getSpi();
        ret = ret && this.gvcid.equals(id.getGvcid());
        return ret;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + spi;
        result = 31 * result + gvcid.hashCode();
        return result;
    }

}
