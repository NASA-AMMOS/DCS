package gov.nasa.jpl.ammos.kmc.crypto.service;

import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;

/**
 * An utility class for Crypto Service.
 *
 *
 */
public final class CryptoServiceUtilities {

    private CryptoServiceUtilities() {
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
