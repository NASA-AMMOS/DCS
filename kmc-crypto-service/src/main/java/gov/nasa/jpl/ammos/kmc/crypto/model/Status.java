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

    /**
     * Constructor of the status.
     * @param httpCode  HTTP response code.
     * @param reason    Reason for failed status.
     */
    public Status(final int httpCode, final String reason) {
        this.httpCode = httpCode;
        this.reason = reason;
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

    @Override
    public final String toString() {
        return "Status(httpCode=" + httpCode + ", reason=" + reason + ")";
    }
}
