package gov.nasa.jpl.ammos.kmc.crypto.service;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.StreamSupport;

@Configuration
@PropertySource("classpath:kmc-crypto-service.properties")
public class KmcCryptoServiceConfiguration implements EnvironmentAware {

    public static final int MAX_CRYPTO_SERVICE_BYTES = 100000000;  // max bytes for service response
    static Environment springEnv;

    @Override
    public void setEnvironment(Environment environment) {
        KmcCryptoServiceConfiguration.springEnv = environment;
    }

    public KmcCryptoServiceConfiguration(){}

    @PostConstruct
    public Properties getConfiguration(){
        Properties props = new Properties();
        MutablePropertySources propSrcs = ((AbstractEnvironment) this.springEnv).getPropertySources();
        StreamSupport.stream(propSrcs.spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::<String>stream)
                .forEach(propName -> props.setProperty(propName, this.springEnv.getProperty(propName)));

        return props;
    }

}
