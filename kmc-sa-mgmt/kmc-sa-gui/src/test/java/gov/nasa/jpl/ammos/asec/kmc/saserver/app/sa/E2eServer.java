package gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa;

import gov.nasa.jpl.ammos.asec.kmc.sadb.BaseH2Test;
import org.springframework.boot.SpringApplication;

/**
 * Runs the SA management controller against an empty in-memory H2 SADB for the frontend Playwright suite
 * (src/main/frontend/e2e). Not a unit test; started by e2e/start-server.sh. Arguments are passed to Spring, e.g.
 * --server.port, --server.servlet.context-path and --spring.web.resources.static-locations.
 */
public final class E2eServer {

    private E2eServer() {
    }

    public static void main(String[] args) throws Exception {
        // creates the H2 user, initializes the schema and sets DB_PASS/KMC_UNIT_TEST, as the controller tests do
        BaseH2Test.beforeClass();
        SpringApplication.run(TestingWebApp.class, args);
    }
}
