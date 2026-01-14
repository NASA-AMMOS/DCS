package gov.nasa.jpl.ammos.asec.kmc.sadb.config;

public class EnvProvider implements IEnvProvider {

    @Override
    public String getEnv(String key) {
        return getEnv(key, null);
    }

    @Override
    public String getEnv(String key, String defaultValue) {
        String env = System.getenv(key);
        return env == null ? defaultValue : env;
    }
}
