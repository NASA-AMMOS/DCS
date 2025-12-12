package gov.nasa.jpl.ammos.asec.kmc.sadb.config;

public class EnvProvider implements IEnvProvider {

    public String getEnv(String key) {
        return getEnv(key, null);
    }

    public String getEnv(String key, String defaultValue) {
        String env = System.getenv(key);
        return env == null ? defaultValue : env;
    }
}
