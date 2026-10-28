package gov.nasa.jpl.ammos.kmc.crypto.model;

/**
 * Response from DCS ICV Verify Service.
 *
 *
 */
public class IcvVerifyServiceResponse {
    private final Status status;
    private final boolean result;

    /**
     * Constructor of the IcvVerifyServiceResponse.
     * @param status  The ICV Verify Service status.
     * @param result  Result of ICV verification.
     */
    public IcvVerifyServiceResponse(final Status status, final boolean result) {
        this.status = status;
        this.result = result;
    }

    /**
     * Returns the status of ICV verification.
     * @return Status of ICV verification.
     */
    public final Status getStatus() {
        return status;
    }

    /**
     * Returns the result of ICV verification.
     * @return true if the integrity of the input data is verified by the ICV.
     */
    public final boolean getResult() {
        return result;
    }
}
