package gov.nasa.jpl.ammos.asec.kmc.sadb.config;

public class PropsEnvProvider extends EnvProvider {

    @Override
    public String getEnv(String key) {
        return getEnv(key, null);
    }

    @Override
    public String getEnv(String key, String defaultValue) {
        if (System.getProperty(key) != null) {
            return System.getProperty(key);
        } else {
            return super.getEnv(key, defaultValue);
        }
    }
}
