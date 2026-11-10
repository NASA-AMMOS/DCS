package gov.nasa.jpl.ammos.kmc.crypto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.library.KmcKeyServiceClient;
import gov.nasa.jpl.ammos.kmc.crypto.model.RootCause;
import gov.nasa.jpl.ammos.kmc.crypto.model.Status;

public class CryptoServiceUtilitiesTest {

    private static final Gson gson = new Gson();

    // same shape as DecrypterLibrary reporting a GCM tag mismatch
    private static KmcCryptoException tagMismatch() {
        AEADBadTagException root = new AEADBadTagException("Tag mismatch");
        IOException io = new IOException("javax.crypto.AEADBadTagException: Tag mismatch", root);
        return new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                "Exception on reading/writing io stream: " + io, io);
    }

    @Test
    public void errorCodeAndRootCauseAreIncluded() {
        Status status = CryptoServiceUtilities.failureStatus(400, "failed", tagMismatch());
        assertEquals("CRYPTO_ALGORITHM_ERROR", status.getErrorCode());
        assertEquals("AUTH_TAG_MISMATCH", status.getRootCause());
    }

    @Test
    public void errorCodeIsFoundDeeperInChain() {
        RuntimeException wrapper = new RuntimeException("wrapper", tagMismatch());
        assertEquals("CRYPTO_ALGORITHM_ERROR", CryptoServiceUtilities.getErrorCode(wrapper));
    }

    @Test
    public void noCauseKeepsOriginalJson() {
        Status status = CryptoServiceUtilities.failureStatus(400, "missing metadata.", null);
        assertEquals(gson.toJson(new Status(400, "missing metadata.")), gson.toJson(status));
        assertEquals("{\"httpCode\":400,\"reason\":\"missing metadata.\"}", gson.toJson(status));
    }

    @Test
    public void badPaddingRootCause() {
        IOException io = new IOException(new BadPaddingException("Given final block not properly padded"));
        KmcCryptoException e = new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, "io error", io);
        assertEquals(RootCause.BAD_PADDING, CryptoServiceUtilities.findRootCause(e));
    }

    @Test
    public void deepestKmcCodeBelowNullValue() {
        // same shape as KmcCryptoManager wrapping a library constructor error
        KmcCryptoException kms = new KmcCryptoException(KmcCryptoErrorCode.KMS_CONNECTION_ERROR, "no KMS", null);
        KmcCryptoManagerException e = new KmcCryptoManagerException(KmcCryptoManagerErrorCode.NULL_VALUE,
                "Failed to create instance", new InvocationTargetException(kms));
        assertEquals(RootCause.KMS_UNREACHABLE, CryptoServiceUtilities.findRootCause(e));
    }

    @Test
    public void networkExceptionIsKmsUnreachable() {
        KmcCryptoException e = new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR, "get key failed",
                new ConnectException("Connection refused"));
        assertEquals(RootCause.KMS_UNREACHABLE, CryptoServiceUtilities.findRootCause(e));
    }

    @Test
    public void noKeySource() {
        KmcCryptoException e = new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR,
                KmcKeyServiceClient.NO_KEY_SOURCE_ERROR_MSG, null);
        assertEquals(RootCause.NO_KEY_SOURCE, CryptoServiceUtilities.findRootCause(e));
    }

    @Test
    public void noKeySourceFoundDeeperInChain() {
        KmcCryptoException cause = new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_KEY_ERROR,
                KmcKeyServiceClient.NO_KEY_SOURCE_ERROR_MSG, null);
        KmcCryptoManagerException e = new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR, "failed to create Encrypter", cause);
        assertTrue(CryptoServiceUtilities.isNoKeySource(e));
        assertFalse(CryptoServiceUtilities.isNoKeySource(new KmcCryptoException(
                KmcCryptoErrorCode.CRYPTO_KEY_ERROR, "keyRef does not exist", null)));
    }

    @Test
    public void badParameter() {
        KmcCryptoManagerException e = new KmcCryptoManagerException(
                KmcCryptoManagerErrorCode.CONFIG_PARAMETER_VALUE_INVALID, "bad macLength", null);
        assertEquals(RootCause.INVALID_PARAMETER, CryptoServiceUtilities.findRootCause(e));
    }

    @Test
    public void unknownRootCause() {
        assertEquals(RootCause.UNKNOWN, CryptoServiceUtilities.findRootCause(new IllegalStateException("x")));
    }

    @Test
    public void cyclicChainTerminates() {
        Exception a = new Exception("a");
        Exception b = new Exception("b", a);
        a.initCause(b);
        assertEquals(RootCause.UNKNOWN, CryptoServiceUtilities.findRootCause(a));
    }
}
