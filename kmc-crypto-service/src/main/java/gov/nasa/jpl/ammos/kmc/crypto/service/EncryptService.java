package gov.nasa.jpl.ammos.kmc.crypto.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

import gov.nasa.jpl.ammos.kmc.crypto.Encrypter;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.library.KmcKeyServiceClient;
import gov.nasa.jpl.ammos.kmc.crypto.model.CryptoServiceResponse;
import gov.nasa.jpl.ammos.kmc.crypto.model.EncryptServiceResponse;
import gov.nasa.jpl.ammos.kmc.crypto.model.Status;

/**
 * The servlet provides service for encrypting the input data (plaintext).
 * Depending on the specified key, the data could be encrypted with a symmetric or
 * asymmetric algorithm.
 *
 *
 */
@WebServlet("/encrypt")
public class EncryptService extends HttpServlet {
    private static final long serialVersionUID = 3114402936011612850L;

    private static final Logger logger = LoggerFactory.getLogger(EncryptService.class);
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private static final int maxBytes = KmcCryptoServiceConfiguration.MAX_CRYPTO_SERVICE_BYTES;
    private static final int AES_BLOCK_SIZE = 16;  // AES block size in bytes
    private String kmcHome;

    @Override
    public final void init(final ServletConfig config) throws ServletException {
        super.init(config);

        kmcHome = System.getenv(KmcCryptoManager.ENV_KMC_CRYPTO_SERVICE_HOME);
        if (kmcHome == null) {
            kmcHome = KmcCryptoManager.DEFAULT_KMC_CRYPTO_SERVICE_HOME;
        }
    }

    /*
     * Post URI: /encrypt?keyRef=string&transformation=string&iv=base64&encryptOffset=int&macLength=int
     *
     * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected final void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        ServletOutputStream out = response.getOutputStream();
        response.setContentType("application/json");

        // create KmcCryptoManager early to initialize the logger.
        KmcCryptoManager cryptoManager = null;
        try {
            String[] args = new String[] {
                    "-" + KmcCryptoManager.CFG_KMC_CRYPTO_CONFIG_DIR + "=" + kmcHome + "/etc"
            };
            cryptoManager = new KmcCryptoManager(args);
        } catch (KmcCryptoManagerException e) {
            String msg = "Failed to create KmcCryptoManager: " + e;
            failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        CryptoServiceUtilities.logRequestParameters(logger, audit, request);

        // get request parameters
        String keyRef = request.getParameter("keyRef");
        if (keyRef == null) {
            String msg = "Missing keyRef parameter.";
            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        } else {
            logger.debug("request parameter: keyRef = {}", keyRef);
        }
        String transformation = request.getParameter("transformation");
        if (transformation != null) {
            logger.debug("request parameter: EncryptService: transformation = {}", transformation);
        }
        String iv = request.getParameter("iv");
        if (iv != null) {
            logger.debug("request parameter: iv = {}", iv);
        }
        int encryptOffset = 0;
        String encryptOffsetParam = request.getParameter("encryptOffset");
        if (encryptOffsetParam != null) {
            logger.debug("request parameter: encryptOffset = {}", encryptOffsetParam);
            try {
                encryptOffset = Integer.parseInt(encryptOffsetParam);
            } catch (NumberFormatException e) {
                String msg = "Invalid encryptOffset value: " + encryptOffsetParam;
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            }
        }
        String macLength = request.getParameter("macLength");
        if (macLength != null) {
            logger.debug("request parameter: macLength = {}", macLength);
            try {
                cryptoManager.setMacLength(macLength);
            } catch (KmcCryptoManagerException e) {
                String msg = "Invalid macLength value (" + macLength + "): " + e;
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            }
        }
        String algorithm = request.getParameter("algorithm");
        if (algorithm != null) {
            String msg = "Encryption does not use the algorithm parameter.  The key determines the crypto algorithm.";
            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }

        // Read the plaintext from request
        InputStream reader = request.getInputStream();
        int bufSize = maxBytes + 1;  // add a byte to detect exceeding max
        byte[] readBuffer = new byte[bufSize];
        int offset = 0;
        int bytesRead = -1;
        while ((bytesRead = reader.read(readBuffer, offset, bufSize - offset)) > 0) {
            if (logger.isTraceEnabled()) {
                logger.trace("EncryptService: offset = {}, read {} bytes.", offset, bytesRead);
            }
            if (offset + bytesRead > maxBytes) {
                String msg = "Input data exceeds maximum size of " + maxBytes + " bytes.";
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            } else {
                offset = offset + bytesRead;
            }
        }
        logger.debug("Finished reading input stream of {} bytes.", offset);
        if (offset == 0) {
            String msg = "Input has 0 byte to encrypt.";
            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        byte[] plaintext = Arrays.copyOf(readBuffer, offset);
        InputStream bis = new ByteArrayInputStream(plaintext);
        int encryptedSize = (plaintext.length / AES_BLOCK_SIZE + 1) * AES_BLOCK_SIZE;
        ByteArrayOutputStream eos = new ByteArrayOutputStream(encryptedSize);

        try {
            if (transformation != null) {
                cryptoManager.setCipherTransformation(transformation);
            }
        } catch (KmcCryptoManagerException e) {
            String msg = "Error in cipher transformation: " + e;
            failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        Encrypter encrypter;
        try {
            encrypter = cryptoManager.createEncrypter(keyRef);
        } catch (KmcCryptoManagerException e) {
            String msg = "EncryptService: ";
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
        try {
            String metadata = encrypter.encrypt(bis, encryptOffset, iv, eos);
            logger.debug("metadata = {}", metadata);
            byte[] encryptedData = eos.toByteArray();
            Status status = new Status(HttpServletResponse.SC_OK, "OK");
            EncryptServiceResponse res = new EncryptServiceResponse(status, metadata, encryptedData);
            logger.debug("encryptedData size = {}, base64 size = {}", encryptedData.length, res.getBase64Ciphertext().length());
            logger.trace("EncryptServiceResponse = {}", gson.toJson(res));
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(res));
            out.flush();
            audit.info("EncryptService: User successfully encrypted data of " + offset + " bytes using keyRef " + keyRef);
        } catch (KmcCryptoException e) {
            String msg = "Exception during encryption: " + e;
            logger.error(msg);
            if (e.getErrorCode() == KmcCryptoErrorCode.INVALID_INPUT_VALUE) {
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            } else {
                failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            }
        }
    }

    private void failureResponse(final HttpServletResponse response, final int errorCode, final String msg)
            throws IOException {
        audit.info("EncryptService: Failure response: code " + errorCode + ", error: " + msg);
        logger.error("failureResponse() HTTP code: {}, {}", errorCode, msg);
        Status status = new Status(errorCode, msg);
        CryptoServiceResponse res = new CryptoServiceResponse(status, null);
        response.setStatus(errorCode);
        response.getOutputStream().print(gson.toJson(res));
        response.getOutputStream().flush();
    }

}
