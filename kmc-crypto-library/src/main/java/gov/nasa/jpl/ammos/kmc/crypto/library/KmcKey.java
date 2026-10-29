package gov.nasa.jpl.ammos.kmc.crypto.library;

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;

import ch.ntb.inf.kmip.attributes.ContactInformation;
import ch.ntb.inf.kmip.attributes.CryptographicAlgorithm;
import ch.ntb.inf.kmip.attributes.CryptographicLength;
import ch.ntb.inf.kmip.attributes.CryptographicUsageMask;
import ch.ntb.inf.kmip.attributes.InitialDate;
import ch.ntb.inf.kmip.attributes.KMIPAttributeValue;
import ch.ntb.inf.kmip.attributes.LastChangeDate;
import ch.ntb.inf.kmip.attributes.Name;
import ch.ntb.inf.kmip.attributes.ObjectType;
import ch.ntb.inf.kmip.attributes.State;
import ch.ntb.inf.kmip.kmipenum.EnumCryptographicAlgorithm;
import ch.ntb.inf.kmip.kmipenum.EnumObjectType;
import ch.ntb.inf.kmip.kmipenum.EnumState;
import ch.ntb.inf.kmip.objects.base.Attribute;
import ch.ntb.inf.kmip.objects.base.KeyBlock;
import ch.ntb.inf.kmip.objects.managed.CryptographicObject;
import ch.ntb.inf.kmip.objects.managed.SymmetricKey;

/**
 * The KmcKey embodies all information of a key, including its key material, key algorithm,
 * key length, key reference, KMIP unique identifier, and other attributes.
 *
 */
public class KmcKey {
    // KMIP Usage Mask (see page 126 of kmip-spec-v1.1.pdf)
    private static final int USAGE_MASK_SIGN = 0x01;
    private static final int USAGE_MASK_VERIFY = 0x02;
    private static final int USAGE_MASK_ENCRYPT = 0x04;
    private static final int USAGE_MASK_DECRYPT = 0x08;
    private static final int USAGE_MASK_MAC_GENERATE = 0x0080;
    private static final int USAGE_MASK_MAC_VERIFY = 0x0100;

    // KMC Usage Mask
    public static final int USAGE_MASK_ENCRYPTION = USAGE_MASK_ENCRYPT | USAGE_MASK_DECRYPT;
    public static final int USAGE_MASK_ICV = USAGE_MASK_MAC_GENERATE | USAGE_MASK_MAC_VERIFY;
    public static final int USAGE_MASK_PUBLIC_KEY = USAGE_MASK_VERIFY | USAGE_MASK_ENCRYPT;
    public static final int USAGE_MASK_PRIVATE_KEY = USAGE_MASK_SIGN | USAGE_MASK_DECRYPT;

    // KMC Usage String
    public static final String USAGE_ENCRYPTION = "Encryption";
    public static final String USAGE_ICV = "Integrity Check";
    public static final String USAGE_PUBLIC_KEY = "Verify | Encrypt";
    public static final String USAGE_PRIVATE_KEY = "Sign | Decrypt";
    public static final String USAGE_UNKNOWN = "Unknown";

    private static final Map<Integer, String> USAGE_MASK_MAP;
    static {
        USAGE_MASK_MAP = new HashMap<Integer, String>();
        USAGE_MASK_MAP.put(USAGE_MASK_SIGN, "Sign");
        USAGE_MASK_MAP.put(USAGE_MASK_VERIFY, "Verify");
        USAGE_MASK_MAP.put(USAGE_MASK_ENCRYPT, "Encrypt");
        USAGE_MASK_MAP.put(USAGE_MASK_DECRYPT, "Decrypt");
        USAGE_MASK_MAP.put(USAGE_MASK_MAC_GENERATE, "CreateIcv");
        USAGE_MASK_MAP.put(USAGE_MASK_MAC_VERIFY, "VerifyIcv");
        USAGE_MASK_MAP.put(USAGE_MASK_ENCRYPTION, USAGE_ENCRYPTION);
        USAGE_MASK_MAP.put(USAGE_MASK_ICV, USAGE_ICV);
        USAGE_MASK_MAP.put(USAGE_MASK_PUBLIC_KEY, USAGE_PUBLIC_KEY);
        USAGE_MASK_MAP.put(USAGE_MASK_PRIVATE_KEY, USAGE_PRIVATE_KEY);
    }

    private final String keyId;   // KMIP Unique Identifier
    private String creator;
    private String keyRef;
    private int keyType;
    private String algorithm;
    private int keyLength;
    private String state;
    private int usageMask;
    private String initialDate;
    private String lastChangeDate;
    private CryptographicObject cryptoObject;
    private Key javaKey;
    private long timeCreated;

    private static final Logger logger = LoggerFactory.getLogger(KmcKey.class);

    /**
     * Constructor of KmcKey that has its unique identifier retrieved from KMS.
     * Its cryptographic object and attributes will be filled in later.
     * @param keyId The unique identifier of the key in KMS.
     */
    public KmcKey(final String keyId) {
        this(null, keyId, -1);
    }

    /**
     * Constructor of KmcKey with its key reference, unique identifier and key type.
     * Its cryptographic object and attributes will be filled in later.
     * @param keyRef The key reference of the key.
     * @param keyId The unique identifier of the key in KMS.
     * @param keyType The type of key.
     */
    public KmcKey(final String keyRef, final String keyId, final int keyType) {
        this.keyRef = keyRef;
        this.keyId = keyId;
        this.keyType = keyType;
        this.timeCreated = System.currentTimeMillis();
    }

    /**
     * Constructor of KmcKey from a key retrieved from Java keystore.
     * @param keyRef The key reference (or alias in keystore) of the key.
     * @param javaKey The cryptographic object of the key retrieved from Java keystore.
     */
    public KmcKey(final String keyRef, final Key javaKey) {
        String alg = null;
        int len = 0;
        int mask = 0;
        this.keyId = null;
        this.keyRef = keyRef;
        if (javaKey instanceof SecretKey key) {
            this.keyType = EnumObjectType.SymmetricKey;
            alg = key.getAlgorithm();
            len = key.getEncoded().length * 8;
            mask = USAGE_MASK_ENCRYPTION;
            if ("AES".equals(alg)) {
                // can be used for CMAC
                mask = mask | USAGE_MASK_ICV;
            }
        } else if (javaKey instanceof RSAPublicKey key) {
            this.keyType = EnumObjectType.PublicKey;
            alg = key.getAlgorithm();
            len = key.getModulus().bitLength();
            mask = USAGE_MASK_PUBLIC_KEY;
        } else if (javaKey instanceof RSAPrivateKey key) {
            this.keyType = EnumObjectType.PrivateKey;
            alg = key.getAlgorithm();
            len = key.getModulus().bitLength();
            mask = USAGE_MASK_PRIVATE_KEY;
        } else {
            this.keyType = EnumObjectType.Default;
        }
        logger.info("KmcKey(keystore) algorithm = {}, length = {}", alg, len);
        this.javaKey = javaKey;
        this.algorithm = alg;
        this.keyLength = len;
        this.usageMask = mask;
        setState("Active");
    }

    /**
     * Set the attributes retrieved from KMS for the key.
     * @param attributes The list of KMIP attributes.
     */
    public final void setAttributes(final List<Attribute> attributes) {
        for (Attribute a : attributes) {
            KMIPAttributeValue[] values = a.getValues();
            if (a instanceof Name) {
                keyRef = values[0].getValueString();
                logger.debug("setAttributes(); keyRef = " + keyRef);
            } else if (a instanceof ObjectType) {
                EnumObjectType objectType = (EnumObjectType) values[0].getValueAsKMIPType();
                keyType = objectType.getValue();
                logger.debug("setAttributes(); keyType = " + keyType + "(" + objectType.getKey() + ")");
            } else if (a instanceof State) {
                state = ((EnumState) values[0].getValueAsKMIPType()).getKey();
                logger.debug("setAttributes(); state = " + state);
            } else if (a instanceof CryptographicUsageMask) {
                usageMask = Integer.parseInt(values[0].getValueString());
                logger.debug("setAttributes(); usageMask = " + usageMask);
            } else if (a instanceof InitialDate) {
                initialDate = values[0].getValueString();
                logger.debug("setAttributes(); initialDate = " + initialDate);
            } else if (a instanceof LastChangeDate) {
                lastChangeDate = values[0].getValueString();
                logger.debug("setAttributes(); lastChangeDate = " + lastChangeDate);
            } else if (a instanceof ContactInformation) {
                creator = values[0].getValueString();
                logger.debug("setAttributes(); creator = " + creator);
            }
        }
    }

    /**
     * Returns the time when the KmcKey object was created.
     * @return KmcKey object creation time.
     */
    public final long getKmcKeyTime() {
        return timeCreated;
    }

    /**
     * Returns the creator of the key.
     * @return the creator of the key.
     */
    public final String getCreator() {
        return creator;
    }

    /**
     * Returns the key reference of the key.
     * @return the key reference of the key.
     */
    public final String getKeyRef() {
        return keyRef;
    }

    /**
     * Returns the unique identifier of the key.
     * @return the unique identifier of the key.
     */
    public final String getKeyId() {
        return keyId;
    }

    /**
     * Returns the key type of the key, e.g. symmetric, public, or private.
     * @return the key type of the key.
     */
    public final String getKeyType() {
        return new EnumObjectType(keyType).getKey();
    }

    /**
     * Returns the KMIP usage mask of the key.
     * @return the KMIP usage mask of the key.
     */
    public final int getUsageMask() {
        return usageMask;
    }

    /**
     * Returns the intended usage of the key.
     * @return the intended usage of the key.
     */
    public final String getKeyUsage() {
        String usage = USAGE_MASK_MAP.get(usageMask);
        if (usage == null) {
            logger.error("Unknown usage for mask = " + usageMask);
            usage = USAGE_UNKNOWN;
        }
        return usage;
    }

    /**
     * Returns the date when the key was created.
     * @return the date when the key was created.
     */
    public final String getInitialDate() {
        return initialDate;
    }

    /**
     * Returns the date when the key was last changed.
     * @return the date when the key was last changed.
     */
    public final String getLastChangeDate() {
        return lastChangeDate;
    }

    /**
     * Sets the date when the key was last changed.
     * @param date The last change date.
     */
    public final void setLastChangeDate(final String date) {
        this.lastChangeDate = date;
    }

    /**
     * Sets the cryptographic object of the key.
     * The cryptographic object contains the key material, key algorithm and key length.
     * @param cryptoObject the cryptographic object of the key.
     */
    public final void setCryptographicObject(final CryptographicObject cryptoObject) {
        this.cryptoObject = cryptoObject;
    }

    /**
     * Returns the cryptographic object of the key in Java data structure.
     * @return the cryptographic object of the key in Java data structure.
     * @exception KmcCryptoException if error in creating the Java key.
     */
    public final Key getJavaKey() throws KmcCryptoException {
        if (javaKey != null) {
            return javaKey;
        }
        switch (keyType) {
        case EnumObjectType.SymmetricKey:
            javaKey = getSecretKey();
            break;
        case EnumObjectType.PublicKey:
            javaKey = getPublicKey();
            break;
        case EnumObjectType.PrivateKey:
            javaKey = getPrivateKey();
            break;
        default:
            String msg = "Unknown key type: " + keyType;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }
        return javaKey;
    }

    /**
     * Returns the key algorithm of the key.
     * @return the key algorithm of the key, or null if error in getting key algorithm.
     */
    public final String getKeyAlgorithm() {
        if (this.algorithm != null) {
            // keystore key
            return this.algorithm;
        }
        try {
            CryptographicAlgorithm alg = getKeyBlock().getCryptographicAlgorithm();
            String value = alg.getValues()[0].getValueString();
            int algInt = Integer.parseInt(value);
            EnumCryptographicAlgorithm algEnum = new EnumCryptographicAlgorithm(algInt);
            return algEnum.getKey();
        } catch (Exception e) {
            logger.error("getKeyAlgorithm() Exception getting key block: " + e);
            return null;
        }
    }

    /**
     * Returns the key algorithm of the key in Java naming convention.
     * @return the key algorithm of the key in Java naming convention, or null if error in getting key algorithm.
     */
    public final String getJavaKeyAlgorithm() {
        String keyAlgorithm = getKeyAlgorithm();
        return kmip2javaAlgorithm(keyAlgorithm);
    }

    /**
     * Returns the key length of the key.
     * @return the key length of the key, or null if error in getting key length.
     */
    public final int getKeyLength() {
        if (getKeyBlock() == null) {
            // keystore key
            return this.keyLength;
        }
        try {
            CryptographicLength len = getKeyBlock().getCryptographicLength();
            String value = len.getValues()[0].getValueString();
            return Integer.parseInt(value);
        } catch (NullPointerException e) {
            logger.error("getKeyLength() Exception getting key block: " + e);
            return -1;
        }
    }

    /**
     * Returns the key material of the key.
     * @return the key material of the key, or null if error in getting key material.
     */
    public final byte[] getKeyMaterial() {
        try {
            return getKeyBlock().getKeyValue().getKeyMaterial().getKeyMaterialByteString().getValue();
        } catch (NullPointerException e) {
            logger.error("getKeyMaterial() Exception getting key block: " + e);
            return null;
        }
    }

    /**
     * Returns the KMIP state of the key.
     * @return the KMIP state of the key.
     */
    public final String getState() {
        //return ((KmipKeyMgtServiceClient) client).getState(keyRef, keyId);
        return state;
    }

    /**
     * Sets the KMIP state of the key.
     * @param state The state of the key.
     */
    public final void setState(final String state) {
        this.state = state;
    }

    private SecretKey getSecretKey() {
        byte[] keyMaterial = getKeyMaterial();
        String keyAlgorithm = getKeyAlgorithm();
        if (keyMaterial == null || keyAlgorithm == null) {
            return null;
        } else {
            return new SecretKeySpec(keyMaterial, keyAlgorithm);
        }
    }

    private PublicKey getPublicKey() throws KmcCryptoException {
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance(getKeyAlgorithm());
        } catch (NullPointerException|NoSuchAlgorithmException e) {
            String msg = "Exception in creating public key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
        try {
            EncodedKeySpec ks = new X509EncodedKeySpec(getKeyMaterial());
            return keyFactory.generatePublic(ks);
        } catch (NullPointerException|InvalidKeySpecException e) {
            String msg = "Exception in creating public key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
    }

    private PrivateKey getPrivateKey() throws KmcCryptoException {
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance(getKeyAlgorithm());
        } catch (NullPointerException|NoSuchAlgorithmException e) {
            String msg = "Exception in creating private key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, null);
        }
        try {
            EncodedKeySpec ks = new PKCS8EncodedKeySpec(getKeyMaterial());
            return keyFactory.generatePrivate(ks);
        } catch (NullPointerException|InvalidKeySpecException e) {
            String msg = "Exception in creating private key: " + e;
            logger.error(msg);
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, msg, e);
        }
    }

    private KeyBlock getKeyBlock() {
        if (cryptoObject instanceof SymmetricKey key2) {
            return key2.getKeyBlock();
        } else if (cryptoObject instanceof ch.ntb.inf.kmip.objects.managed.PrivateKey key1) {
            return key1.getKeyBlock();
        } else if (cryptoObject instanceof ch.ntb.inf.kmip.objects.managed.PublicKey key) {
            return key.getKeyBlock();
        } else {
            return null;
        }
    }

    /**
     * Returns the Java key algorithm name from the KMIP name.
     * @param keyAlgorithmName The KMIP algorithm name.
     * @return The Java algorithm name.
     */
    private String kmip2javaAlgorithm(final String keyAlgorithmName) {
        // KMIP key algorithm name
        if ("HMAC_SHA1".equals(keyAlgorithmName)) {
            return "HmacSHA1";
        } else if ("HMAC_SHA256".equals(keyAlgorithmName)) {
            return "HmacSHA256";
        } else if ("HMAC_SHA384".equals(keyAlgorithmName)) {
            return "HmacSHA384";
        } else if ("HMAC_SHA512".equals(keyAlgorithmName)) {
            return "HmacSHA512";
        } else {
            return keyAlgorithmName;
        }
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("KmcKey(");
        sb.append(keyId + ", ");
        sb.append(keyRef + ", ");
        sb.append(getKeyType() + ", ");
        sb.append(getJavaKeyAlgorithm() + ", ");
        sb.append(getKeyLength() + ", ");
        sb.append(state + ", ");
        sb.append(getKeyUsage() + ", ");
        sb.append(initialDate + ", ");
        sb.append(lastChangeDate + ", ");
        sb.append(creator);
        sb.append(")");
        return sb.toString();
    }

}
