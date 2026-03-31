package gov.nasa.jpl.ammos.kmc.keyclient;

import java.util.List;


/**
 * A client for operating keys in Key Management Server or keystore.
 *
 *
 */
public interface KmcKeyClient {
    /**
     * The suffix is used to distinguish the public and private key of the asymmetric key pair.
     */
    String PRIVATE_KEY_SUFFIX = "_:_private";

    /**
     * Locate a cryptographic key in KMS or keystore.
     * @param keyRef The key reference.
     * @return The KMIP unique identifier in KMS, or alias in keystore.
     * @throws KmcKeyClientException if error in locating the key.
     */
    String locateCryptoKey(final String keyRef) throws KmcKeyClientException;

    /**
     * Retrieves all the keys stored in KMS or keystore.
     * @return A list of keys.
     * @throws KmcKeyClientException if error occurred in retrieving keys.
     */
    List<KmcKey> getAllKeys() throws KmcKeyClientException;

    /**
     * Retrieves a key stored in KMS or keystore.
     * @param keyRef The key reference of the key.
     * @return The retrieved symmetric key.
     * @throws KmcKeyClientException if error occurred in retrieving the key.
     */
    KmcKey getKey(final String keyRef) throws KmcKeyClientException;

    /**
     * Retrieves a key stored in KMS or keystore by its unique identifier.
     * @param keyId The unique id of the key.
     * @return The retrieved symmetric key.
     * @throws KmcKeyClientException if error occurred in retrieving the key.
     */
    KmcKey getKeyById(final String keyId) throws KmcKeyClientException;

    /**
     * Retrieves a symmetric key stored in KMS or keystore.
     * @param keyRef The key reference of the key.
     * @return The retrieved symmetric key.
     * @throws KmcKeyClientException if error occurred in retrieving the key.
     */
    KmcKey getSymmetricKey(final String keyRef) throws KmcKeyClientException;

    /**
     * Retrieves a key stored in a keystore with a keyPass.
     * @param keyRef The key reference (i.e. alias) of the key.
     * @param keyPass The key password of the key.
     * @return The retrieved secret key.
     * @throws KmcKeyClientException if error occurred in retrieving the key.
     */
    KmcKey getKey(final String keyRef, final String keyPass) throws KmcKeyClientException;

    /**
     * Retrieves a symmetric key stored in a keystore.
     * @param keyRef The key reference (i.e. alias) of the key.
     * @param keyPass The key password of the key.
     * @return The retrieved secret key.
     * @throws KmcKeyClientException if error occurred in retrieving the key.
     */
    KmcKey getSymmetricKey(final String keyRef, final String keyPass) throws KmcKeyClientException;

    /**
     * Retrieves a public key stored in KMS or keystore.
     * @param keypairRef The key reference of the key pair.
     * @return The retrieved public key.
     * @throws KmcKeyClientException if error occurred in retrieving the key.
     */
    KmcKey getPublicKey(final String keypairRef) throws KmcKeyClientException;

    /**
     * Retrieves a private key stored in KMS or keystore.
     * @param keypairRef The key reference of the key pair.
     * @return The retrieved private key.
     * @throws KmcKeyClientException if error occurred in retrieving the key.
     */
    KmcKey getPrivateKey(final String keypairRef) throws KmcKeyClientException;

    /**
     * Creates a key in KMS used for encryption and decryption.
     * @param creator   Creator of the key.
     * @param keyRef The key reference of the key.
     * @param algorithm The key algorithm of the key.
     * @param keyLength The key length of the key.
     * @return The created key.
     * @throws KmcKeyClientException if error occurred in creating the key.
     */
    KmcKey createEncryptionKey(final String creator, final String keyRef,
            final String algorithm, final int keyLength) throws KmcKeyClientException;

    /**
     * Creates a key in KMS used for integrity check.
     * @param creator   Creator of the key.
     * @param keyRef The key reference of the key.
     * @param algorithm The key algorithm of the key.
     * @param keyLength The key length of the key.
     * @return The created key.
     * @throws KmcKeyClientException if error occurred in creating the key.
     */
    KmcKey createIntegrityCheckKey(final String creator, final String keyRef,
            final String algorithm, final int keyLength) throws KmcKeyClientException;

    /**
     * Creates an asymmetric key pair in KMS.
     * @param creator   Creator of the key.
     * @param keyRef The key reference of the key.
     * @param algorithm The key algorithm of the key.
     * @param keyLength The key length of the key.
     * @return The created key.
     * @throws KmcKeyClientException if error occurred in creating the key pair.
     */
    KmcKey createAsymmetricKeyPair(final String creator, final String keyRef,
            final String algorithm, final int keyLength) throws KmcKeyClientException;

    /**
     * Destroys a symmetric key or asymmetric key pair in KMS.
     * @param keyRef The key reference of the key.
     * @throws KmcKeyClientException if error occurred in destroying the key.
     */
    void destroyKey(final String keyRef) throws KmcKeyClientException;

    /**
     * Destroys a symmetric key in KMS.
     * @param keyRef The key reference of the key.
     * @throws KmcKeyClientException if error occurred in destroying the key.
     */
    void destroySymmetricKey(final String keyRef) throws KmcKeyClientException;

    /**
     * Destroys an asymmetric key pair in KMS.
     * @param keyRef The key reference of the key.
     * @throws KmcKeyClientException if error occurred in destroying the key pair.
     */
    void destroyAsymmetricKeyPair(final String keyRef) throws KmcKeyClientException;

}
