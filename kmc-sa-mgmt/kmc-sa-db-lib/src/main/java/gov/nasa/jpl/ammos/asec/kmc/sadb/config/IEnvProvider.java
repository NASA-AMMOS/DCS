package gov.nasa.jpl.ammos.asec.kmc.sadb.config;

public interface IEnvProvider {
    String getEnv(String key, String defaultValue);
    String getEnv(String key);
}
