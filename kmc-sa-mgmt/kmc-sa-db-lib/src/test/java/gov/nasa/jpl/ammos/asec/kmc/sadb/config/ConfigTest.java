package gov.nasa.jpl.ammos.asec.kmc.sadb.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Basic Config test
 *
 */
public class ConfigTest {

    @Test
    public void testCreate() {
        Config.setSysOverridesEnabled(false);
        Config cfg = new Config("/tmp", "kmc-sa-mgmt.properties");
        assertEquals("sadb_user", cfg.getUser());
        assertEquals("", cfg.getPass());
        assertEquals("jdbc:h2:mem:test;MODE=mysql;INIT=RUNSCRIPT FROM 'classpath:create_sadb.sql'\\;", cfg.getConn());
        assertEquals("localhost", cfg.getHost());
        assertEquals("3306", cfg.getPort());
        assertEquals("sadb", cfg.getSchema());
        assertEquals("none", cfg.getKeystore());
        assertEquals("none", cfg.getKeystorePass());
        assertEquals("none", cfg.getTruststore());
        assertEquals("none", cfg.getTruststorePass());
        assertFalse(cfg.getUseTls());
        assertFalse(cfg.getUseMtls());
    }
}