package gov.nasa.jpl.ammos.kmc.crypto.model;

/**
 * Response from KMC ICV Create Service.
 *
 *
 */
public class IcvCreateServiceResponse {
    private final Status status;
    private final String metadata;

    /**
     * Constructor of the IcvCreateServiceResponse.
     * @param status  The ICV Create Service status.
     * @param metadata  The metadata containing the ICV and other crypto attributes such as key and algorithm.
     */
    public IcvCreateServiceResponse(final Status status, final String metadata) {
        this.status = status;
        this.metadata = metadata;
    }

    /**
     * Returns the status of ICV creation.
     * @return Status of ICV creation.
     */
    public final Status getStatus() {
        return status;
    }

    /**
     * Returns the metadata containing the ICV and other crypto attributes such as key and algorithm.
     * @return Metadata resulting from ICV creation.
     */
    public final String getMetadata() {
        return metadata;
    }
}
