package gov.nasa.jpl.ammos.kmc.crypto.model;

import java.util.Base64;

/**
 * Response from DCS Encrypt Service.
 *
 *
 */
public class EncryptServiceResponse {
    private final Status status;
    private final String metadata;
    private final String base64ciphertext;

    /**
     * Constructor of the EncryptServiceResponse.
     * @param status  The Encrypt Service status.
     * @param metadata   The metadata produced by the Encrypter.
     * @param ciphertext The ciphertext produced by the Encrypter.
     */
    public EncryptServiceResponse(final Status status, final String metadata, final byte[] ciphertext) {
        this.status = status;
        this.metadata = metadata;
        this.base64ciphertext = Base64.getEncoder().encodeToString(ciphertext);
    }

    /**
     * Returns the status of decryption.
     * @return Status of decryption.
     */
    public final Status getStatus() {
        return status;
    }

    /**
     * Returns the metadata for encryption.
     * @return Metadata for encryption.
     */
    public final String getMetadata() {
        return metadata;
    }

    /**
     * Returns the encrypted data in base64 encoding.
     * @return Encrypted text in base64 encoding.
     */
    public final String getBase64Ciphertext() {
        return base64ciphertext;
    }

    /**
     * Returns the encrypted data.
     * @return Encrypted text.
     */
    public final byte[] getCiphertext() {
        return Base64.getDecoder().decode(base64ciphertext);
    }

    @Override
    public final String toString() {
        return "Status = " + status + ", Metadata = " + this.metadata + ", Base64 Ciphertext = " + this.base64ciphertext;
    }
}
