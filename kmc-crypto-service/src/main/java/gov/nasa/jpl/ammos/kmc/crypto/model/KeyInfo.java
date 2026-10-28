package gov.nasa.jpl.ammos.kmc.crypto.model;

/**
 * Response from DCS ICV Create Service.
 *
 *
 */
public class KeyInfo {
    public static final int USAGE_MASK_SIGN = 0x01;
    public static final int USAGE_MASK_VERIFY = 0x02;
    public static final int USAGE_MASK_ENCRYPT = 0x04;
    public static final int USAGE_MASK_DECRYPT = 0x08;
    public static final int USAGE_MASK_MAC_GENERATE = 0x0080;
    public static final int USAGE_MASK_MAC_VERIFY = 0x0100;

    private final String keyRef;
    private final String keyType;
    private final String algorithm;
    private final int keyLength;
    private final String state;
    private final int usageMask;

    /**
     * Constructor of the KeyInfo.
     * @param keyRef key reference.
     * @param keyType key type - symmetric or asymmetric
     * @param algorithm key algorithm
     * @param keyLength key length in bits
     * @param state key state - active, compromised, etc.
     * @param usageMask bit mask for the usage of the key.
     */
    public KeyInfo(final String keyRef, final String keyType, final String algorithm, final int keyLength,
            final String state, final int usageMask) {
        this.keyRef = keyRef;
        this.keyType = keyType;
        this.algorithm = algorithm;
        this.keyLength = keyLength;
        this.state = state;
        this.usageMask = usageMask;
    }

    /**
     * Returns the key reference.
     * @return key reference.
     */
    public final String getKeyRef() {
        return keyRef;
    }

    /**
     * Returns the key type.
     * @return key type.
     */
    public final String getType() {
        return keyType;
    }

    /**
     * Returns the key algorithm.
     * @return key algorithm.
     */
    public final String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the key length.
     * @return key length.
     */
    public final int keyLength() {
        return keyLength;
    }

    /**
     * Returns the key state.
     * @return key state.
     */
    public final String getState() {
        return state;
    }

    /**
     * Returns the usage mask of the key.
     * @return key usage mask.
     */
    public final int getUsageMask() {
        return usageMask;
    }
}
