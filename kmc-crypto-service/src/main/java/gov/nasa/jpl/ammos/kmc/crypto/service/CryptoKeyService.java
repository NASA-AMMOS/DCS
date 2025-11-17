package gov.nasa.jpl.ammos.kmc.crypto.service;

import java.io.IOException;

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

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.library.KeyServiceClient;
import gov.nasa.jpl.ammos.kmc.crypto.library.KmcKey;
import gov.nasa.jpl.ammos.kmc.crypto.library.KmcKeyServiceClient;
import gov.nasa.jpl.ammos.kmc.crypto.model.CryptoKeyServiceResponse;
import gov.nasa.jpl.ammos.kmc.crypto.model.KeyInfo;
import gov.nasa.jpl.ammos.kmc.crypto.model.Status;

/**
 * The servlet provides service for retrieving information of a key by its keyRef.
 *
 *
 */
@WebServlet("/key-info")
public class CryptoKeyService extends HttpServlet {
    private static final long serialVersionUID = 1056079085902283284L;

    private static final Logger logger = LoggerFactory.getLogger(CryptoKeyService.class);
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

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
    protected final void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("application/json");

        KmcCryptoManager cryptoManager;
        try {
            String[] args = new String[] {
                    "-" + KmcCryptoManager.CFG_KMC_CRYPTO_CONFIG_DIR + "=" + kmcHome + "/etc"
            };
            cryptoManager = new KmcCryptoManager(args);
        } catch (KmcCryptoManagerException e) {
            String msg = "CryptoKeyService: Failed to get KmcCryptoManager: " + e;
            failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return;
        }

        CryptoServiceUtilities.logRequestParameters(logger, audit, request);

        String keyRef = request.getParameter("keyRef");
        if (keyRef == null) {
            String msg = "CryptoKeyService: missing keyRef parameter.";
            failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        } else {
            logger.debug("Retrieve key of keyRef = " + keyRef);
        }

        KmcKey kmcKey = getKey(cryptoManager, keyRef, response);
        if (kmcKey != null) {
            Status status = new Status(HttpServletResponse.SC_OK, "OK");
            KeyInfo keyInfo = new KeyInfo(keyRef, kmcKey.getKeyType(),
                kmcKey.getJavaKeyAlgorithm(), kmcKey.getKeyLength(), kmcKey.getState(), kmcKey.getUsageMask());
            CryptoKeyServiceResponse res = new CryptoKeyServiceResponse(status);
            res.setKeyInfo(keyInfo);
            logger.debug("CryptoKeyService Response = " + gson.toJson(res));
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(res));
            out.flush();
        }
    }

    private void failureResponse(final HttpServletResponse response, final int errorCode, final String msg)
            throws IOException {
        audit.info("CryptoKeyService: Failure response: code " + errorCode + ", error: " + msg);
        logger.error(msg);
        Status status = new Status(errorCode, msg);
        CryptoKeyServiceResponse res = new CryptoKeyServiceResponse(status);
        response.setStatus(errorCode);
        response.getOutputStream().print(gson.toJson(res));
        response.getOutputStream().flush();
    }

    private KmcKey getKey(final KmcCryptoManager cryptoManager, final String keyRef,
            final HttpServletResponse response) throws IOException {

        KeyServiceClient keyClient;
        try {
            keyClient = new KmcKeyServiceClient(cryptoManager);
        } catch (KmcCryptoException e) {
            String msg = "CryptoKeyService: ";
            if (e.getCause() == null) {
                msg = msg + e.getMessage();
            } else {
                msg = msg + e.getCause().getMessage();
            }
            failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            return null;
        }

        // retrieve crypto key
        KmcKey kmcKey;
        try {
            kmcKey = keyClient.getKmcKey(keyRef);
            if (kmcKey == null) {
                String msg = "keyRef does not exist: " + keyRef;
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
                return null;
            }
            audit.info("CryptoKeyService: User obtained key " + keyRef + " from KMS.");
        } catch (KmcCryptoException e) {
            if (e.getErrorCode() == KmcCryptoErrorCode.CRYPTO_KEY_ERROR) {
                String msg = "keyRef does not exist: " + keyRef;
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            } else if (e.getErrorCode() == KmcCryptoErrorCode.INVALID_INPUT_VALUE) {
                String msg = "keyRef does not exist: " + keyRef;
                failureResponse(response, HttpServletResponse.SC_BAD_REQUEST, msg);
            } else {
                String msg = "CryptoKeyService: Exception retrieving key " + keyRef + ": " + e;
                failureResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            }
            return null;
        }
        logger.info("CryptoKeyService: retrieved key {} of algorithm {}", keyRef, kmcKey.getKeyAlgorithm());
        return kmcKey;
    }
}
