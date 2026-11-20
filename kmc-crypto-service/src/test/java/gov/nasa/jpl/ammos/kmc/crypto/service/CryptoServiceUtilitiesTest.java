package gov.nasa.jpl.ammos.kmc.crypto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import javax.crypto.AEADBadTagException;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.model.Status;

// The mapping itself is tested in kmc-crypto (KmcCryptoFailuresTest); these tests check the service's status.
public class CryptoServiceUtilitiesTest {

    private static final Gson gson = new Gson();

    @Test
    public void errorCodeAndRootCauseAreIncluded() {
        // same shape as DecrypterLibrary reporting a GCM tag mismatch
        AEADBadTagException root = new AEADBadTagException("Tag mismatch");
        IOException io = new IOException("javax.crypto.AEADBadTagException: Tag mismatch", root);
        KmcCryptoException e = new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                "Exception on reading/writing io stream: " + io, io);
        Status status = CryptoServiceUtilities.failureStatus(400, "failed", e);
        assertEquals("CRYPTO_ALGORITHM_ERROR", status.getErrorCode());
        assertEquals("AUTH_TAG_MISMATCH", status.getRootCause());
    }

    @Test
    public void noCauseKeepsOriginalJson() {
        Status status = CryptoServiceUtilities.failureStatus(400, "missing metadata.", null);
        assertEquals(gson.toJson(new Status(400, "missing metadata.")), gson.toJson(status));
        assertEquals("{\"httpCode\":400,\"reason\":\"missing metadata.\"}", gson.toJson(status));
    }
}
