package gov.nasa.jpl.ammos.kmc.crypto.model;

import java.util.Base64;

/**
 * Response from KMC Decrypt Service.
 *
 *
 */
public class DecryptServiceResponse {
    private final Status status;
    private final String base64cleartext;

    /**
     * Constructor of the DecryptServiceResponse.
     * @param status  The Decrypt Service status.
     * @param cleartext  The decrypted data.
     */
    public DecryptServiceResponse(final Status status, final byte[] cleartext) {
        this.status = status;
        this.base64cleartext = Base64.getEncoder().encodeToString(cleartext);
    }

    /**
     * Returns the status of decryption.
     * @return Status of decryption.
     */
    public final Status getStatus() {
        return status;
    }

    /**
     * Returns the decrypted data.
     * @return Decrypted data in a byte array.
     */
    public final byte[] getCleartext() {
        return Base64.getDecoder().decode(base64cleartext);
    }
}
