package gov.nasa.jpl.ammos.asec.kmc.saserver.app;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.sadb.DaoFactory;
import gov.nasa.jpl.ammos.asec.kmc.sadb.config.Config;
import gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa.SaController;
import gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa.SecAssnDeserializer;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.filters.HttpHeaderSecurityFilter;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.io.IOException;
import java.security.Security;

@SpringBootApplication(scanBasePackageClasses = SaController.class)
@Configuration
public class KmcSaMgmt extends SpringBootServletInitializer {
    static {
        // Insert the bcfips provider into the java security providers list up front, so that tomcat and friends can
        // see it.
        Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
        Security.insertProviderAt(new BouncyCastleJsseProvider("fips:BCFIPS"), 2);
    }

    private static final Logger  LOG = LoggerFactory.getLogger(KmcSaMgmt.class);
    private              IKmcDao dao;

    public static void main(String[] args) {
        System.setProperty("spring.config.name", "kmc-sa-mgmt-service");
        SpringApplication.run(KmcSaMgmt.class, args);
    }

    @Bean
    @Scope("singleton")
    public IKmcDao getDao() throws KmcException {
        //NOTE: this hardcoded path may need to be set via params to pull from $CFGPATH  in the future, evaluate the
        // config method's behavior
        Config cfg = new Config(defaultKmcCfgPath, "kmc-sa-mgmt-service.properties");
        cfg.setOverrideJvmTruststore(false);
        try {
            dao = DaoFactory.getDao(cfg);
            dao.init();
        } catch (KmcException e) {
            if (e.getCause() instanceof HibernateException) {
                LOG.error("Error starting database", e);
            }
            throw e;
        }
        return dao;
    }

    @Value("${gov.nasa.jpl.ammos.asec.kmc.saserver.app.default-config-path}")
    private String defaultKmcCfgPath; // was "/opt/ammos/kmc/etc"

    @Value("${hsts.url-pattern}")
    private String hstsUrlPattern;

    @Value("${hsts.enabled}")
    private String hstsEnabled;

    @Value("${hsts.hsts-max-age-seconds}")
    private String hstsMaxAgeSeconds;

    @Value("${hsts.hsts-include-sub-domains}")
    private String hstsIncludeSubDomains;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${server.ssl.enabled:true}") // Default is true for production
    private boolean sslEnabled;

    @Value("${server.ssl.client-auth}")
    private String clientAuth;


    @Bean
    public TomcatServletWebServerFactory tomcatServletWebServerFactory() {

        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

        if (sslEnabled) {
            factory.addConnectorCustomizers(connector -> {
                // Create custom SSL configuration
                SSLHostConfig sslHostConfig = connector.findSslHostConfigs()[0];

                // Set up optional client authentication instead of required
                sslHostConfig.setCertificateVerification("optional");
            });

            factory.addContextCustomizers(context -> {
                context.getPipeline().addValve(new CustomSSLValve(contextPath, clientAuth));
            });
        }

        return factory;
    }

    private static class CustomSSLValve extends ValveBase {

        private String contextPath;
        private String clientAuth;

        public CustomSSLValve(String contextPath, String clientAuth) {
            this.contextPath = contextPath;
            this.clientAuth = clientAuth;
        }

        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {

            if (!clientAuth.equalsIgnoreCase("NONE") && !request.getRequestURI().startsWith(contextPath + "/api" +
                    "/health")) {
                // Enforce client cert for all resources except for /health
                if (request.getConnector().getSecure()) {
                    Object cert = request.getAttribute("jakarta.servlet.request.X509Certificate");
                    if (cert == null) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("text/plain");
                        response.getWriter().write("Client certificate required\n");
                        response.getWriter().flush();
                        return;
                    }
                }
            }
            getNext().invoke(request, response);
        }
    }

    @Bean
    public FilterRegistrationBean<HttpHeaderSecurityFilter> httpHeaderSecurityFilterRegistration() {
        FilterRegistrationBean<HttpHeaderSecurityFilter> registration =
                new FilterRegistrationBean<>();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new HttpHeaderSecurityFilter());
        registration.addUrlPatterns(hstsUrlPattern);
        registration.addInitParameter("hstsEnabled", hstsEnabled);
        registration.addInitParameter("hstsMaxAgeSeconds", hstsMaxAgeSeconds);
        registration.addInitParameter("hstsIncludeSubDomains", hstsMaxAgeSeconds);

        registration.setName("httpHeaderSecurity");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(64000);
        return filter;
    }

    @Bean
    public MappingJackson2HttpMessageConverter converter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        Jackson2ObjectMapperBuilder         builder   = new Jackson2ObjectMapperBuilder();
        builder.deserializerByType(ISecAssn.class, new SecAssnDeserializer());
        converter.setObjectMapper(builder.build());
        return converter;
    }

    @PreDestroy
    public void destroy() throws KmcException {
        if (dao != null) {
            dao.close();
        }
    }
}
