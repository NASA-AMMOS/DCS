package gov.nasa.jpl.ammos.kmc.keyclient;

/**
 * This exception is thrown by the ASEC KMC cryptographic API to indicate that
 * it was not initialized properly due to errors in the config file.
 *
 *
 */
public class KmcKeyClientException extends Exception {
    private static final long serialVersionUID = 7713300322251991392L;
    private final KmcKeyOpsErrorCode errorCode;

    /**
     * Error code for Crypto exceptions.
     *
     */
    public enum KmcKeyOpsErrorCode {
        /**
         * The ASEC KMC Crypto has not been initialized.
         */
        NOT_INITIALIZED,
        /**
         * The ASEC KMC config file is not found.
         */
        CONFIG_FILE_NOT_FOUND,
        /**
         * The parameter is not found in the ASEC KMS config.
         */
        CONFIG_PARAMETER_NOT_FOUND,
        /**
         * The value in config file or command-line argument is invalid.
         */
        CONFIG_VALUE_INVALID,
        /**
         * The input value is invalid.
         */
        INVALID_INPUT_VALUE,
        /**
         * Key not found, or Key does not match crypto algorithm.
         */
        KEY_OPERATION_ERROR,
        /**
         * The specified cryptographic algorithm is illegal.
         */
        KEY_ALGORITHM_ERROR,
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
    KmcKeyClientException(final KmcKeyOpsErrorCode errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code of this exception.
     * @return The error code.
     */
    public final KmcKeyOpsErrorCode getErrorCode() {
        return this.errorCode;
    }

}
