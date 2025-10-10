package gov.nasa.jpl.ammos.kmc.crypto.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManager;
import gov.nasa.jpl.ammos.kmc.crypto.KmcCryptoManagerException;

/**
 * Unit tests of KmcCryptoManager.
 *
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KmcCryptoManagerTest {

    /**
     * Test loading of kmc-crypto.cfg from its default location.
     * @throws KmcCryptoManagerException if the KmcCryptoManager cannot be created for any reason.
     * Disabled due to test cases not using the default config anymore
     */
    //@Test
    public final void testDefaultConfigDir() throws KmcCryptoManagerException {
        KmcCryptoManager manager = new KmcCryptoManager(null);
        assertEquals(KmcCryptoManager.DEFAULT_CRYPTO_CONFIG_DIR, manager.getKmcConfigDir());
    }

    /**
     * Test loading of kmc-crypto.cfg from a command-line config directory.
     * @throws KmcCryptoManagerException if the KmcCryptoManager cannot be created for any reason.
     */
    @Test
    public final void testCommandLineConfigDir() throws KmcCryptoManagerException  {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("cli-config-dir").getFile());
        String configDir = file.getAbsolutePath();
        String[] args = new String[] {
                "-" + KmcCryptoManager.CFG_KMC_CRYPTO_CONFIG_DIR + "=" + configDir
        };
        KmcCryptoManager manager = new KmcCryptoManager(args);
        assertTrue(manager.getKeyManagementServiceURI().contains("cli-config-dir-test-host.kms.example.com"));
    }

    /**
     * Test loading of kmc-crypto.cfg from KMC_HOME.  The test sets
     * KMC_HOME to the directory test-kmc-home under test resources.
     * Run this last because KMC_HOME setting affects other tests.
     * @throws Exception if the KmcCryptoManager cannot be created for any reason.
     * Disabled: java17 does not allow reflective access for the setEnv method
     */
    //@Test
    public final void zzzzKmcHome() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("test-kmc-home").getFile());
        String kmcHome = file.getAbsolutePath();
        setEnv(KmcCryptoManager.ENV_KMC_HOME, kmcHome);
        KmcCryptoManager manager = new KmcCryptoManager(null);
        assertTrue(manager.getKeyManagementServiceURI().contains("test-kmc-home-test-host.kms.example.com"));
    }

    /**
     * Test loading of kmc-crypto.cfg from CLASSPATH.  The test sets
     * KMC_HOME to a non-existing directory to cause kmc-crypto.cfg
     * to be loaded from CLASSPATH.
     * Run this last last because KMC_HOME setting affects other tests.
     * @throws Exception if the KmcCryptoManager cannot be created for any reason.
     */
    @Test
    public final void zzzzClasspathConfigDir() throws Exception {
        setEnv(KmcCryptoManager.ENV_KMC_HOME, "/non-exist");
        KmcCryptoManager manager = new KmcCryptoManager(null);
        assertTrue(manager.getKeyManagementServiceURI().contains("test-kmc-home.kms.example.com"));
    }

    /**
     * Sets an environment variable FOR THE CURRENT RUN OF THE JVM
     * Does not actually modify the system's environment variables,
     * but rather only the copy of the variables that java has taken,
     * and hence should only be used for testing purposes!
     * @param key The Name of the variable to set
     * @param value The value of the variable to set
     * WARNING: this method is broken in java17+
     */
    @SuppressWarnings("unchecked")
    private void setEnv(String var, String value) {
        Map<String, String> newEnv = new HashMap<String, String>();
        newEnv.put(var, value);
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newEnv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newEnv);
        } catch (NoSuchFieldException e) {
            try {
                Class<?>[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class<?> cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newEnv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}
