package gov.nasa.jpl.ammos.kmc.crypto.service;

import java.io.IOException;

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

import gov.nasa.jpl.ammos.kmc.crypto.model.Status;

/**
 * The servlet responds 200 and a JSON message that the KMC Crypto Service is up and running.
 *
 *
 */
@WebServlet("/status")
public class CryptoServiceStatus extends HttpServlet {
    private static final long serialVersionUID = 1052192343529234784L;

    private static final Logger logger = LoggerFactory.getLogger(CryptoServiceStatus.class);
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    protected final void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("application/json");
            Status status = new Status(HttpServletResponse.SC_OK, "The KMC Crypto Service is up and running.");
            String res = gson.toJson(status);
            logger.debug("CryptoServiceResponse = {}", res);
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(res);
            out.flush();
    }

}
