package gov.nasa.jpl.ammos.kmc.crypto.model;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Status response of the key service.
 *
 *
 */
public class Status {
    private final int httpCode;
    private final String reason;
    private final String errorCode;
    private final String rootCause;

    /**
     * Constructor of the status.
     * @param httpCode  HTTP response code.
     * @param reason    Reason for failed status.
     */
    public Status(final int httpCode, final String reason) {
        this(httpCode, reason, null, null);
    }

    /**
     * Constructor of the status with the details of a failure.
     * @param httpCode   HTTP response code.
     * @param reason     Reason for failed status.
     * @param errorCode  Error code of the KMC exception that caused the failure, or null.
     * @param rootCause  Fine-grained root cause of the failure, or null.
     */
    public Status(final int httpCode, final String reason, final String errorCode,
            final String rootCause) {
        this.httpCode = httpCode;
        this.reason = reason;
        this.errorCode = errorCode;
        this.rootCause = rootCause;
    }

    /**
     * Returns true if the service response is a success.
     * @return true if success, otherwise false.
     */
    public final boolean isSuccess() {
        return httpCode == HttpServletResponse.SC_OK;
    }

    /**
     * Returns of HTTP response code of the service.
     * @return Reason of the failed service.
     */
    public final int getHttpCode() {
        return httpCode;
    }

    /**
     * Returns of reason of the failed service.
     * @return Reason of the failed service.
     */
    public final String getReason() {
        return reason;
    }

    /**
     * Returns the error code of the KMC exception that caused the failure.
     * @return Error code of the failed service, or null.
     */
    public final String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the fine-grained root cause of the failure.
     * @return Root cause of the failed service, or null if not included.
     */
    public final String getRootCause() {
        return rootCause;
    }

    @Override
    public final String toString() {
        return "Status(httpCode=" + httpCode + ", reason=" + reason + ", errorCode=" + errorCode
                + ", rootCause=" + rootCause + ")";
    }
}
