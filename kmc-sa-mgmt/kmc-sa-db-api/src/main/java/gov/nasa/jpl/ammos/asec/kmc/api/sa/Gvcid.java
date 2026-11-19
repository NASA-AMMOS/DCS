package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.persistence.Embeddable;
import tools.jackson.core.JacksonException;

import java.io.Serializable;

/**
 * Global VCID, which is comprised of:
 * <p>
 * 1. Transfer Frame Version Number (TFVN)
 * 2. Spacecraft ID (SCID)
 * 3. Virtual Channel ID (VCID)
 * 4. Multiplexer Access ID (MAPID)
 */
@Embeddable
public class Gvcid implements Serializable {

    private static final ObjectMapper mapper = JsonMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    /**
     * TFVN
     */
    private Byte  tfvn;
    /**
     * SCID
     */
    private Short scid;
    /**
     * VCID
     */
    private Byte  vcid;
    /**
     * MAP ID
     */
    private Byte  mapid;

    /**
     * Constructor
     */
    public Gvcid() {

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
     * Constructor
     *
     * @param tfvn  transfer frame version number
     * @param scid  spacecraft id
     * @param vcid  virtual channel id
     * @param mapid multiplexer access id
     */
    public Gvcid(Byte tfvn, Short scid, Byte vcid, Byte mapid) {
        this.tfvn = tfvn;
        this.scid = scid;
        this.vcid = vcid;
        this.mapid = mapid;
    }

    /**
     * Get transfer frame version number
     *
     * @return tfvn
     */
    public Byte getTfvn() {
        return tfvn;
    }

    /**
     * Set transfer frame version number
     *
     * @param tfvn tfvn
     */
    public void setTfvn(Byte tfvn) {
        this.tfvn = tfvn;
    }

    /**
     * Get spacecraft id
     *
     * @return scid
     */
    public Short getScid() {
        return scid;
    }

    /**
     * Set spacecraft id
     *
     * @param scid scid
     */
    public void setScid(Short scid) {
        this.scid = scid;
    }

    /**
     * Get virtual channel id
     *
     * @return vcid
     */
    public Byte getVcid() {
        return vcid;
    }

    /**
     * Set virtual channel id
     *
     * @param vcid vcid
     */
    public void setVcid(Byte vcid) {
        this.vcid = vcid;
    }

    /**
     * Get multiplexer access id
     *
     * @return mapid
     */
    public Byte getMapid() {
        return mapid;
    }

    /**
     * Set multiplexer access id
     *
     * @param mapid mapid
     */
    public void setMapid(Byte mapid) {
        this.mapid = mapid;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Gvcid)) {
            return false;
        }

        Gvcid gvcid = (Gvcid) o;
        return this.tfvn.equals(gvcid.getTfvn()) &&
                this.vcid.equals(gvcid.getVcid()) &&
                this.scid.equals(gvcid.getScid()) &&
                this.mapid.equals(gvcid.getMapid());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + tfvn;
        result = 31 * result + vcid;
        result = 31 * result + scid;
        result = 31 * result + mapid;
        return result;
    }
}
