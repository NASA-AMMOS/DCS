package gov.nasa.jpl.ammos.kmc.crypto.model;

/**
 * Fine-grained root cause of a failed service.  Unlike the exception class names and messages,
 * these values are owned by the crypto service and do not change with the Java version or crypto provider.
 *
 *
 */
public enum RootCause {
    /**
     * Authenticated decryption or verification failed because the authentication tag did not match.
     * Causes include a wrong key, IV, AAD or MAC length, or corrupted data.
     */
    AUTH_TAG_MISMATCH,
    /**
     * Decryption failed because the padding is invalid, usually a wrong key or corrupted data.
     */
    BAD_PADDING,
    /**
     * The length of the input data is not valid for the cipher.
     */
    ILLEGAL_BLOCK_SIZE,
    /**
     * The key cannot be used with the crypto algorithm.
     */
    INVALID_KEY,
    /**
     * A parameter of the crypto algorithm, such as the IV, is invalid.
     */
    INVALID_ALGORITHM_PARAMETER,
    /**
     * The crypto algorithm or transformation is not supported.
     */
    UNSUPPORTED_ALGORITHM,
    /**
     * Neither a keystore nor a KMS is configured.
     */
    NO_KEY_SOURCE,
    /**
     * The key could not be retrieved or does not match the crypto algorithm.
     */
    KEY_ERROR,
    /**
     * The key management service could not be reached.
     */
    KMS_UNREACHABLE,
    /**
     * The metadata could not be parsed.
     */
    METADATA_INVALID,
    /**
     * An input value is invalid.
     */
    INVALID_INPUT,
    /**
     * A request parameter or configuration value, such as the MAC length or cipher transformation, is invalid.
     */
    INVALID_PARAMETER,
    /**
     * The crypto configuration is missing or incomplete.
     */
    CONFIG_ERROR,
    /**
     * The crypto function failed for another reason.
     */
    ALGORITHM_ERROR,
    /**
     * The cause is not one of the known causes.
     */
    UNKNOWN
}
