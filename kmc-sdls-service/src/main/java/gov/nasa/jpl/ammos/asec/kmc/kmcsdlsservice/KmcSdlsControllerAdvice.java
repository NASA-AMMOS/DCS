package gov.nasa.jpl.ammos.asec.kmc.kmcsdlsservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice(basePackageClasses = KmcSdlsController.class)
public class KmcSdlsControllerAdvice extends ResponseEntityExceptionHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(KmcSdlsControllerAdvice.class);


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleKmcSdlsEngineException(
            Exception ex, WebRequest request) {

        LOG.trace(ExceptionUtils.getStackTrace(ex));
        LOG.error(ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
