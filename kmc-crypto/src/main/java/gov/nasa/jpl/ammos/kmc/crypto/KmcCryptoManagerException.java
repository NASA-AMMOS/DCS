package gov.nasa.jpl.ammos.kmc.crypto;

import java.io.Serial;

/**
 * This exception is thrown by the ASEC KMC Crypto Manager to indicate that
 * errors in the KMC config file and for any other reasons.
 *
 *
 */
public class KmcCryptoManagerException extends Exception {
    @Serial
    private static final long serialVersionUID = 5316048143261131543L;
    /**
     * The error code of the KmcCryptoManagerException.
     */
    private final KmcCryptoManagerErrorCode errorCode;

    /**
     * Error code for KmcCryptoManager exceptions.
     *
     */
    public enum KmcCryptoManagerErrorCode {
        /**
         * The ASEC KMC Crypto Manager has not been initialized.
         */
        NOT_INITIALIZED,
        /**
         * The ASEC KMC Crypto config file not found.
         */
        CONFIG_FILE_NOT_FOUND,
        /**
         * The parameter is not found in the ASEC KMC Crypto config file.
         */
        CONFIG_PARAMETER_NOT_FOUND,
        /**
         * The value of the parameter in the config file or command-line argument is invalid.
         */
        CONFIG_PARAMETER_VALUE_INVALID,
        /**
         * Failed to retrieve key or wrong key algorithm.
         */
        CRYPTO_KEY_ERROR,
        /**
         * Wrong algorithm is used.
         */
        CRYPTO_ALGORITHM_ERROR,
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
    public KmcCryptoManagerException(final KmcCryptoManagerErrorCode errorCode,
                                     final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code of this exception.
     * @return The error code.
     */
    public final KmcCryptoManagerErrorCode getErrorCode() {
        return this.errorCode;
    }

}
