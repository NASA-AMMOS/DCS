package gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.sadb.KmcDao;
import gov.nasa.jpl.ammos.asec.kmc.sadb.config.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@SpringBootApplication
public class TestingWebApp {
    public static void main(String[] args) {
        SpringApplication.run(TestingWebApp.class, args);
    }

    @Bean
    public IKmcDao getDao() throws KmcException {
        Config config = new Config("/", "kmc-sa-mgmt-service.properties");
        KmcDao dao    = new KmcDao("sadb_user", "");
        dao.init();
        return dao;
    }

    @Bean
    public MappingJackson2HttpMessageConverter converter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        Jackson2ObjectMapperBuilder         builder   = new Jackson2ObjectMapperBuilder();
        builder.deserializerByType(ISecAssn.class, new SecAssnDeserializer());
        converter.setObjectMapper(builder.build());
        return converter;
    }

}
