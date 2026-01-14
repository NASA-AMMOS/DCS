package gov.nasa.jpl.ammos.asec.kmc.api.sa;

/**
 * Factory for creating Security Association subtypes (TM, AOS, TC)
 */
public class SecAssnFactory {

    private SecAssnFactory() {

    }

    /**
     * Create a security association of the given ID and type
     *
     * @param id   spi/scid
     * @param type frame type
     * @return security association
     */
    public static ISecAssn createSecAssn(SpiScid id, FrameType type) {
        switch (type) {
            case TM -> {
                return new SecAssnTm(id);
            }
            case AOS -> {
                return new SecAssnAos(id);
            }
            default -> {
                return new SecAssn(id);
            }
        }
    }

    /**
     * Create a security association of the given type
     *
     * @param type frame type
     * @return security association
     */
    public static ISecAssn createSecAssn(FrameType type) {
        switch (type) {
            case TM -> {
                return new SecAssnTm();
            }
            case AOS -> {
                return new SecAssnAos();
            }
            default -> {
                return new SecAssn();
            }
        }
    }
}
