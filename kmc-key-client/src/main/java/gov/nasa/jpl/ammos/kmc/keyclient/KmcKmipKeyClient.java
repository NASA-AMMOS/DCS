package gov.nasa.jpl.ammos.kmc.keyclient;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.keyclient.KmcKeyClientException.KmcKeyOpsErrorCode;

import ch.ntb.inf.kmip.attributes.ContactInformation;
import ch.ntb.inf.kmip.attributes.CryptographicAlgorithm;
import ch.ntb.inf.kmip.attributes.CryptographicLength;
import ch.ntb.inf.kmip.attributes.CryptographicUsageMask;
import ch.ntb.inf.kmip.attributes.InitialDate;
import ch.ntb.inf.kmip.attributes.KMIPAttributeValue;
import ch.ntb.inf.kmip.attributes.LastChangeDate;
import ch.ntb.inf.kmip.attributes.Name;
import ch.ntb.inf.kmip.attributes.ObjectType;
import ch.ntb.inf.kmip.attributes.RevocationReason;
import ch.ntb.inf.kmip.attributes.State;
import ch.ntb.inf.kmip.attributes.UniqueIdentifier;
import ch.ntb.inf.kmip.container.KMIPBatch;
import ch.ntb.inf.kmip.container.KMIPContainer;
import ch.ntb.inf.kmip.kmipenum.EnumCryptographicAlgorithm;
import ch.ntb.inf.kmip.kmipenum.EnumObjectType;
import ch.ntb.inf.kmip.kmipenum.EnumOperation;
import ch.ntb.inf.kmip.kmipenum.EnumResultStatus;
import ch.ntb.inf.kmip.kmipenum.EnumRevocationReasonCode;
import ch.ntb.inf.kmip.kmipenum.EnumState;
import ch.ntb.inf.kmip.objects.base.Attribute;
import ch.ntb.inf.kmip.objects.base.CommonTemplateAttribute;
import ch.ntb.inf.kmip.objects.base.PrivateKeyTemplateAttribute;
import ch.ntb.inf.kmip.objects.base.PublicKeyTemplateAttribute;
import ch.ntb.inf.kmip.objects.base.TemplateAttribute;
import ch.ntb.inf.kmip.objects.base.TemplateAttributeStructure;
import ch.ntb.inf.kmip.objects.managed.CryptographicObject;
import ch.ntb.inf.kmip.objects.managed.ManagedObject;
import ch.ntb.inf.kmip.objects.managed.PrivateKey;
import ch.ntb.inf.kmip.objects.managed.PublicKey;
import ch.ntb.inf.kmip.objects.managed.SymmetricKey;
import ch.ntb.inf.kmip.stub.KMIPStub;
import ch.ntb.inf.kmip.types.KMIPTextString;

/**
 * A {@link KmcKeyClient} that manages keys at the Key Management Server.
 *
 *
 */
public class KmcKmipKeyClient implements KmcKeyClient {

    private final KMIPStub kmipServer;

    private static final Logger logger = LoggerFactory.getLogger(KmcKmipKeyClient.class);

    /**
     * Constructor of KmcKeyClient for managing keys in the KMS.
     * @param manager The KMS Manager for providing config parameters.
     * @throws KmcKeyClientException if errors getting the config parameters.
     */
    public KmcKmipKeyClient(final KmcKeyClientManager manager) throws KmcKeyClientException {
        Properties configParams = manager.getConfigParameters();
        Map<String, Object> configMap = new HashMap<String, Object>();
        for (final String name: configParams.stringPropertyNames()) {
            configMap.put(name, configParams.getProperty(name));
        }
        try {
            kmipServer = new KMIPStub(configMap);
        } catch (Exception e) {
            String msg = "Failed to create transport for KMIP server: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KMS_CONNECTION_ERROR, msg, e);
        }
    }

    @Override
    public final String locateCryptoKey(final String keyRef) throws KmcKeyClientException {
        return locateCryptographicKey(keyRef);
    }

    private String locatePrivateKey(final String keyRef) throws KmcKeyClientException {
        return locateCryptographicKey(keyRef + PRIVATE_KEY_SUFFIX);
    }

    /**
     * Returns the KMIP unique identifiers of all the keys exist in KMS.
     * The destroyed keys will not be included.
     * @return a list of keyIds.
     * @throws KmcKeyClientException if error in retrieving all keyIds.
     */
    public final List<String> locateAllKeyIds() throws KmcKeyClientException {
        List<String> keyIds = new ArrayList<String>();
        KMIPContainer request = createLocateKeyRequest(null);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in locating all key ids from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        logger.debug("locateAllKeyIds() response = {}", response);
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                for (Attribute a : b.getAttributes()) {
                    if (a instanceof UniqueIdentifier) {
                        KMIPAttributeValue[] values = a.getValues();
                        if (values.length > 0) {
                            logger.debug("locateAllKeyIds(): " + a.getAttributeName() + " = " + values[0].getValueString());
                            keyIds.add(values[0].getValueString());
                        }
                    }
                }
            } else {
                if (! logger.isDebugEnabled()) {
                    logger.error("locateAllKeyIds() response = {}", response);
                }
                logger.error("Operation Locate for all key ids failed.");
                return null;
            }
        }
        logger.debug("locateAllKeyIds() keyIds = {}", keyIds);
        return keyIds;
    }

    @Override
    public final List<KmcKey> getAllKeys() throws KmcKeyClientException {
        List<KmcKey> keys = new ArrayList<KmcKey>();
        List<String> keyIds = locateAllKeyIds();
        if (keyIds == null) {
            logger.info("getAllKeys() return an empty list of keys");
            return keys;
        }
        logger.debug("getAllKeys() keyIds = {}", keyIds);
        for (String keyId : keyIds) {
            KmcKey key;
            try {
                key = getKeyById(keyId);
                if (key == null) {
                    logger.error("Error in getting attributes for key {}", keyId);
                    // failed to get key attributes, key will only have keyId
                    key = new KmcKey(keyId);
                }
            } catch (KmcKeyClientException e) {
                logger.error("Exception in getting attributes for key {}: {}", keyId, e.getMessage());
                // failed to get key attributes, key will only have keyId
                key = new KmcKey(keyId);
            }
            keys.add(key);
        }
        return keys;
    }

    /**
     * Retrieves the key from its keyId.
     * @param keyId The KMIP unique identifier of the key.
     * @return The KmcKey of the retrieved key.
     * @throws KmcKeyClientException if error in retrieving the key.
     */
    @Override
    public final KmcKey getKeyById(final String keyId) throws KmcKeyClientException {
        if (keyId == null) {
            String msg = "The input keyId cannot be null.";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.INVALID_INPUT_VALUE, msg, null);
        }
        logger.debug("getKeyById() keyId = {}", keyId);
        CryptographicObject cryptoObject = getCryptographicObject(keyId);
        if (cryptoObject == null) {
            String msg = "The retrieved key is null.";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        }
        KmcKey key = new KmcKey(keyId);
        key.setCryptographicObject(cryptoObject);
        List<Attribute> attributes = getAllAttributes(keyId);
        key.setAttributes(attributes);
        return key;
    }

    @Override
    public final KmcKey getKey(final String keyRef) throws KmcKeyClientException {
        String keyId = locateCryptoKey(keyRef);
        if (keyId == null) {
            String msg = "The key, " + keyRef + ", does not exist.";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.INVALID_INPUT_VALUE, msg, null);
        } else {
            return getKeyById(keyId);
        }
    }

    @Override
    public final KmcKey getSymmetricKey(final String keyRef) throws KmcKeyClientException {
        String keyId = locateCryptographicKey(keyRef);
        if (keyId == null) {
            String msg = "The key, " + keyRef + ", does not exist.";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        } else {
            return getKeyById(keyId);
        }
    }

    @Override
    public final KmcKey getPublicKey(final String keypairRef) throws KmcKeyClientException {
        String keyId = locateCryptographicKey(keypairRef);
        if (keyId == null) {
            String msg = "The key, " + keypairRef + ", does not exist.";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        } else {
            return getKeyById(keyId);
        }
    }

    @Override
    public final KmcKey getPrivateKey(final String keypairRef) throws KmcKeyClientException {
        String keyId = locatePrivateKey(keypairRef);
        if (keyId == null) {
            String msg = "The key, " + keypairRef + ", does not exist.";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        } else {
            return getKeyById(keyId);
        }
    }

    @Override
    public final KmcKey getKey(final String keyRef, final String keyPass) throws KmcKeyClientException {
        String msg = "This method getKey() is for keystore only";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    @Override
    public final KmcKey getSymmetricKey(final String keyRef, final String keyPass) throws KmcKeyClientException {
        String msg = "This method getSymmetricKey() is for keystore only";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }
/*
    @Override
    public java.security.PublicKey getPublicKey(final String keyRef) throws KmcKeyClientException {
        CryptographicObject kmipKey = getCryptographicObject(keyRef, EnumObjectType.PublicKey);
        if (kmipKey == null) {
            return null;
        }
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance(KmipUtils.getKeyAlgorithm(kmipKey));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Exception " + e);
        }
        try {
            EncodedKeySpec ks = new X509EncodedKeySpec(KmipUtils.getKeyMaterial(kmipKey));
            return keyFactory.generatePublic(ks);
        } catch (InvalidKeySpecException e) {
            logger.error("Exception " + e);
            return null;
        }
    }

    @Override
    public java.security.PrivateKey getPrivateKey(final String keyRef) throws KmcKeyClientException {
        CryptographicObject kmipKey = getCryptographicObject(keyRef, EnumObjectType.PrivateKey);
        if (kmipKey == null) {
            return null;
        }
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance(KmipUtils.getKeyAlgorithm(kmipKey));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Exception " + e);
        }
        try {
            EncodedKeySpec ks = new PKCS8EncodedKeySpec(KmipUtils.getKeyMaterial(kmipKey));
            return keyFactory.generatePrivate(ks);
        } catch (InvalidKeySpecException e) {
            logger.error("Exception " + e);
            return null;
        }
    }

    public byte[] getPublicKeyMaterial(final String keyRef) throws KmcKeyClientException {
        CryptographicObject key = getCryptographicObject(keyRef, EnumObjectType.PublicKey);
        return KmipUtils.getKeyMaterial(key);

    }

    public byte[] getPrivateKeyMaterial(final String keyRef) throws KmcKeyClientException {
        PrivateKey key = (PrivateKey) getCryptographicObject(keyRef, EnumObjectType.PrivateKey);
        return key.getKeyBlock().getKeyValue().getKeyMaterial().getKeyMaterialByteString().getValue();
    }

    @Override
    public final KmcKey createEncryptionKey(final String keyRef) throws KmcKeyClientException {
        return createEncryptionKey(keyRef, DEFAULT_SYMMETRIC_KEY_ALGORITHM, DEFAULT_SYMMETRIC_KEY_LENGTH);
    }
*/
    @Override
    public final KmcKey createEncryptionKey(final String creator, final String keyRef, final String algorithm,
            final int keyLength) throws KmcKeyClientException {
        int usageMask = KmcKey.USAGE_MASK_ENCRYPTION;
        return createSymmetricKey(creator, keyRef, algorithm, keyLength, usageMask);
    }

/*
    @Override
    public final KmcKey createIntegrityCheckKey(final String keyRef) throws KmcKeyClientException {
        return createIntegrityCheckKey(keyRef, DEFAULT_INTEGRITY_CHECK_KEY_ALGORIGHTM, DEFAULT_INTEGRITY_CHECK_KEY_LENGTH);
    }
*/
    @Override
    public final KmcKey createIntegrityCheckKey(final String creator, final String keyRef,
            final String algorithm, final int keyLength) throws KmcKeyClientException {
        int usageMask = KmcKey.USAGE_MASK_ICV;
        return createSymmetricKey(creator, keyRef, algorithm, keyLength, usageMask);
    }

    private KmcKey createSymmetricKey(final String creator, final String keyRef, final String algorithm,
            final int keyLength, final int usageMask) throws KmcKeyClientException {
        // if keyRef exists, kmip4j creates another key with the same keyRef but different keyId
        if (locateCryptographicKey(keyRef) != null) {
            logger.error("Symmetric key \"" + keyRef + "\" exists, will not create it again.");
            return null;
        }
        KMIPContainer request = createSymmetricKeyRequest(
                creator, keyRef, algorithm, keyLength, usageMask);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in Create symmetric key from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        if (response == null) {
            String msg = "Null response in Create symmetric key: " + keyRef;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("Operation Create symmetric key \"" + keyRef + "\" done.");
                String keyId = null;
                for (Attribute a : b.getAttributes()) {
                    if (a instanceof UniqueIdentifier) {
                        KMIPAttributeValue[] values = a.getValues();
                        if (values.length > 0) {
                            logger.debug(a.getAttributeName() + ": " + values[0].getValueString());
                            keyId = values[0].getValueString();
                        }
                    }
                }
                if (keyId == null) {
                    String msg = "Create symmetric key \"" + keyRef + "\" did not return a unique identifier."
                                    + "\nResponse:\n" + response.toString();
                    logger.error(msg);
                    throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
                } else {
                    return getKeyById(keyId);
                }
                /*
                KmcKey kmcKey = new KmcKey(this, keyRef, keyId, EnumObjectType.SymmetricKey);
                CryptographicObject object = getCryptographicObject(keyRef, EnumObjectType.SymmetricKey);
                if (object == null) {
                    String msg = "Failed to create CryptographicObject for symmetric key \"" + keyRef;
                    logger.error(msg);
                    throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
                }
                kmcKey.setCryptographicObject(object);
                return kmcKey;
                */
            } else {
                String msg = "Create symmetric key \"" + keyRef + "\" failed.";
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        String msg = "Create symmetric key \"" + keyRef + "\" has empty response.";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    @Override
    public final KmcKey createAsymmetricKeyPair(final String creator, final String keyRef,
            final String algorithm, final int keyLength) throws KmcKeyClientException {
        if (locateCryptographicKey(keyRef) != null) {
            logger.error("Key Pair \"" + keyRef + "\" exists, will not create it again.");
            return null;
        }
        KMIPContainer request = createKeyPairRequest(creator, keyRef, algorithm, keyLength);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in Create asymmetric key pair from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        if (response == null) {
            String msg = "Null response in Create asymmetric key pair: " + keyRef;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        }

        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("Operation Create asymmetric key pair \"" + keyRef + "\" done.");
                String keyId = null;
                for (Attribute a : b.getAttributes()) {
                    if (a instanceof UniqueIdentifier) {
                        KMIPAttributeValue[] values = a.getValues();
                        if (values.length > 0) {
                            logger.debug(a.getAttributeName() + ": " + values[0].getValueString());
                            keyId = values[0].getValueString();
                        }
                    }
                }
                if (keyId == null) {
                    String msg = "Create asymmetric keypair \"" + keyRef + "\" did not return a unique identifier."
                                    + "\nResponse:\n" + response.toString();
                    logger.error(msg);
                    throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
                } else if (keyId.startsWith("PublicKey")) {  // maybe KMIP4J specific
                    return getKeyById(keyId);
                }
            } else {
                String msg = "Create asymmetric keypair \"" + keyRef + "\" failed.";
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        String msg = "Create asymmetric keypair \"" + keyRef + "\" has empty response.";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    @Override
    public final void destroyKey(final String keyRef) throws KmcKeyClientException {
        KmcKey key = getKey(keyRef);
        String keyType = key.getKeyType();
        if ("SymmetricKey".equals(keyType)) {
            destroyCryptographicKey(key.getKeyId());
        } else if ("PublicKey".equals(keyType)) {
            destroyCryptographicKey(key.getKeyId());
            destroyPrivateKey(keyRef);
        } else {
            String msg = "Key \"" + keyRef + "\" has unknown key type: " + keyType;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        }
    }

    @Override
    public final void destroySymmetricKey(final String keyRef) throws KmcKeyClientException {
        String uniqueIdentifier = locateCryptoKey(keyRef);
        if (uniqueIdentifier == null) {
            String msg = "Key \"" + keyRef + "\" does not exist.";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        }
        destroyCryptographicKey(uniqueIdentifier);
    }

    @Override
    public final void destroyAsymmetricKeyPair(final String keyRef) throws KmcKeyClientException {
        destroyPublicKey(keyRef);
        destroyPrivateKey(keyRef);
    }

    private void destroyPublicKey(final String keyRef) throws KmcKeyClientException {
        String uniqueIdentifier = locateCryptographicKey(keyRef);
        if (uniqueIdentifier == null) {
            String msg = "Public Key \"" + keyRef + "\" does not exist.";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        }
        destroyCryptographicKey(uniqueIdentifier);
    }

    private void destroyPrivateKey(final String keyRef) throws KmcKeyClientException {
        String uniqueIdentifier = locatePrivateKey(keyRef);
        if (uniqueIdentifier == null) {
            String msg = "Private Key \"" + keyRef + "\" does not exist.";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        }
        destroyCryptographicKey(uniqueIdentifier);
    }

    /**
     * Retrieves the attributes of the key from its keyId.
     * @param keyId The KMIP unique identifier of the key.
     * @return The list of key attributes.
     * @throws KmcKeyClientException if error in retrieving the key.
     */
    public final List<Attribute> getAllAttributes(final String keyId) throws KmcKeyClientException {
        List<Attribute> attributes = new ArrayList<Attribute>();
        KMIPContainer request = createGetAllAttributesRequest(keyId);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in creating key from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("getAllAttributes() for \"" + keyId + "\" done.");
                logger.debug(response.toString());
                for (Attribute a : b.getAttributes()) {
                    logger.debug("Attribute: " + a);
                    attributes.add(a);
                }
            } else {
                String msg = "Operation Get Attributes for key \"" + keyId + "\" failed."
                                + "\nResponse: " + response.toString();
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        return attributes;
    }

    /**
     * Returns the state of the key in the KMS.
     * @param keyRef The key reference of the key.
     * @param keyId The KMIP unique identifier of the key.
     * @return The state of key.
     * @throws KmcKeyClientException if error in retrieving the key.
     */
    public final String getKeyState(final String keyRef, final String keyId) throws KmcKeyClientException {
        KMIPContainer request = createGetStateRequest(keyId);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in get key state from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                for (Attribute a : b.getAttributes()) {
                    if (a instanceof State) {
                        KMIPAttributeValue[] values = a.getValues();
                        if (values.length > 0) {
                            String state = values[0].getValueString();
                            logger.debug("getState() for " + keyRef + " = " + state);
                            return new EnumState(state).getKey();
                        }
                    }
                }
            } else {
                String msg = "Operation Get State for key \"" + keyRef + "\" failed."
                                + "\nResponse: " + response.toString();
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        String msg = "Operation Get Attribute State for key \"" + keyRef + "\" has no response.";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * Returns the state of the key pair from KMS.
     * @param keyRef The key reference of the key pair.
     * @param publicKeyId The KMIP unique identifier of the public key.
     * @return The state of key.
     * @throws KmcKeyClientException if error in retrieving the key.
     */
    public final String getKeyPairState(final String keyRef, final String publicKeyId)
            throws KmcKeyClientException {
        String publicKeyState = getKeyState(keyRef, publicKeyId);
        String privateKeyId = locatePrivateKey(keyRef);
        if (privateKeyId == null) {
            String msg = "getKeyPairState() fails to locate private key";
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        }
        String privateKeyState = getKeyState(keyRef, privateKeyId);
        logger.debug("publicKeyState = " + publicKeyState + ", privateKeyState = " + privateKeyState);
        if (publicKeyState.equals(privateKeyState)) {
            return publicKeyState;
        } else {
            String msg = "getKeyPairState() has different states for public and private key: "
                    + publicKeyState + " vs " + privateKeyState;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        }
    }
/*
    private boolean createSymmetricKey(final String keyRef, final String algorithm, final int keyLength, final int usageMask)
            throws KmcKeyClientException {
        if (locateSymmetricKey(keyRef) != null) {
            logger.error("Symmetric key \"" + keyRef + "\" exists, will not create it again.");
            return false;
        }
        KMIPContainer request = createSymmetricKeyRequest(keyRef, algorithm, keyLength, usageMask);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in creating key from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()){
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("Operation Create symmetric key \"" + keyRef + "\" done." );
                //logger.info(response.toString());
                for (Attribute a : b.getAttributes()) {
                    if (a instanceof UniqueIdentifier) {
                        KMIPAttributeValue[] values = a.getValues();
                        if (values.length > 0) {
                            logger.debug(a.getAttributeName() + ": " + values[0].getValueString());
                            logger.info("unique identifier = " + values[0].getValueString());
                        }
                    }
                }
                return true;
            } else {
                logger.error("Operation Create symmetric key \"" + keyRef + "\" failed." );
                logger.error(response.toString());
                return false;
            }
        }
        logger.warn("Operation Create symmetric key \"" + keyRef + "\" has empty response.");
        return false;
    }
*/
    private Name createNameAttribute(final String nameValue) {
        Name nameAttribute = new Name();
        nameAttribute.setValue(nameValue, "namevalue");
        nameAttribute.setValue("UninterpretedTextString", "nametype");
        return nameAttribute;
    }

    private KMIPContainer createSymmetricKeyRequest(final String creator, final String keyRef,
            final String algorithm, final int keyLength, final int usageMask)
                    throws KmcKeyClientException {
        logger.info("createSymmetricKeyRequest() creator = " + creator
                + ", algorithm = " + algorithm + ", usageMask = " + usageMask);

        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation and Attribute
        batch.setOperation(EnumOperation.Create);
        batch.addAttribute(new ObjectType(EnumObjectType.SymmetricKey));

        ContactInformation creatorAttribute = new ContactInformation();
        creatorAttribute.setValue(creator, "ContactInformation");
        logger.debug("creator attribute = " + creatorAttribute);

        EnumCryptographicAlgorithm algEnum = null;
        try {
            algEnum = new EnumCryptographicAlgorithm(algorithm);
        } catch (Exception e) {
            String msg = "Invalid algorithm in creating key: " + algorithm;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_ALGORITHM_ERROR, msg, null);
        }

        Name nameAttribute = new Name();
        nameAttribute.setValue(keyRef, "namevalue");
        nameAttribute.setValue("UninterpretedTextString", "nametype");

        // Set Template Attribute
        ArrayList<Attribute> templateAttributes = new ArrayList<Attribute>();
        templateAttributes.add(creatorAttribute);
        templateAttributes.add(nameAttribute);
        templateAttributes.add(new CryptographicAlgorithm(algEnum));
        templateAttributes.add(new CryptographicLength(keyLength));
        templateAttributes.add(new CryptographicUsageMask(usageMask));
        TemplateAttributeStructure tas = new TemplateAttribute();
        tas.setAttributes(templateAttributes);
        batch.addTemplateAttributeStructure(tas);

        return container;
    }

    private KMIPContainer createKeyPairRequest(final String creator, final String keyRef,
            final String algorithm, final int keyLength) {
        logger.info("createKeyPairRequest() creator = " + creator
                + ", algorithm = " + algorithm + ", keyLength = " + keyLength);

        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation and Attribute
        batch.setOperation(EnumOperation.CreateKeyPair);
        //batch.addAttribute(new ObjectType(EnumObjectType.Default));

        ContactInformation creatorAttribute = new ContactInformation();
        creatorAttribute.setValue(creator, "ContactInformation");

        EnumCryptographicAlgorithm algEnum = new EnumCryptographicAlgorithm(algorithm);

        // Set Common Template Attribute
        ArrayList<Attribute> commonTemplateAttributes = new ArrayList<Attribute>();
        commonTemplateAttributes.add(new CryptographicAlgorithm(algEnum));
        commonTemplateAttributes.add(new CryptographicLength(keyLength));
        TemplateAttributeStructure commonTAS = new CommonTemplateAttribute();
        commonTAS.setAttributes(commonTemplateAttributes);
        batch.addTemplateAttributeStructure(commonTAS);

        // Set Public Key Template Attribute
        Name publicNameAttribute = new Name();
        publicNameAttribute.setValue(keyRef, "namevalue");
        publicNameAttribute.setValue("UninterpretedTextString", "nametype");
        ArrayList<Attribute> publicTemplateAttributes = new ArrayList<Attribute>();
        publicTemplateAttributes.add(creatorAttribute);
        publicTemplateAttributes.add(publicNameAttribute);
        publicTemplateAttributes.add(new CryptographicUsageMask(KmcKey.USAGE_MASK_PUBLIC_KEY));
        TemplateAttributeStructure publicTAS = new PublicKeyTemplateAttribute();
        publicTAS.setAttributes(publicTemplateAttributes);
        batch.addTemplateAttributeStructure(publicTAS);

        // Set Private Key Template Attribute
        Name privateNameAttribute = new Name();
        privateNameAttribute.setValue(keyRef + PRIVATE_KEY_SUFFIX, "namevalue");
        privateNameAttribute.setValue("UninterpretedTextString", "nametype");
        ArrayList<Attribute> privateTemplateAttributes = new ArrayList<Attribute>();
        privateTemplateAttributes.add(creatorAttribute);
        privateTemplateAttributes.add(privateNameAttribute);
        privateTemplateAttributes.add(new CryptographicUsageMask(KmcKey.USAGE_MASK_PRIVATE_KEY));
        TemplateAttributeStructure privateTAS = new PrivateKeyTemplateAttribute();
        privateTAS.setAttributes(privateTemplateAttributes);
        batch.addTemplateAttributeStructure(privateTAS);
/*
        // Set Public Key Template Attribute
        Name publicNameAttribute = new Name();
        publicNameAttribute.setValue(keyRef, "namevalue");
        publicNameAttribute.setValue("UninterpretedTextString", "nametype");
        ArrayList<Attribute> publicTemplateAttributes = new ArrayList<Attribute>();
        publicTemplateAttributes.add(creatorAttribute);
        publicTemplateAttributes.add(publicNameAttribute);
        publicTemplateAttributes.add(new CryptographicAlgorithm(algEnum));
        publicTemplateAttributes.add(new CryptographicLength(keyLength));
        publicTemplateAttributes.add(new CryptographicUsageMask(KmcKey.USAGE_MASK_ENCRYPT));
        TemplateAttributeStructure publicTAS = new PublicKeyTemplateAttribute();
        publicTAS.setAttributes(publicTemplateAttributes);
        batch.addTemplateAttributeStructure(publicTAS);

        // Set Private Key Template Attribute
        Name privateNameAttribute = new Name();
        privateNameAttribute.setValue(keyRef + PRIVATE_KEY_SUFFIX, "namevalue");
        privateNameAttribute.setValue("UninterpretedTextString", "nametype");
        ArrayList<Attribute> privateTemplateAttributes = new ArrayList<Attribute>();
        privateTemplateAttributes.add(creatorAttribute);
        privateTemplateAttributes.add(privateNameAttribute);
        privateTemplateAttributes.add(new CryptographicAlgorithm(algEnum));
        privateTemplateAttributes.add(new CryptographicLength(keyLength));
        privateTemplateAttributes.add(new CryptographicUsageMask(KmcKey.USAGE_MASK_DECRYPT));
        TemplateAttributeStructure privateTAS = new PrivateKeyTemplateAttribute();
        privateTAS.setAttributes(privateTemplateAttributes);
        batch.addTemplateAttributeStructure(privateTAS);
*/
        return container;
    }

    private String locateCryptographicKey(final String keyRef)
            throws KmcKeyClientException {
        KMIPContainer request = createLocateKeyRequest(keyRef);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in locating key from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        if (response == null) {
            logger.error("Null response in locating key: " + keyRef);
            return null;
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                for (Attribute a : b.getAttributes()) {
                    if (a instanceof UniqueIdentifier) {
                        KMIPAttributeValue[] values = a.getValues();
                        if (values.length > 0) {
                            logger.debug(a.getAttributeName() + ": " + values[0].getValueString());
                            return values[0].getValueString();
                        }
                    }
                }
            } else {
                logger.error("Operation failed in locating key: " + keyRef);
                return null;
            }
        }
        logger.info("Empty response in locating key: " + keyRef);
        return null;
    }

    private KMIPContainer createLocateKeyRequest(final String keyRef) {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation and Attribute
        batch.setOperation(EnumOperation.Locate);

        if (keyRef == null) {
            return container;
        } else {
            Name nameAttribute = createNameAttribute(keyRef);
            batch.addAttribute(nameAttribute);
        }

        return container;
    }


    /**
     * Sets the key to the specified state.
     * @param keyRef KeyRef is only used in error messages.  It doesn't affect the operation.
     * @param operation The operation to be performed on the key.
     * @param revokeReason The reason to revoke the key.
     * @throws KmcKeyClientException if the operation failed.
     */
    public final KmcKey setKeyState(final String keyRef, final String state, final String revokeReason)
            throws KmcKeyClientException {
        try {
            String keyId = locateCryptoKey(keyRef);
            if (keyId == null) {
                String error = "Key " + keyRef + " does not exists.";
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.INVALID_INPUT_VALUE, error, null);
            }
            // get the key before operation so that it can be returned in case of Destroy
            KmcKey key = getKeyById(keyId);

            String operation = null;
            if ("PreActive".equals(state)) {
                String error = "Cannot change key state to PreActive.";
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.INVALID_INPUT_VALUE, error, null);
            } else if ("Active".equals(state)) {
                operation = "Activate";
            } else if ("Deactivated".equals(state)) {
                operation = "Deactivate";
            } else if ("Compromised".equals(state)) {
                operation = "Revoke";
            } else if ("Destroyed".equals(state)) {
                operation = "Destroy";
            } else if ("DestroyedCompromised".equals(state)) {
                operation = "Destroy";
            }

            if ("PublicKey".equals(key.getKeyType())) {
                operateKeyPair(keyRef, keyId, operation, revokeReason);
            } else {
                operateKey(keyRef, keyId, operation, revokeReason);
            }
            if ("Destroy".equals(state)) {
                // can't get the destroyed key
                key.setState("Destroyed");
                key.setLastChangeDate(new Date().toString());
            } else {
                key = getKeyById(keyId);
            }
            return key;
        } catch (KmcKeyClientException e) {
            String error = "Failed to set key state: " + e;
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.INVALID_INPUT_VALUE, error, null);
        }
    }

    /**
     * Performs operation on the key which changes its state as a result.
     * @param keyRef KeyRef is only used in error messages.  It doesn't affect the operation.
     * @param keyId The keyId is used for identify the key.
     * @param operation The operation to be performed on the key.
     * @param revokeReason The reason to revoke the key.
     * @throws KmcKeyClientException if the operation failed.
     */
    public final void operateKey(final String keyRef, final String keyId, final String operation,
            final String revokeReason) throws KmcKeyClientException {
        int opsCode;
        int revokeCode;
        try {
            if ("Deactivate".equals(operation)) {
                opsCode = (new EnumOperation("Revoke")).getValue();
                revokeCode = EnumRevocationReasonCode.PrivilegeWithdrawn;
            } else if ("Revoke".equals(operation)) {
                opsCode = (new EnumOperation(operation)).getValue();
                revokeCode = EnumRevocationReasonCode.KeyCompromise;
            } else {
                opsCode = (new EnumOperation(operation)).getValue();
                revokeCode = EnumRevocationReasonCode.Unspecified;
            }
        } catch (Exception ex) {
            String error = "Invalid key operation: " + operation;
            logger.error(error);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, error, ex);
        }

        RevocationReason reason = null;
        if (revokeReason != null) {
            // the revokeReason string is not saved in database for Deactivate,
            // maybe due to error in revoke() in CryptographicObject.java in KMIPWebAppServer
            reason = new RevocationReason();
            KMIPAttributeValue[] values = reason.getValues();
            values[0].setValue(new EnumRevocationReasonCode(revokeCode));
            values[1].setValue(new KMIPTextString(revokeReason));
        }

        KMIPContainer request = createKeyOperationRequest(keyId, opsCode, reason);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in requesting key operation (" + operation + "): " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("Successfully key operation (" + operation + ") on keyRef: " + keyRef);
                return;
            } else {
                String msg = "Failed key operation (" + operation + ") on keyRef: " + keyRef
                        + ", keyId: " + keyId;
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        String msg = "Empty response for key operation (" + operation + ") on keyRef: " + keyRef;
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    /**
     * Performs operation on the key pair which changes its state as a result.
     * @param keyRef KeyRef is used to locate the private key of the key pair.
     * @param publicKeyId The keyId of the public key of the key pair.
     * @param operation The operation to be performed on the key pair.
     * @param revokeReason The reason to revoke the key.
     * @throws KmcKeyClientException if the operation failed.
     */
    public final void operateKeyPair(final String keyRef, final String publicKeyId,
            final String operation, final String revokeReason) throws KmcKeyClientException {
        // public key
        operateKey(keyRef, publicKeyId, operation, revokeReason);
        String privateKeyId = locatePrivateKey(keyRef);
        if (privateKeyId == null) {
            String msg = "operateKeyPair() Can't locate private key of key pair: " + keyRef;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
        } else {
            operateKey(keyRef, privateKeyId, operation, revokeReason);
        }
    }

/*
    public void activateKey(final String keyRef, final String keyId, final int keyType) throws KmcKeyClientException {
        KMIPContainer request = createKeyOperationRequest(keyId, EnumOperation.Activate, null);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in activating key in KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()){
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("Activate key " + keyRef + " completed successfully." );
                return;
            } else {
                String msg = "Failed to Activate key: " + keyRef;
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        String msg = "Activate key " + keyRef + " got empty response.";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    public void deactivateKey(final String keyRef, final String keyId, final int keyType, final String reason) throws KmcKeyClientException {
        // the reason string is not saved in database, maybe due to error in revoke() in CryptographicObject.java in KMIPWebAppServer
        EnumRevocationReasonCode code = new EnumRevocationReasonCode(EnumRevocationReasonCode.PrivilegeWithdrawn);
        RevocationReason revokeReason = new RevocationReason();
        KMIPAttributeValue[] values = revokeReason.getValues();
        values[0].setValue(code);
        values[1].setValue(new KMIPTextString(reason));

        KMIPContainer request = createKeyOperationRequest(keyId, EnumOperation.Revoke, revokeReason);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in Revoke key in KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()){
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("Revoke key " + keyRef + " completed successfully." );
                return;
            } else {
                String msg = "Failed to Revoke key: " + keyRef;
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        String msg = "Revoke key " + keyRef + " got empty response.";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    public void revokeKey(final String keyRef, final String keyId, final int keyType, final String reason) throws KmcKeyClientException {
        EnumRevocationReasonCode code = new EnumRevocationReasonCode(EnumRevocationReasonCode.KeyCompromise);
        RevocationReason revokeReason = new RevocationReason();
        KMIPAttributeValue[] values = revokeReason.getValues();
        values[0].setValue(code);
        values[1].setValue(new KMIPTextString(reason));

        KMIPContainer request = createKeyOperationRequest(keyId, EnumOperation.Revoke, revokeReason);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in Revoke key in KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()){
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("Revoke key " + keyRef + " completed successfully." );
                return;
            } else {
                String msg = "Failed to Revoke key: " + keyRef;
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        String msg = "Revoke key " + keyRef + " got empty response.";
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    public boolean destroyCryptographicKey(final String keyId, final int keyType) throws KmcKeyClientException {
        KMIPContainer request = createDestroyKeyRequest(keyId, keyType);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in destroying key from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()){
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("Operation Destroy key completed successfully." );
                return true;
            } else {
                logger.error("Operation Destroy key failed." );
                return false;
            }
        }
        logger.error("Failed to destroy key.");
        return false;
    }
*/
    /**
     * Destroy the key identified by the keyId.
     * @param keyId The KMIP unique identifier of the key to be destroyed.
     * @throws KmcKeyClientException if error in destroying the key.
     */
    public final void destroyCryptographicKey(final String keyId) throws KmcKeyClientException {
        KMIPContainer request = createKeyOperationRequest(keyId, EnumOperation.Destroy, null);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in destroying key from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                logger.info("Operation Destroy key completed successfully.");
                return;
            } else {
                String msg = "Failed to destroy key of keyId: " + keyId;
                logger.error(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        String msg = "KMS returns null in destroy key of keyId: " + keyId;
        logger.error(msg);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }

    private KMIPContainer createKeyOperationRequest(final String keyId, final int keyOperation,
            final RevocationReason revokeReason) throws KmcKeyClientException {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation
        batch.setOperation(keyOperation);

        // Set Attributes
        UniqueIdentifier a = new UniqueIdentifier();
        a.setValue(keyId, null);
        batch.addAttribute(a);
        if (keyOperation == EnumOperation.Revoke) {
            if (revokeReason == null) {
                String msg = "Revoke key requires a reason.";
                logger.info(msg);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
            batch.addAttribute(revokeReason);
        }

        return container;
    }
/*
    private KMIPContainer createDestroyKeyRequest(final String uniqueIdentifier, final int keyType) {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation and Attribute
        batch.setOperation(EnumOperation.Destroy);
        //batch.addAttribute(new ObjectType(keyType));

        // Set Key Unique Identify
        UniqueIdentifier a = new UniqueIdentifier();
        a.setValue(uniqueIdentifier, null);
        batch.addAttribute(a);

        return container;
    }
*/
    private CryptographicObject getCryptographicObject(final String keyId)
            throws KmcKeyClientException {
        KMIPContainer request = createGetCryptographicObjectRequest(keyId);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
            logger.debug("getCryptographicObject() response = {}", response);
        } catch (Exception e) {
            String msg = "Exception in Get cryptographic object from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                ManagedObject object = b.getManagedObject();
                if (object instanceof SymmetricKey) {
                    return (CryptographicObject) object;
                } else if (object instanceof PublicKey) {
                    return (CryptographicObject) object;
                } else if (object instanceof PrivateKey) {
                    return (CryptographicObject) object;
                }
            } else {
                String msg = "Failed to get cryptographic object for keyId: " + keyId;
                logger.error(msg);
                logger.error("The KMS response = {}", response);
                throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
            }
        }
        String msg = "KMS returns null to get cryptographic object for KeyId: " + keyId;
        logger.error(msg);
        logger.error("The KMS response = {}", response);
        throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, null);
    }
/*
    private CryptographicObject getCryptographicObject(final String keyRef, final int keyType)
            throws KmcKeyClientException {
        String keyUniqueName = locateCryptographicKey(keyRef, keyType);
        if (keyUniqueName == null) {
            logger.error("Cryptographic key \"" + keyRef + "\" of type " + keyType + " does not exist.");
            return null;
        }
        KMIPContainer request = createGetKeyRequest(keyUniqueName, keyType);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in locating key from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        for (KMIPBatch b : response.getBatches()){
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                ManagedObject object = b.getManagedObject();
                if (object instanceof SymmetricKey) {
                    return (CryptographicObject) object;
                } else if (object instanceof PublicKey) {
                    return (CryptographicObject) object;
                } else if (object instanceof PrivateKey) {
                    return (CryptographicObject) object;
                }
            } else {
                logger.error("Operation Get cryptographic key \"" + keyRef + "\" failed." );
                return null;
            }
        }
        logger.error("Failed to get cryptographic key \"" + keyRef + "\".");
        return null;
    }
*/
    private KMIPContainer createGetCryptographicObjectRequest(final String keyId) {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation and Attribute
        batch.setOperation(EnumOperation.Get);

        // Set Key Unique Identifier
        UniqueIdentifier a = new UniqueIdentifier();
        a.setValue(keyId, null);
        batch.addAttribute(a);

        return container;
    }
/*
    private KMIPContainer createGetKeyRequest(final String uniqueIdentifier, final int keyType) {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation and Attribute
        batch.setOperation(EnumOperation.Get);
        if (keyType != -1) {
            batch.addAttribute(new ObjectType(keyType));
        }
        // Set Key Unique Identify
        UniqueIdentifier a = new UniqueIdentifier();
        a.setValue(uniqueIdentifier, null);
        batch.addAttribute(a);

        return container;
    }
*/
    private KMIPContainer createGetStateRequest(final String uniqueIdentifier) {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation and Attribute
        batch.setOperation(EnumOperation.GetAttributes);
        State stateAttr = new State();
        batch.addAttribute(stateAttr);
        // Set Key Unique Identify
        UniqueIdentifier a = new UniqueIdentifier();
        a.setValue(uniqueIdentifier, null);
        batch.addAttribute(a);

        return container;
    }

    private KMIPContainer createGetAllAttributesRequest(final String keyId) {
        // Create Container with one Batch
        KMIPContainer container = new KMIPContainer();
        KMIPBatch batch = new KMIPBatch();
        container.addBatch(batch);
        container.calculateBatchCount();

        // Set Operation
        batch.setOperation(EnumOperation.GetAttributes);

        // Set keyId
        UniqueIdentifier a = new UniqueIdentifier();
        a.setValue(keyId, null);
        batch.addAttribute(a);

        // list of basic attributes
        batch.addAttribute(new ContactInformation());
        batch.addAttribute(new Name());
        batch.addAttribute(new ObjectType());
        batch.addAttribute(new State());
        batch.addAttribute(new ObjectType(EnumObjectType.Default));
        batch.addAttribute(new CryptographicUsageMask());
        batch.addAttribute(new InitialDate());
        batch.addAttribute(new LastChangeDate());

        return container;
    }

/*
    public KmcKey getKmcKey(final String keyId) throws KmcKeyClientException {
        KMIPContainer request = createGetKeyRequest(keyId, -1);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in locating key from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        CryptographicObject cryptoObject = null;
        for (KMIPBatch b : response.getBatches()) {
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                ManagedObject object = b.getManagedObject();
                if (object instanceof SymmetricKey) {
                    cryptoObject = (CryptographicObject) object;
                } else if (object instanceof PublicKey) {
                    cryptoObject = (CryptographicObject) object;
                } else if (object instanceof PrivateKey) {
                    cryptoObject = (CryptographicObject) object;
                }
            } else {
                logger.error("Operation Get cryptographic key \"" + keyId + "\" failed." );
                return null;
            }
        }
        if (cryptoObject == null) {
            logger.error("Failed to get cryptographic object for \"" + keyId + "\".");
            return null;
        } else {
            //KmcKey key = new KmcKey(this, keyRef, keyId, keyType);
            KmcKey key = new KmcKey(this, keyId, keyId, -1);
            key.setCryptographicObject(cryptoObject);
            return key;
        }
    }

    public KmcKey getKmcKey(final String keyRef, final String keyId, final int keyType) throws KmcKeyClientException {
        KMIPContainer request = createGetKeyRequest(keyId, keyType);
        KMIPContainer response;
        try {
            response = kmipServer.processRequest(request);
        } catch (Exception e) {
            String msg = "Exception in locating key from KMS: " + e;
            logger.error(msg);
            throw new KmcKeyClientException(KmcKeyOpsErrorCode.KEY_OPERATION_ERROR, msg, e);
        }
        CryptographicObject cryptoObject = null;
        for (KMIPBatch b : response.getBatches()){
            if (b.getResultStatus().getValue() == EnumResultStatus.Success) {
                ManagedObject object = b.getManagedObject();
                if (object instanceof SymmetricKey) {
                    cryptoObject = (CryptographicObject) object;
                } else if (object instanceof PublicKey) {
                    cryptoObject = (CryptographicObject) object;
                } else if (object instanceof PrivateKey) {
                    cryptoObject = (CryptographicObject) object;
                }
            } else {
                logger.error("Operation Get cryptographic key \"" + keyRef + "\" failed." );
                return null;
            }
        }
        if (cryptoObject == null) {
            logger.error("Failed to get cryptographic object for \"" + keyRef + "\".");
            return null;
        } else {
            KmcKey key = new KmcKey(this, keyRef, keyId, keyType);
            key.setCryptographicObject(cryptoObject);
            return key;
        }
    }
*/
}
