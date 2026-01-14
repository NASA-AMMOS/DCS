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

import gov.nasa.jpl.ammos.kmc.crypto.IcvCreator;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.library.KmcKeyServiceClient;
import gov.nasa.jpl.ammos.kmc.crypto.model.CryptoServiceResponse;
import gov.nasa.jpl.ammos.kmc.crypto.model.IcvCreateServiceResponse;
import gov.nasa.jpl.ammos.kmc.crypto.model.Status;

/**
 * The servlet provides service for creating an integrity check value (ICV) of the input data.
 * Depending on the specified key, the ICV could be created with the Message Digest (no key),
 * HMAC (symmetric key), CMAC (symmetric key) or Digital Signature (asymmetric key) algorithm.
 *
 *
 */
@WebServlet("/icv-create")
public class IcvCreateService extends HttpServlet {
    private static final long serialVersionUID = 4114402936011612850L;

    private static final Logger logger = LoggerFactory.getLogger(IcvCreateService.class);
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

    // /icv-create?keyRef=keyRef&macLength=int&algorithm=algorithm
    @Override
    protected final void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("application/json");

        // create KmcCryptoManager early to initialize the logger.
        KmcCryptoManager cryptoManager;
        try {
            String[] args = new String[] {
                    "-" + KmcCryptoManager.CFG_KMC_CRYPTO_CONFIG_DIR + "=" + kmcHome + "/etc"
            };
            cryptoManager = new KmcCryptoManager(args);
        } catch (KmcCryptoManagerException e) {
            String msg = "IcvCreateService: failed to create KmcCryptoManager: " + e;
            failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        CryptoServiceUtilities.logRequestParameters(logger, audit, request);

        String keyRef = request.getParameter("keyRef");
        if (keyRef != null) {
            logger.debug("IcvCreateService: keyRef = {}", keyRef);
        }
        String algorithm = request.getParameter("algorithm");
        if (algorithm != null) {
            logger.debug("IcvCreateService: algorithm = {}", algorithm);
        }
        String macLength = request.getParameter("macLength");
        if (macLength != null) {
            logger.debug("IcvCreateService: macLength = {}", macLength);
            try {
                cryptoManager.setMacLength(macLength);
            } catch (KmcCryptoManagerException e) {
                String msg = "IcvCreateService: bad macLength parameter (" + macLength + "): " + e;
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            }
        }

        // only Message Digest and Digital Signature use the algorithm parameter,
        // returns error for other crypto requests.
        if (algorithm != null) {
            try {
                if (keyRef == null || "null".equals(keyRef)) {
                    // Message Digest does not use a key
                    boolean allowed = cryptoManager.isAllowedAlgorithm(algorithm,
                            KmcCryptoManager.CFG_ALLOWED_MESSAGE_DIGEST_ALGORITHMS);
                    if (allowed) {
                        cryptoManager.setMessageDigestAlgorithm(algorithm);
                    } else {
                        String msg = "IcvCreateService: keyRef is not found in the request and the algorithm ("
                                 + algorithm + ") is not an allowed Message Digest algorithm";
                        failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                        return;
                    }
                } else if (algorithm.startsWith("SHA") && algorithm.endsWith("withRSA")) {
                    // digital signature algorithm
                    boolean allowed = cryptoManager.isAllowedAlgorithm(algorithm,
                            KmcCryptoManager.CFG_ALLOWED_DIGITAL_SIGNATURE_ALGORITHMS);
                    if (allowed) {
                        if (macLength != null) {
                            String msg = "Digital Signature does not support macLength.";
                            logger.error(msg);
                            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                            return;
                        }
                        cryptoManager.setDigitalSignatureAlgorithm(algorithm);
                    } else {
                        String msg = "IcvCreateService: the algorithm (" + algorithm
                                + ") is not an allowed Digital Signature algorithm";
                        failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                        return;
                    }
                } else {
                    String msg = "IcvCreateService: The algorithm parameter"
                            + " is only allowed for Message Digest or Digital Signature."
                            + " Other crypto functions use the algorithm specified by the key.";
                    failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                    return;
                }
            } catch (KmcCryptoManagerException e) {
                String msg = "IcvCreateService: Exception in setting algorithm: " + e;
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            }
        }

        if (macLength != null) {
            try {
                cryptoManager.setMacLength(macLength);
            } catch (KmcCryptoManagerException e) {
                String msg = "IcvCreateService: bad macLength parameter (" + macLength + "): " + e;
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            }
        }

        IcvCreator icvCreator;
        try {
            if (keyRef == null || "null".equals(keyRef)) {
                icvCreator = cryptoManager.createIcvCreator();
            } else {
                icvCreator = cryptoManager.createIcvCreator(keyRef);
            }
        } catch (KmcCryptoManagerException e) {
            String msg = "IcvCreateService: ";
            if (e.getCause() == null) {
                msg = msg + e.getMessage();
            } else {
                msg = msg + e.getCause().getMessage();
            }
            if (e.getErrorCode() == KmcCryptoManagerErrorCode.CRYPTO_KEY_ERROR) {
                if (msg.contains(KmcKeyServiceClient.NO_KEY_SOURCE_ERROR_MSG)) {
                    // no key source
                    failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
                } else {
                    // non-exist keyRef
                    failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                }
            } else if (e.getErrorCode() == KmcCryptoManagerErrorCode.CRYPTO_ALGORITHM_ERROR) {
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            } else {
                failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            }
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
                logger.trace("IcvCreateService: offset = " + offset + ", read " + bytesRead + " bytes.");
            }
            if (offset + bytesRead > maxBytes) {
                String msg = "IcvCreateService: input data exceeds maximum size of " + maxBytes + " bytes.";
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            } else {
                offset = offset + bytesRead;
            }
        }
        logger.debug("IcvCreateService: Finished reading input stream of " + offset + " bytes.");
        if (offset == 0) {
            String msg = "IcvCreateService: empty input data.";
            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        byte[] data = Arrays.copyOf(dataBuffer, offset);
        InputStream bis = new ByteArrayInputStream(data);

        try {
            String metadata = icvCreator.createIntegrityCheckValue(bis);
            logger.debug("metadata = " + metadata);
            Status status = new Status(HttpServletResponse.SC_OK, "OK");
            IcvCreateServiceResponse res = new IcvCreateServiceResponse(status, metadata);
            logger.info("IcvCreateServiceResponse = " + gson.toJson(res));
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(res));
            out.flush();

            audit.info("IcvCreateService: User successfully created ICV for data of " + offset + " bytes using keyRef " + keyRef);
        } catch (KmcCryptoException e) {
            String msg = "IcvCreateService: Exception during ICV Creation: " + e;
            failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    private void failureResponse(final HttpServletResponse response, final int errorCode, final String msg)
            throws IOException {
        audit.info("IcvCreateService: Failure response: code " + errorCode + ", error: " + msg);
        logger.error(msg);
        Status status = new Status(errorCode, msg);
        CryptoServiceResponse res = new CryptoServiceResponse(status, null);
        response.setStatus(errorCode);
        response.getOutputStream().print(gson.toJson(res));
        response.getOutputStream().flush();
    }

}
