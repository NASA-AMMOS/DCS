package gov.nasa.jpl.ammos.asec.kmc.kmcsdlsservice;

import gov.nasa.jpl.ammos.asec.kmc.SDLS_TC_TransferFrame;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.codec.binary.Hex;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *  SDLS REST Controller
 *
 */

@RestController
public class KmcSdlsController {

    private static final Logger LOG = LoggerFactory.getLogger(KmcSdlsController.class);
    private final KmcSdlsService kmcSdlsService = KmcSdlsService.getInstance();

    @RequestMapping(value = "/apply_security", method = RequestMethod.POST)
    public byte[] applySecurity(HttpServletRequest request) {
        byte[] transferFrameData = new byte[0];
        try {
            transferFrameData = IOUtils.toByteArray(request.getInputStream());
        } catch (IOException e) {
            LOG.error("/apply_security: Encountered an I/O error while attempting to " +
                    "process request Input Stream {} : {}", request, e.getMessage());
            throw new RuntimeException("Unable to read Input Stream due to error: " + e.getMessage());
        }
        String transferFrameDataString = Hex.encodeHexString(transferFrameData);
        LOG.debug("Received Transfer Frame Hex: " + transferFrameDataString);

        String cookies = buildCookiesString(request.getCookies());

        String transferFrameSDLSApplied = null;
        try {
            transferFrameSDLSApplied = this.kmcSdlsService.getKmcSdlsEngine().applySecurity(transferFrameDataString,cookies);
        } catch (Exception e) {
            LOG.error("Encountered unexpected exception while attempting applySecurity() " +
                    "on frame {} : {}", transferFrameDataString, e.getMessage());
            throw new RuntimeException("Unable to execute applySecurity() due to error: " + e.getMessage());
        }

        LOG.debug("SDLS Applied Transfer Frame Hex: " + transferFrameSDLSApplied);

        try {
            return Hex.decodeHex(transferFrameSDLSApplied);
        } catch (DecoderException e) {
            LOG.error("Encountered an error while attempting to decode HEX {} : {}",
                    transferFrameSDLSApplied, e.getMessage());
            throw new RuntimeException("Unable to decode due to error: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/process_security", method = RequestMethod.POST)
    public SDLS_TC_TransferFrame processSecurity(HttpServletRequest request) {
        byte[] sdlsTransferFrameData;
        try {
            sdlsTransferFrameData = IOUtils.toByteArray(request.getInputStream());
        } catch (IOException e) {
            LOG.error("/process_security: Encountered an I/O error while attempting to " +
                    "read request Input Stream {} : {}", request, e.getMessage());
            throw new RuntimeException("Unable to read Input Stream due to error: " + e.getMessage());
        }
        String sdlsTransferFrameDataString = Hex.encodeHexString(sdlsTransferFrameData);
        LOG.debug("Received SDLS Transfer Frame Hex: " + sdlsTransferFrameDataString);

        String cookies = buildCookiesString(request.getCookies());

        SDLS_TC_TransferFrame processSecurityResult;
        try {
            processSecurityResult = this.kmcSdlsService.getKmcSdlsEngine().processSecurity(sdlsTransferFrameDataString,cookies);
        } catch (Exception e) {
            LOG.error("Encountered unexpected error while calling processSecurity() on frame {} : {}",
                    sdlsTransferFrameDataString, e.getMessage());
            throw new RuntimeException("Unable to execute processSecurity() due to error: " + e.getMessage());
        }

        return processSecurityResult;
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public String status() {
        int engineStatus = this.kmcSdlsService.getKmcSdlsEngine().getEngineStatus();
        if(engineStatus != 0) {
            String codeEnum = this.kmcSdlsService.getKmcSdlsEngine().getCryptoLibErrorCodeEnum(engineStatus);
            LOG.error("/status : SdlsEngine returned a non-zero status code of {} - {}", engineStatus, codeEnum);
            throw new RuntimeException("SdlsEngine status error: " + codeEnum);
        }
        return this.kmcSdlsService.getKmcSdlsEngine().getCryptoLibErrorCodeEnum(engineStatus)+"\n";
    }

    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public String health() {
        // This method will simply return HTTP 200 status with the following string
        return "Service is UP\n";
    }

    private String buildCookiesString(Cookie[] cookies)
    {
        if (cookies != null) {
            String cookiesStr = Arrays.stream(cookies)
                    .map(c -> c.getName() + "=" + c.getValue())
                    .collect(Collectors.joining("; "));
            LOG.debug("Cookies String: " + cookiesStr);
            return cookiesStr;
        }
        else {
            LOG.debug("Cookies are null!");
            return null;
        }
    }
}
