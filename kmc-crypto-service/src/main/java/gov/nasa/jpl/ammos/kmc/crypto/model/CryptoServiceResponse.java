package gov.nasa.jpl.ammos.kmc.crypto.model;



/**
 * Response from DCS Crypto Service.
 *
 *
 */
public class CryptoServiceResponse {
    private final Status status;
    private final Object result;

    /**
     * Constructor of the CryptoServiceResponse.
     * @param status  The Crypto Service status.
     * @param result  The Crypto Service result.
     */
    public CryptoServiceResponse(final Status status, final Object result) {
        this.status = status;
        this.result = result;
    }

    /**
     * Returns the metadata for encryption.
     * @return Metadata for encryption.
     */
    public final Status getStatus() {
        return status;
    }

    /**
     * Returns the encrypted data.
     * @return Encrypted data in base64.
     */
    public final Object getResult() {
        return result;
    }
}
