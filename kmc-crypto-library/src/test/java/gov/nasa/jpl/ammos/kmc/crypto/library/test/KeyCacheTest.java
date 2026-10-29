package gov.nasa.jpl.ammos.kmc.crypto.library.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Security;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import gov.nasa.jpl.ammos.kmc.crypto.Encrypter;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoException.KmcCryptoErrorCode;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;

/**
 * Unit tests for KeyCache.
 *
 *
 */
public class KeyCacheTest {
    private static final String KEYNAME_HEAD = "kmc/test/";
    private static final String KEYREF_AES128 = KEYNAME_HEAD + "AES128";
    private static final String KEYREF_AES256 = KEYNAME_HEAD + "AES256";
    private static KmcCryptoManager cryptoManager;

    @BeforeAll
    public static void setUp() throws KmcCryptoManagerException {
        cryptoManager = new KmcCryptoManager(null);
    }

    @Test
    public final void testLoadCryptoKey() throws KmcCryptoManagerException, KmcCryptoException {
        Encrypter encrypter = cryptoManager.createEncrypter(KEYREF_AES128);
        try {
            encrypter.loadCryptoKey(KEYREF_AES256);
            // KMIP encrypter should get KEYREF_AES256 from cache
            cryptoManager.createEncrypter(KEYREF_AES256);
        } catch (KmcCryptoException e) {
            // for keystore encrypter
            assertEquals(KmcCryptoErrorCode.CRYPTO_MISC_ERROR, e.getErrorCode());
            assertTrue(e.getMessage().contains("Encrypter that uses keystore does not load keys to cache."));
        }
    }

}
