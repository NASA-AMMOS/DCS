package gov.nasa.jpl.ammos.kmc.crypto.service;

import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoFailures;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoRootCause;
import gov.nasa.jpl.ammos.kmc.crypto.model.Status;

/**
 * An utility class for Crypto Service.
 *
 *
 */
public final class CryptoServiceUtilities {

    private CryptoServiceUtilities() {
    }

    /**
     * Creates the status of a failed service, including the details of the exception that caused it.
     * Both the error code of the KMC exception and the fine-grained root cause are included.
     * @param httpCode  HTTP response code.
     * @param reason    Reason for failed status.
     * @param cause     Exception that caused the failure, or null.
     * @return Status of the failed service.
     */
    public static Status failureStatus(final int httpCode, final String reason, final Throwable cause) {
        KmcCryptoRootCause rootCause = KmcCryptoFailures.getRootCause(cause);
        return new Status(httpCode, reason, KmcCryptoFailures.getErrorCode(cause),
                rootCause == null ? null : rootCause.name());
    }

    /**
     * Returns true if the failure is caused by having no key source configured.
     * @param cause  Exception that caused the failure.
     * @return true if any message in the cause chain is the no key source error.
     */
    public static boolean isNoKeySource(final Throwable cause) {
        return KmcCryptoFailures.isNoKeySource(cause);
    }

    public static final void logRequestParameters(Logger logger, Logger audit, HttpServletRequest request) {
        boolean first = true;
        String name;
        String message = request.getRequestURI();
        Enumeration<String> enumeration = request.getParameterNames();
        while (enumeration.hasMoreElements()) {
            name = enumeration.nextElement();
            if (first) {
                message = message + "?";
                first = false;
            } else {
                message = message + "&";
            }
            message = message + name + "=" + request.getParameterValues(name)[0];
        }
        logger.info("HTTP Request: " + message);
        audit.info("User requested service: " + message);
    }

    /**
     * Add the crypto algorithm provider class so that the provider can be used for cryptographic functions.
     * @param providerClassName the class name of the provider.
     * @throws KmcCryptoException if provider class not found.
     */
    public static void addCryptoProvider(final String providerClassName) throws KmcCryptoException {
        Class<?> providerClass;
        try {
            providerClass = Class.forName(providerClassName);
            Provider provider = (Provider) providerClass.newInstance();
            Security.addProvider(provider);
        } catch (ClassNotFoundException e) {
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                    "Crypto algorithm provider class not found: " + providerClassName, e);
        } catch (InstantiationException e) {
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                    "Crypto algorithm provider class cannot be instantiated: " + providerClassName, e);
        } catch (IllegalAccessException e) {
            throw new KmcCryptoException(KmcCryptoErrorCode.CRYPTO_ALGORITHM_ERROR,
                    "Crypto algorithm provider class cannot be accessed: " + providerClassName, e);
        }
    }

}
