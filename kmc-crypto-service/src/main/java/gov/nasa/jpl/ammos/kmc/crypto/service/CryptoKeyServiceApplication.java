package gov.nasa.jpl.ammos.kmc.crypto.service;

import java.io.IOException;
import java.security.Security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.filters.HttpHeaderSecurityFilter;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
/**
 * The Spring Boot initializer starts up the application.
 *
 *
 */

@SpringBootApplication
@ServletComponentScan
public class CryptoKeyServiceApplication extends SpringBootServletInitializer {
	{
        //Insert the bcfips provider into the java security providers list up front, so that tomcat and friends can see it.
		Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
		Security.insertProviderAt(new BouncyCastleJsseProvider("fips:BCFIPS"), 2);

	}


    private static final Logger LOG = LoggerFactory.getLogger(CryptoKeyServiceApplication.class);
    

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
	public FilterRegistrationBean<HttpHeaderSecurityFilter> httpHeaderSecurityFilterRegistration() {
		FilterRegistrationBean<HttpHeaderSecurityFilter> registration = new FilterRegistrationBean<>();
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

            if (!clientAuth.equalsIgnoreCase("NONE") && !request.getRequestURI().startsWith(contextPath + "/health")) {
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

  /*   @Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return customizerBuilder(builder);
	}

	public static void main(String[] args) {
		customizerBuilder(new SpringApplicationBuilder()).run(args);
	}

	private static SpringApplicationBuilder customizerBuilder(SpringApplicationBuilder builder) {
		return builder.sources(CryptoKeyServiceApplication.class).bannerMode(Banner.Mode.OFF);
	}
		*/

    public static void main(String[] args) {
		System.setProperty("spring.config.name","kmc-crypto-service");
        SpringApplication.run(CryptoKeyServiceApplication.class, args);
    }
}