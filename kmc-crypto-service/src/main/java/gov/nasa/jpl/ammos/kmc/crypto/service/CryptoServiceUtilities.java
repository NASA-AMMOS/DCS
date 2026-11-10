package gov.nasa.jpl.ammos.kmc.crypto.service;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;
import gov.nasa.jpl.ammos.kmc.crypto.library.KmcKeyServiceClient;
import gov.nasa.jpl.ammos.kmc.crypto.model.RootCause;
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
        return new Status(httpCode, reason, getErrorCode(cause), getRootCause(cause));
    }

    /**
     * Returns the error code of the first KMC exception in the cause chain.
     * @param cause  Exception that caused the failure, or null.
     * @return Name of the error code, or null if there is no KMC exception in the chain.
     */
    static String getErrorCode(final Throwable cause) {
        for (Throwable t : getCauseChain(cause)) {
            if (t instanceof KmcCryptoException) {
                return ((KmcCryptoException) t).getErrorCode().name();
            }
            if (t instanceof KmcCryptoManagerException) {
                return ((KmcCryptoManagerException) t).getErrorCode().name();
            }
        }
        return null;
    }

    /**
     * Returns the fine-grained root cause of the failure.
     * @param cause  Exception that caused the failure, or null.
     * @return Name of the root cause, or null if there is no exception.
     */
    static String getRootCause(final Throwable cause) {
        if (cause == null) {
            return null;
        }
        return findRootCause(cause).name();
    }

    /**
     * Maps the cause chain to a root cause.  The Java exceptions are the most specific, so they are checked
     * first, from the deepest up.  Otherwise the deepest KMC error code that has a mapping is used.
     * @param cause  Exception that caused the failure.
     * @return Root cause, UNKNOWN if none of the exceptions is known.
     */
    static RootCause findRootCause(final Throwable cause) {
        List<Throwable> chain = getCauseChain(cause);
        for (int i = chain.size() - 1; i >= 0; i--) {
            RootCause rootCause = fromJavaException(chain.get(i));
            if (rootCause != null) {
                return rootCause;
            }
        }
        for (int i = chain.size() - 1; i >= 0; i--) {
            RootCause rootCause = fromKmcException(chain.get(i));
            if (rootCause != null) {
                return rootCause;
            }
        }
        return RootCause.UNKNOWN;
    }

    private static RootCause fromJavaException(final Throwable t) {
        // AEADBadTagException is a subclass of BadPaddingException, so it must be checked first
        if (t instanceof AEADBadTagException) {
            return RootCause.AUTH_TAG_MISMATCH;
        } else if (t instanceof BadPaddingException) {
            return RootCause.BAD_PADDING;
        } else if (t instanceof IllegalBlockSizeException) {
            return RootCause.ILLEGAL_BLOCK_SIZE;
        } else if (t instanceof InvalidKeyException) {
            return RootCause.INVALID_KEY;
        } else if (t instanceof InvalidAlgorithmParameterException) {
            return RootCause.INVALID_ALGORITHM_PARAMETER;
        } else if (t instanceof NoSuchAlgorithmException || t instanceof NoSuchPaddingException) {
            return RootCause.UNSUPPORTED_ALGORITHM;
        } else if (t instanceof SocketException || t instanceof SocketTimeoutException
                || t instanceof UnknownHostException) {
            return RootCause.KMS_UNREACHABLE;
        } else if (t instanceof NumberFormatException) {
            return RootCause.INVALID_INPUT;
        }
        return null;
    }

    private static RootCause fromKmcException(final Throwable t) {
        if (t instanceof KmcCryptoException) {
            switch (((KmcCryptoException) t).getErrorCode()) {
                case INVALID_INPUT_VALUE:
                    return RootCause.INVALID_INPUT;
                case CRYPTO_KEY_ERROR:
                    return keyError(t);
                case CRYPTO_ALGORITHM_ERROR:
                    return RootCause.ALGORITHM_ERROR;
                case CRYPTO_METADATA_ERROR:
                    return RootCause.METADATA_INVALID;
                case KMS_CONNECTION_ERROR:
                    return RootCause.KMS_UNREACHABLE;
                default:
                    return null;
            }
        } else if (t instanceof KmcCryptoManagerException) {
            switch (((KmcCryptoManagerException) t).getErrorCode()) {
                case CRYPTO_KEY_ERROR:
                    return keyError(t);
                case CRYPTO_ALGORITHM_ERROR:
                    return RootCause.ALGORITHM_ERROR;
                case CONFIG_PARAMETER_VALUE_INVALID:
                    return RootCause.INVALID_PARAMETER;
                case NOT_INITIALIZED:
                case CONFIG_FILE_NOT_FOUND:
                case CONFIG_PARAMETER_NOT_FOUND:
                    return RootCause.CONFIG_ERROR;
                default:
                    // NULL_VALUE is used by the manager when wrapping other errors
                    return null;
            }
        }
        return null;
    }

    private static RootCause keyError(final Throwable t) {
        String message = t.getMessage();
        if (message != null && message.contains(KmcKeyServiceClient.NO_KEY_SOURCE_ERROR_MSG)) {
            return RootCause.NO_KEY_SOURCE;
        }
        return RootCause.KEY_ERROR;
    }

    /**
     * Returns true if the failure is caused by having no key source configured.
     * @param cause  Exception that caused the failure.
     * @return true if any message in the cause chain is the no key source error.
     */
    public static boolean isNoKeySource(final Throwable cause) {
        for (Throwable t : getCauseChain(cause)) {
            String message = t.getMessage();
            if (message != null && message.contains(KmcKeyServiceClient.NO_KEY_SOURCE_ERROR_MSG)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the exceptions in the cause chain, from the top-level exception to the root cause,
     * stopping if the chain has a cycle.
     */
    private static List<Throwable> getCauseChain(final Throwable cause) {
        List<Throwable> chain = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = cause; t != null && seen.add(t); t = t.getCause()) {
            chain.add(t);
        }
        return chain;
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
