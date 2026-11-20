package gov.nasa.jpl.ammos.kmc.crypto;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Explains the failure of a cryptographic operation: the error code of the KMC exception
 * and the fine-grained root cause, found from the exception's cause chain.
 *
 *
 */
public final class KmcCryptoFailures {

    /**
     * Message of the key error raised when neither a keystore nor a KMS is configured.
     */
    public static final String NO_KEY_SOURCE_ERROR_MSG = "Neither keystore nor KMS is defined in kmc-crypto.cfg";

    private KmcCryptoFailures() {
    }

    /**
     * Returns the error code of the first KMC exception in the cause chain.
     * @param cause  Exception that caused the failure, or null.
     * @return Name of the error code, or null if there is no KMC exception in the chain.
     */
    public static String getErrorCode(final Throwable cause) {
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
     * Returns the fine-grained root cause of the failure.  The Java exceptions are the most specific, so they
     * are checked first, from the deepest up.  Otherwise the deepest KMC error code that has a mapping is used.
     * @param cause  Exception that caused the failure, or null.
     * @return Root cause, UNKNOWN if none of the exceptions is known, or null if there is no exception.
     */
    public static KmcCryptoRootCause getRootCause(final Throwable cause) {
        if (cause == null) {
            return null;
        }
        List<Throwable> chain = getCauseChain(cause);
        for (int i = chain.size() - 1; i >= 0; i--) {
            KmcCryptoRootCause rootCause = fromJavaException(chain.get(i));
            if (rootCause != null) {
                return rootCause;
            }
        }
        for (int i = chain.size() - 1; i >= 0; i--) {
            KmcCryptoRootCause rootCause = fromKmcException(chain.get(i));
            if (rootCause != null) {
                return rootCause;
            }
        }
        return KmcCryptoRootCause.UNKNOWN;
    }

    /**
     * Returns true if the failure is caused by having no key source configured.
     * @param cause  Exception that caused the failure, or null.
     * @return true if any message in the cause chain is the no key source error.
     */
    public static boolean isNoKeySource(final Throwable cause) {
        for (Throwable t : getCauseChain(cause)) {
            if (hasNoKeySourceMessage(t)) {
                return true;
            }
        }
        return false;
    }

    private static KmcCryptoRootCause fromJavaException(final Throwable t) {
        // AEADBadTagException is a subclass of BadPaddingException, so it must be checked first
        if (t instanceof AEADBadTagException) {
            return KmcCryptoRootCause.AUTH_TAG_MISMATCH;
        } else if (t instanceof BadPaddingException) {
            return KmcCryptoRootCause.BAD_PADDING;
        } else if (t instanceof IllegalBlockSizeException) {
            return KmcCryptoRootCause.ILLEGAL_BLOCK_SIZE;
        } else if (t instanceof InvalidKeyException) {
            return KmcCryptoRootCause.INVALID_KEY;
        } else if (t instanceof InvalidAlgorithmParameterException) {
            return KmcCryptoRootCause.INVALID_ALGORITHM_PARAMETER;
        } else if (t instanceof NoSuchAlgorithmException || t instanceof NoSuchPaddingException) {
            return KmcCryptoRootCause.UNSUPPORTED_ALGORITHM;
        } else if (t instanceof SocketException || t instanceof SocketTimeoutException
                || t instanceof UnknownHostException) {
            return KmcCryptoRootCause.KMS_UNREACHABLE;
        } else if (t instanceof NumberFormatException) {
            return KmcCryptoRootCause.INVALID_INPUT;
        }
        return null;
    }

    private static KmcCryptoRootCause fromKmcException(final Throwable t) {
        if (t instanceof KmcCryptoException) {
            switch (((KmcCryptoException) t).getErrorCode()) {
                case INVALID_INPUT_VALUE:
                    return KmcCryptoRootCause.INVALID_INPUT;
                case CRYPTO_KEY_ERROR:
                    return keyError(t);
                case CRYPTO_ALGORITHM_ERROR:
                    return KmcCryptoRootCause.ALGORITHM_ERROR;
                case CRYPTO_METADATA_ERROR:
                    return KmcCryptoRootCause.METADATA_INVALID;
                case KMS_CONNECTION_ERROR:
                    return KmcCryptoRootCause.KMS_UNREACHABLE;
                default:
                    return null;
            }
        } else if (t instanceof KmcCryptoManagerException) {
            switch (((KmcCryptoManagerException) t).getErrorCode()) {
                case CRYPTO_KEY_ERROR:
                    return keyError(t);
                case CRYPTO_ALGORITHM_ERROR:
                    return KmcCryptoRootCause.ALGORITHM_ERROR;
                case CONFIG_PARAMETER_VALUE_INVALID:
                    return KmcCryptoRootCause.INVALID_PARAMETER;
                case NOT_INITIALIZED:
                case CONFIG_FILE_NOT_FOUND:
                case CONFIG_PARAMETER_NOT_FOUND:
                    return KmcCryptoRootCause.CONFIG_ERROR;
                default:
                    // NULL_VALUE is used by the manager when wrapping other errors
                    return null;
            }
        }
        return null;
    }

    private static KmcCryptoRootCause keyError(final Throwable t) {
        return hasNoKeySourceMessage(t) ? KmcCryptoRootCause.NO_KEY_SOURCE : KmcCryptoRootCause.KEY_ERROR;
    }

    private static boolean hasNoKeySourceMessage(final Throwable t) {
        String message = t.getMessage();
        return message != null && message.contains(NO_KEY_SOURCE_ERROR_MSG);
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
}
