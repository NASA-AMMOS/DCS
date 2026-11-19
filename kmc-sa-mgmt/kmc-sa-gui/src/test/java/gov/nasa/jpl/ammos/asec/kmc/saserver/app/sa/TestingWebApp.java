package gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.sadb.KmcDao;
import gov.nasa.jpl.ammos.asec.kmc.sadb.config.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.module.SimpleModule;

@SpringBootApplication
public class TestingWebApp {
    public static void main(String[] args) {
        SpringApplication.run(TestingWebApp.class, args);
    }

    @Bean
    public IKmcDao getDao() throws KmcException {
        Config config = new Config("/", "kmc-sa-mgmt-service.properties");
        // password is random and provided by BaseH2Test
        KmcDao dao    = new KmcDao("sadb_user", System.getProperty("DB_PASS"));
        dao.init();
        return dao;
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperCustomizer() {
        return builder -> builder
                .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .addModule(new SimpleModule().addDeserializer(ISecAssn.class, new SecAssnDeserializer()));
    }

}
