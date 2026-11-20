package gov.nasa.jpl.ammos.kmc.crypto;

import java.io.Serial;

/**
 * This exception is thrown by the ASEC KMC cryptographic API for any error occurred
 * during initialization or performing the cryptographic functions.
 *
 *
 */
public class KmcCryptoException extends Exception {
    @Serial
    private static final long serialVersionUID = 7713300322251991392L;
    /**
     * The error code of the KmcCryptoException.
     */
    private final KmcCryptoErrorCode errorCode;

    /**
     * Error code for Crypto exceptions.
     *
     */
    public enum KmcCryptoErrorCode {
        /**
         * Invalid input value.
         */
        INVALID_INPUT_VALUE,
        /**
         * Key not found, or key does not match crypto algorithm.
         */
        CRYPTO_KEY_ERROR,
        /**
         * Error occurred performing the crypto function.
         */
        CRYPTO_ALGORITHM_ERROR,
        /**
         * Error occurred parsing the metadata.
         */
        CRYPTO_METADATA_ERROR,
        /**
         * Miscellaneous error while performing cryptography.
         */
        CRYPTO_MISC_ERROR,
        /**
         * Failed to connect to the key management service.
         */
        KMS_CONNECTION_ERROR
    }

    /**
     * Constructor of the CryptoException.
     * @param errorCode The error code of this error.
     * @param message The detailed message of the error.
     * @param cause The underlying cause of the exception.
     */
    public KmcCryptoException(final KmcCryptoErrorCode errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code of this exception.
     * @return The error code.
     */
    public final KmcCryptoErrorCode getErrorCode() {
        return this.errorCode;
    }

    /**
     * Returns the fine-grained root cause of this exception, found from its cause chain.
     * @return The root cause, UNKNOWN if none of the exceptions in the chain is known.
     */
    public final KmcCryptoRootCause getRootCause() {
        return KmcCryptoFailures.getRootCause(this);
    }

}
