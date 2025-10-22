package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator for Security Association parameters and objects
 */
public class SecAssnValidator {

    /**
     * Private constructor, static methods only
     */
    private SecAssnValidator() {

    }

    /**
     * Service type range
     */
    public static final Set<Short> ST_RANGE    = new HashSet<>(Arrays.asList((short) 0, (short) 1));
    /**
     * State range
     */
    public static final Set<Short> STATE_RANGE = new HashSet<>(Arrays.asList((short) 0, (short) 1, (short) 2,
            (short) 3));

    /**
     * Validate a provided SA. Throws an exception on invalid.
     *
     * @param sa security association
     * @throws KmcException ex
     */
    public static void validate(ISecAssn sa) throws KmcException {
        // validate required
        if (sa.getScid() == null || sa.getTfvn() == null || sa.getMapid() == null || sa.getVcid() == null) {
            throw new KmcException(String.format("%s invalid GVCID: SCID, VCID, TFVN, and MAPID are all required", sa));
        }
        // bounds check on sa state
        if (!STATE_RANGE.contains(sa.getSaState())) {
            throw new KmcException(String.format("%s invalid SA State %d", sa, sa.getSaState()));
        }
        verifyEst(sa);
        verifyAst(sa);
        verifyArsn(sa);
        verifyAbm(sa);
        verifyIv(sa);

        checkArgsNotNull("Invalid ABM: ABM and ABM length are both required", sa.getAbm(), sa.getAbmLen());
        if (sa.getAbmLen() != sa.getAbm().length) {
            throwEx(String.format("%s invalid ABM: byte size of ABM and ABM length must agree, %d != %d, ", sa, sa.getAbm().length, sa.getAbmLen()));
        }
    }

    private static void verifyEst(ISecAssn sa) throws KmcException {
        if (!ST_RANGE.contains(sa.getEst())) {
            throw new KmcException(String.format("%s invalid EST: %d", sa, sa.getEst()));
        }
    }

    private static void verifyAst(ISecAssn sa) throws KmcException {
        if (!ST_RANGE.contains(sa.getAst())) {
            throw new KmcException(String.format("%s invalid AST: %d", sa, sa.getAst()));
        }
    }

    /**
     * Check if any of the provided objects are not null, and if so, further check that all provided objects are not
     * null.
     *
     * @param message
     * @param objs
     * @return
     */
    private static boolean checkArgsNotNull(String message, Object... objs) throws KmcException {
        if (ObjectUtils.anyNotNull(objs)) {
            if (ObjectUtils.anyNull(objs)) {
                throwEx(message);
            }
            return true;
        }
        return false;
    }

    /**
     * Check if any one object in a list of objects isn't null, and if so, further check that all objects in another
     * list of objects is not null.
     *
     * @param message    error message
     * @param anyPresent any of these objects are not null
     * @param required   all of these objects are required to be not null
     * @return args null or not
     * @throws KmcException ex
     */
    public static boolean checkArgsNotNull(String message, List<Object> anyPresent, List<Object> required) throws KmcException {
        if (ObjectUtils.anyNotNull(anyPresent.toArray())) {
            if (ObjectUtils.anyNull(required.toArray())) {
                throwEx(message);
            }
            return true;
        }
        return false;
    }

    /**
     * Throw a (runtime) parameter exception
     *
     * @param message error message
     * @throws KmcException
     */
    private static void throwEx(String message) throws KmcException {
        throw new KmcException(message);
    }

    /**
     * Verify IV
     *
     * @param iv       initialization vectore
     * @param ivLen    iv length
     * @param est      EST
     * @param ecsBytes ECS bytes
     * @return iv byte array
     * @throws KmcException ex
     */
    public static byte[] verifyIv(String iv, Short ivLen, Short est, String ecsBytes) throws KmcException {
        return verifyIv(convertHexToBytes(iv, "iv must be a hex string", true), ivLen, est,
                convertHexToBytes(ecsBytes, "ECS must be a hex string", true));
    }

    /**
     * Verify IV
     *
     * @param iv       IV
     * @param ivLen    IV length
     * @param est      encryption service type
     * @param ecsBytes ECS bytes
     * @return valid or invalid
     * @throws KmcException exception
     */
    public static byte[] verifyIv(byte[] iv, Short ivLen, Short est, byte[] ecsBytes) throws KmcException {
        ECSTYPE validEcs = ECSTYPE.fromBytes(ecsBytes);
        if (est != null && est == (short) 0) {
            //Encryption is not on, skip validation
            return iv;
        }
        if (validEcs == null) {
            //Bypass validation, ECS holds no information in this case
            return iv;
            //throwEx("Unknown ECS, check value or request an update of the KMC API Library ECSTYPE list");
        }
        if (ivLen.shortValue() != validEcs.getIvLen()) {
            throwEx("ivLen of " + ivLen.shortValue() + " is invalid for the current ECS; " + validEcs.name() + " " +
                    "accepts only ivLen " + validEcs.getIvLen());
        }
        if (iv != null && iv.length != validEcs.getIvLen()) {
            throwEx("IV " + Hex.encodeHexString(iv) + " is length: " + iv.length + " but selected ECS " + validEcs.name() + " accepts only IVs of length " + validEcs.getIvLen());
        }
        return iv;
    }

    /**
     * Verify encryption parameters
     *
     * @param ekid encryption key id
     * @param ecs  encryption cypher suite
     * @return ecs byte array
     * @throws KmcException ex
     */
    public static byte[] verifyEnc(String ekid, String ecs) throws KmcException {
        List<Object> anyPresent = Arrays.asList(ekid, ecs);
        List<Object> required   = Arrays.asList(ekid, ecs);
        if (checkArgsNotNull("When specifying encryption '--ekid' and '--ecs' are required", anyPresent, required)) {
            byte[] ecsBytes = convertHexToBytes(ecs, "ECS must be a hex string");
            return ecsBytes;
        }
        return null;
    }

    /**
     * Validate auth parameters
     *
     * @param akid authentication key id
     * @param acs  authentication cypher suite
     * @return acs byte array
     * @throws KmcException ex
     */
    public static byte[] verifyAuth(String akid, String acs) throws KmcException {
        List<Object> anyPresent = new ArrayList<>(Arrays.asList(akid, acs));
        List<Object> required   = new ArrayList<>(Arrays.asList(akid, acs));
        if (checkArgsNotNull("When specifying authentication '--akid' and '--acs' are required", anyPresent,
                required)) {
            byte[] acsBytes = convertHexToBytes(acs, "ACS must be a hex string");
            return acsBytes;
        }
        return null;
    }

    /**
     * Validate ABM
     *
     * @param abm    authentication bitmask
     * @param abmLen abm length
     * @return abm byte array
     * @throws KmcException ex
     */
    public static byte[] checkAbm(String abm, Integer abmLen) throws KmcException {
        if (checkArgsNotNull("When specifying '--abm' or '--amblen', both are required", abm, abmLen)) {
            byte[] abmBytes = convertHexToBytes(abm, "ABM must be a hex string");
            if (abmBytes.length != abmLen) {
                throwEx("ABM byte length must match specified " + "ABM length");
            }
            return abmBytes;
        }
        return null;
    }

    /**
     * Validate service type
     *
     * @param st service type
     * @return service type enum
     * @throws KmcException ex
     */
    public static ServiceType checkSt(Short st) throws KmcException {
        if (st != null) {
            if (st < 0 || st > 3) {
                throwEx(String.format("Invalid value '%s' for service type, value must be either between 0 and 3 " +
                        "inclusive, or one of 'PLAINTEXT', 'ENCRYPTION', 'AUTHENTICATION', or " +
                        "'AUTHENTICATED_ENCRYPTION'", st));
            }
            ServiceType t = ServiceType.fromShort(st);
            if (t == ServiceType.UNKNOWN) {
                throwEx(String.format("Invalid value '%s' for service type, value must be either between 0 and 3 " +
                        "inclusive, or one of 'PLAINTEXT', 'ENCRYPTION', 'AUTHENTICATION', or " +
                        "'AUTHENTICATED_ENCRYPTION'", st));
            }
            return t;
        }
        return null;
    }

    /**
     * Validate service type
     *
     * @param st service type
     * @return service type enum
     * @throws KmcException ex
     */
    public static ServiceType checkSt(String st) throws KmcException {
        if (st != null) {
            ServiceType t = ServiceType.valueOf(st);
            if (t == ServiceType.UNKNOWN) {
                throwEx(String.format("Invalid value '%s' for service type, value must be either between 0 and 3 " +
                        "inclusive, or one of 'PLAINTEXT', 'ENCRYPTION', 'AUTHENTICATION', or " +
                        "'AUTHENTICATED_ENCRYPTION'", st));
            }
            return t;
        }
        return null;
    }

    /**
     * Verify ARSNW as set in a SecAssn object
     *
     * @param arsnw security association
     * @throws KmcException ex
     */
    public static void verifyArsnw(Short arsnw) throws KmcException {
        // no op
    }

    /**
     * Verify ABM as set in a SecAssn object
     *
     * @param sa security association
     * @throws KmcException ex
     */
    public static void verifyAbm(ISecAssn sa) throws KmcException {
        if (sa.getAbm() != null && sa.getAbmLen() != null && sa.getAbm().length != sa.getAbmLen()) {
            throwEx("ABM and ABM length must agree");
        }
    }

    /**
     * Verify IV as set in a SecAssn object
     *
     * @param sa security association
     * @throws KmcException ex
     */
    public static void verifyIv(ISecAssn sa) throws KmcException {
        verifyIv(sa.getIv(), sa.getIvLen(), sa.getEst(), sa.getEcs());

    }

    /**
     * Verify ARSN as set in a SecAssn object.
     * <p>
     * The length of the ARSN byte array must match the declared length
     *
     * @param sa security association
     * @throws KmcException ex
     */
    public static void verifyArsn(ISecAssn sa) throws KmcException {
//        if (sa.getArsn().length != sa.getArsnLen()) {
//            throw new KmcException("ARSN and ARSN length must agree");
//        }
    }

    /**
     * Verify ARSN parameters.
     * <p>
     * If either ARSN or ARSN Length are supplied, both are required.
     *
     * @param arsn    anti-replay sequence number
     * @param arsnLen arsn length in bytes
     * @return byte array
     * @throws KmcException ex
     */
    public static byte[] verifyArsn(String arsn, Short arsnLen) throws KmcException {
        if (checkArgsNotNull("When specifying '--arsn' or '--arsnlen', both are required", arsn, arsnLen)) {
            byte[] arcBytes = convertHexToBytes(arsn, "ARSN must be a hex string");
            return arcBytes;
        }
        return null;
    }

    /**
     * Helper to Convert a Hex string to a byte array
     *
     * @param hexString hex string
     * @param msg       message
     * @return byte array
     * @throws KmcException ex
     */
    public static byte[] convertHexToBytes(String hexString, String msg) throws KmcException {
        return convertHexToBytes(hexString, msg, false);
    }

    /**
     * Convert hex string to bytes
     *
     * @param hexString hex string
     * @param msg       message
     * @param allowNull allow null values
     * @return byte array
     * @throws KmcException exception
     */
    public static byte[] convertHexToBytes(String hexString, String msg, boolean allowNull) throws KmcException {
        if (allowNull && hexString == null) {
            return null;
        }
        try {
            String abm = hexString.toLowerCase().replaceFirst("^0x", "");
            return Hex.decodeHex(abm);
        } catch (DecoderException e) {
            throwEx(msg);
        }
        return null;
    }


}
