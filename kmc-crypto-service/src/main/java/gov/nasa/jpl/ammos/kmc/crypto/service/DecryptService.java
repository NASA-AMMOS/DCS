package gov.nasa.jpl.ammos.kmc.crypto.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import gov.nasa.jpl.ammos.kmc.crypto.Decrypter;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException.KmcCryptoManagerErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.library.KmcKeyServiceClient;
import gov.nasa.jpl.ammos.kmc.crypto.model.CryptoServiceResponse;
import gov.nasa.jpl.ammos.kmc.crypto.model.DecryptServiceResponse;
import gov.nasa.jpl.ammos.kmc.crypto.model.Status;

/**
 * The servlet provides service for decrypting the input encrypted data (ciphertext).
 * Depending on the specified key, the data could be encrypted with a symmetric or
 * asymmetric algorithm.
 *
 *
 */
@WebServlet("/decrypt")
public class DecryptService extends HttpServlet {
    private static final long serialVersionUID = 3114402936011612851L;

    private static final Logger logger = LoggerFactory.getLogger(DecryptService.class);
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

    /**
     *
     * Post URI: /decrypt?metadata=value
     *
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     *
     */
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
            String msg = "DecryptService: failed to create KmcCryptoManager: " + e;
            failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        CryptoServiceUtilities.logRequestParameters(logger, audit, request);

        String metadata = request.getParameter("metadata");
        if (metadata == null) {
            String msg = "DecryptService: missing metadata.";
            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        logger.debug("DecryptService: metadata = {}", metadata);

        // Read the ciphertext from request
        InputStream reader = request.getInputStream();
        int bufSize = maxBytes + 1;  // add a byte to detect exceeding max
        byte[] readBuffer = new byte[bufSize];
        int offset = 0;
        int bytesRead = -1;
        while ((bytesRead = reader.read(readBuffer, offset, bufSize - offset)) > 0) {
            logger.trace("DecryptService: offset = {}, read {} bytes.", offset, bytesRead);
            if (offset + bytesRead > maxBytes) {
                String msg = "DecryptService: input data exceeds maximum size of " + maxBytes + " bytes.";
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            } else {
                offset = offset + bytesRead;
            }
        }
        logger.debug("DecryptService: Finished reading input stream of {} bytes.", offset);
        if (offset == 0) {
            String msg = "DecryptService: empty input ciphertext.";
            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }

        // input can be ciphertext with or without base64 encoding
        byte[] ciphertext = Arrays.copyOfRange(readBuffer, 0, offset);
        try {
            String cipherString = new String(ciphertext, "UTF-8");
            logger.trace("cipherString = {}", cipherString);
            ciphertext = Base64.getDecoder().decode(cipherString);
            logger.debug("input is ciphertext with base64 encoding, encoded length = {}, ciphertext length = {}", offset, ciphertext.length);
        } catch (IllegalArgumentException e) {
            logger.trace("Failed to base64 decode input: {}", e.toString());
            logger.debug("input is ciphertext without base64 encoding, ciphertext length = {}", ciphertext.length);
        }
        InputStream bis = new ByteArrayInputStream(ciphertext);
        int decryptedSize = ciphertext.length;
        ByteArrayOutputStream eos = new ByteArrayOutputStream(decryptedSize);

        Decrypter decrypter;
        try {
            decrypter = cryptoManager.createDecrypter();
        } catch (KmcCryptoManagerException e) {
            String msg = "DecryptService: " + e.getMessage();
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
            decrypter.decrypt(bis, eos, metadata);
            byte[] plaintext = eos.toByteArray();
            logger.debug("plaintext size = {}", plaintext.length);
            logger.trace("plaintext = {}", new String(plaintext));
            Status status = new Status(HttpServletResponse.SC_OK, "OK");
            DecryptServiceResponse res = new DecryptServiceResponse(status, plaintext);
            logger.trace("DecryptServiceResponse = {}", gson.toJson(res));
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(res));
            out.flush();

            audit.info("DecryptService: User successfully decrypted data of " + offset + " bytes.");
        } catch (KmcCryptoException e) {
            /*
            if (e.getMessage().contains("does not exist")
                    || e.getMessage().contains("Key algorithm")) {
                String msg = "DecryptService: Exception during decryption: " + e;
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            } else {
                String msg = "DecryptService: Exception during decryption: " + e;
                failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            }*/
            String msg = "DecryptService: Exception during decryption: " + e;
            if (e.getErrorCode() == KmcCryptoErrorCode.CRYPTO_KEY_ERROR) {
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            } else if (e.getErrorCode() == KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR) {
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            } else if (e.getErrorCode() == KmcCryptoErrorCode.CRYPTO_METADATA_ERROR) {
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            } else {
                failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            }
        }
    }

    private void failureResponse(final HttpServletResponse response, final int errorCode, final String msg)
            throws IOException {
        audit.info("DecryptService: Failure response: code " + errorCode + ", error: " + msg);
        logger.error("HTTP code: {}, {}", errorCode, msg);
        Status status = new Status(errorCode, msg);
        CryptoServiceResponse res = new CryptoServiceResponse(status, null);
        response.setStatus(errorCode);
        response.getOutputStream().print(gson.toJson(res));
        response.getOutputStream().flush();
    }

}
