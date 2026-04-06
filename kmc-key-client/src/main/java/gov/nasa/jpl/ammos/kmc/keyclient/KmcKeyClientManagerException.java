package gov.nasa.jpl.ammos.kmc.keyclient;

/**
 * This exception is thrown by the ASEC KMC Crypto Manager to indicate that
 * errors in the KMC config file and for any other reasons.  Unknown parameters
 * are ignored.
 *
 *
 */
public class KmcKeyClientManagerException extends Exception {
    private static final long serialVersionUID = 5316048143261131543L;
    private final KmcKeyOpsManagerErrorCode errorCode;

    /**
     * Error code for KmcCryptoManager exceptions.
     *
     */
    public enum KmcKeyOpsManagerErrorCode {
        /**
         * The ASEC KMS has not been initialized.
         */
        NOT_INITIALIZED,
        /**
         * The ASEC KMS config file is not found.
         */
        CONFIG_FILE_NOT_FOUND,
        /**
         * The parameter is not found in the ASEC KMS config file.
         */
        CONFIG_PARAMETER_NOT_FOUND,
        /**
         * The value of the parameter in the config file or command-line argument is invalid.
         */
        CONFIG_PARAMETER_VALUE_INVALID,
        /**
         * Unexpected null value.
         */
        NULL_VALUE
    }

    /**
     * Constructor of the KmcCryptoManagerException.
     * @param errorCode The error code of this error.
     * @param message The detailed message of the error.
     * @param cause The underlying cause of the exception.
     */
    public KmcKeyClientManagerException(final KmcKeyOpsManagerErrorCode errorCode,
                                     final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code of this exception.
     * @return The error code.
     */
    public final KmcKeyOpsManagerErrorCode getErrorCode() {
        return this.errorCode;
    }

}
