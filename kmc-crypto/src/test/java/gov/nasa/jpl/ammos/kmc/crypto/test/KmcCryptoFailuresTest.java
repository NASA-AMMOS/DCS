package gov.nasa.jpl.ammos.kmc.crypto.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;

import org.junit.jupiter.api.Test;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoFailures;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoRootCause;

public class KmcCryptoFailuresTest {

    // same shape as DecrypterLibrary reporting a GCM tag mismatch
    private static KmcCryptoException tagMismatch() {
        AEADBadTagException root = new AEADBadTagException("Tag mismatch");
        IOException io = new IOException("javax.crypto.AEADBadTagException: Tag mismatch", root);
        return new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                "Exception on reading/writing io stream: " + io, io);
    }

    @Test
    public void tagMismatch_errorCodeAndRootCause() {
        KmcCryptoException e = tagMismatch();
        assertEquals("CRYPTO_ALGORITHM_ERROR", KmcCryptoFailures.getErrorCode(e));
        assertEquals(KmcCryptoRootCause.AUTH_TAG_MISMATCH, KmcCryptoFailures.getRootCause(e));
    }

    @Test
    public void exceptionsReportTheirRootCause() {
        assertEquals(KmcCryptoRootCause.AUTH_TAG_MISMATCH, tagMismatch().getRootCause());
        KmcCryptoManagerException e = new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_FILE_NOT_FOUND, "no kmc-crypto.cfg", null);
        assertEquals(KmcCryptoRootCause.CONFIG_ERROR, e.getRootCause());
    }

    @Test
    public void errorCodeIsFoundDeeperInChain() {
        RuntimeException wrapper = new RuntimeException("wrapper", tagMismatch());
        assertEquals("CRYPTO_ALGORITHM_ERROR", KmcCryptoFailures.getErrorCode(wrapper));
    }

    @Test
    public void noCause() {
        assertNull(KmcCryptoFailures.getErrorCode(null));
        assertNull(KmcCryptoFailures.getRootCause(null));
        assertFalse(KmcCryptoFailures.isNoKeySource(null));
    }

    @Test
    public void badPaddingRootCause() {
        IOException io = new IOException(new BadPaddingException("Given final block not properly padded"));
        KmcCryptoException e = new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, "io error", io);
        assertEquals(KmcCryptoRootCause.BAD_PADDING, KmcCryptoFailures.getRootCause(e));
    }

    @Test
    public void deepestKmcCodeBelowNullValue() {
        // same shape as KmcCryptoManager wrapping a library constructor error
        KmcCryptoException kms = new KmcCryptoException(KmcCryptoErrorCode.KMS_CONNECTION_ERROR, "no KMS", null);
        KmcCryptoManagerException e = new KmcCryptoManagerException(KmcCryptoManagerErrorCode.NULL_VALUE,
                "Failed to create instance", new InvocationTargetException(kms));
        assertEquals(KmcCryptoRootCause.KMS_UNREACHABLE, KmcCryptoFailures.getRootCause(e));
    }

    @Test
    public void networkExceptionIsKmsUnreachable() {
        KmcCryptoException e = new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, "get key failed",
                new ConnectException("Connection refused"));
        assertEquals(KmcCryptoRootCause.KMS_UNREACHABLE, KmcCryptoFailures.getRootCause(e));
    }

    @Test
    public void noKeySource() {
        KmcCryptoException e = new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR,
                KmcCryptoFailures.NO_KEY_SOURCE_ERROR_MSG, null);
        assertEquals(KmcCryptoRootCause.NO_KEY_SOURCE, KmcCryptoFailures.getRootCause(e));
    }

    @Test
    public void noKeySourceFoundDeeperInChain() {
        KmcCryptoException cause = new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR,
                KmcCryptoFailures.NO_KEY_SOURCE_ERROR_MSG, null);
        KmcCryptoManagerException e = new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR, "failed to create Encrypter", cause);
        assertTrue(KmcCryptoFailures.isNoKeySource(e));
        assertFalse(KmcCryptoFailures.isNoKeySource(new KmcCryptoException(
                KmcCryptoErrorCode.CRYPTO_KEY_ERROR, "keyRef does not exist", null)));
    }

    @Test
    public void badParameter() {
        KmcCryptoManagerException e = new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, "bad macLength", null);
        assertEquals(KmcCryptoRootCause.INVALID_PARAMETER, KmcCryptoFailures.getRootCause(e));
    }

    @Test
    public void unknownRootCause() {
        assertEquals(KmcCryptoRootCause.UNKNOWN, KmcCryptoFailures.getRootCause(new IllegalStateException("x")));
    }

    @Test
    public void cyclicChainTerminates() {
        Exception a = new Exception("a");
        Exception b = new Exception("b", a);
        a.initCause(b);
        assertEquals(KmcCryptoRootCause.UNKNOWN, KmcCryptoFailures.getRootCause(a));
    }
}
