package gov.nasa.jpl.ammos.kmc.crypto.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import gov.nasa.jpl.ammos.kmc.crypto.IcvVerifier;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.library.KmcKeyServiceClient;
import gov.nasa.jpl.ammos.kmc.crypto.model.CryptoServiceResponse;
import gov.nasa.jpl.ammos.kmc.crypto.model.IcvVerifyServiceResponse;
import gov.nasa.jpl.ammos.kmc.crypto.model.Status;

/**
 * The servlet provides service for creating an integrity check value (ICV) of the input data.
 * Depending on the specified key, the ICV could be created with the Message Digest (no key),
 * HMAC (symmetric key), or Digital Signature (asymmetric key) algorithm.
 *
 *
 */
@WebServlet("/icv-verify")
public class IcvVerifyService extends HttpServlet {
    private static final long serialVersionUID = 4114402936011612851L;

    private static final Logger logger = LoggerFactory.getLogger(IcvVerifyService.class);
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private static final int maxBytes = KmcCryptoServiceConfiguration.MAX_CRYPTO_SERVICE_BYTES;
    private String kmcHome;

    @Override
    public final void init(final ServletConfig config) throws ServletException {
        super.init(config);

        kmcHome = System.getenv(KmcCryptoManager.ENV_KMC_CRYPTO_SERVICE_HOME);
        if (kmcHome == null) {
            kmcHome = KmcCryptoManager.DEFAULT_KMC_CRYPTO_SERVICE_HOME;
        }
    }

    @Override
    protected final void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("application/json");

        KmcCryptoManager cryptoManager = null;
        try {
            String[] args = new String[] {
                    "-" + KmcCryptoManager.CFG_KMC_CRYPTO_CONFIG_DIR + "=" + kmcHome + "/etc"
            };
            cryptoManager = new KmcCryptoManager(args);
        } catch (KmcCryptoManagerException e) {
            String msg = "IcvVerifyService: failed to create KmcCryptoManager: " + e;
            failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        CryptoServiceUtilities.logRequestParameters(logger, audit, request);

        String metadata = request.getParameter("metadata");
        logger.debug("IcvVerifyService: metadata = " + metadata);
        if (metadata == null) {
            String msg = "IcvVerifyService: missing metadata parameter.";
            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }

        // Read the data from request
        InputStream reader = request.getInputStream();
        int bufSize = maxBytes + 1;  // add a byte to detect exceeding max
        byte[] dataBuffer = new byte[bufSize];
        int offset = 0;
        int bytesRead = -1;
        while ((bytesRead = reader.read(dataBuffer, offset, bufSize - offset)) > 0) {
            if (logger.isTraceEnabled()) {
                logger.trace("IcvVerifyService: offset = " + offset + ", read " + bytesRead + " bytes.");
            }
            if (offset + bytesRead > maxBytes) {
                String msg = "IcvVerifyService: input data exceeds maximum size of " + maxBytes + " bytes.";
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            } else {
                offset = offset + bytesRead;
            }
        }
        logger.debug("IcvVerifyService: Finished reading input stream of " + offset + " bytes.");
        if (offset == 0) {
            String msg = "IcvVerifyService: empty input data.";
            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        byte[] data = Arrays.copyOf(dataBuffer, offset);
        InputStream bis = new ByteArrayInputStream(data);

        IcvVerifier icvVerifier;
        try {
            icvVerifier = cryptoManager.createIcvVerifier();
        } catch (KmcCryptoManagerException e) {
            String msg = "IcvVerifyService: " + e.getMessage();
            if (e.getErrorCode() == KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR) {
                if (msg.contains(KmcKeyServiceClient.NO_KEY_SOURCE_ERROR_MSG)) {
                    // no key source
                    failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                } else {
                    // non-exist keyRef
                    failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                }
            } else {
                failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            }
            return;
        }
        try {
            boolean result = icvVerifier.verifyIntegrityCheckValue(bis, metadata);
            logger.debug("IcvVerifyService: result = " + result);
            Status status = new Status(HttpServletResponse.SC_OK, "OK");
            IcvVerifyServiceResponse res = new IcvVerifyServiceResponse(status, result);
            logger.info("IcvVerifyServiceResponse = " + gson.toJson(res));
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(res));
            out.flush();

            audit.info("IcvVerifyService: User successfully verifed data of " + offset + " bytes with ICV");
        } catch (KmcCryptoException e) {
            String msg = "IcvVerifyService: " + e.getMessage();
            if (e.getErrorCode() == KmcCryptoErrorCode.CRYPTO_KEY_ERROR) {
                if (msg.contains(KmcKeyServiceClient.NO_KEY_SOURCE_ERROR_MSG)) {
                    // no key source
                    failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                } else {
                    // non-exist keyRef
                    failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                }
            } else if (e.getErrorCode() == KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR
                    || e.getErrorCode() == KmcCryptoErrorCode.CRYPTO_METADATA_ERROR) {
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            } else {
                failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            }
        }
    }

    private void failureResponse(final HttpServletResponse response, final int errorCode, final String msg)
            throws IOException {
        audit.info("IcvVerifyService: Failure response: code " + errorCode + ", error: " + msg);
        logger.error(msg);
        Status status = new Status(errorCode, msg);
        CryptoServiceResponse res = new CryptoServiceResponse(status, null);
        response.setStatus(errorCode);
        response.getOutputStream().print(gson.toJson(res));
        response.getOutputStream().flush();
    }

}
