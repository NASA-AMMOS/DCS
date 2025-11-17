package gov.nasa.jpl.ammos.asec.kmc.kmcsdlsservice;

import java.io.IOException;
import java.security.Security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class KmcSdlsSecurityConfig {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${server.ssl.enabled:true}") // Default is true for production
    private boolean sslEnabled;

    @Value("${server.ssl.client-auth}")
    private String clientAuth;

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(new BouncyCastleFipsProvider(),1);
            Security.insertProviderAt(new BouncyCastleJsseProvider("fips:BCFIPS"), 2);

        }
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
}
