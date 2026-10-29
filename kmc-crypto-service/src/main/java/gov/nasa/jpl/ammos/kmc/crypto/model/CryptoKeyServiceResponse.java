package gov.nasa.jpl.ammos.kmc.crypto.model;

/**
 * Response from DCS Key Info Service.
 *
 *
 */
public class CryptoKeyServiceResponse {
    private final Status status;
    private KeyInfo keyInfo;

    /**
     * Constructor of the CryptoKeyServiceResponse.
     * @param status  The KeyInfo service status.
     */
    public CryptoKeyServiceResponse(final Status status) {
        this.status = status;
    }

    /**
     * Returns the key info.
     * @return information about the key.
     */
    public final KeyInfo getKeyInfo() {
        return keyInfo;
    }

    /**
     * Sets the value of KeyInfo.
     * @param keyInfo The keyInfo value.
     */
    public final void setKeyInfo(final KeyInfo keyInfo) {
        this.keyInfo = keyInfo;
    }

    /**
     * Returns the status of KeyInfo service.
     * @return Status of KeyInfo service.
     */
    public final Status getStatus() {
        return status;
    }
}
