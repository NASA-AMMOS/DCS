package gov.nasa.jpl.ammos.asec.kmc.sadb;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import org.h2.tools.RunScript;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class BaseH2Test {
    public static final String JDBC_H_2_MEM_TEST = "jdbc:h2:mem:test";
    public static final String SADB_USER = "sadb_user";
    public static final String PASSWORD = "";
    public static KmcDao dao;

    @BeforeClass
    public static void beforeClass() throws KmcException {
        dao = new KmcDao(SADB_USER, PASSWORD);
        dao.init();
        System.setProperty("KMC_UNIT_TEST", "true");
    }

    /**
     * Before each test, populate the sample DB
     */
    @Before
    public void beforeTest() {
        setupTc();
        setupAos();
        setupTm();
    }

    public void setupTm() {
        setupTable("/create_sadb_jpl_unit_test_security_associations_tm.sql");
    }

    public void setupAos() {
        setupTable("/create_sadb_jpl_unit_test_security_associations_aos.sql");
    }

    public void setupTc() {
        setupTable("/create_sadb_jpl_unit_test_security_associations.sql");
    }

    private void setupTable(String sqlFile) {
        try (Connection conn = DriverManager.getConnection(JDBC_H_2_MEM_TEST, SADB_USER, PASSWORD);
             Reader reader = new InputStreamReader(getClass().getResourceAsStream(sqlFile))) {
            RunScript.execute(conn, reader);
        } catch (SQLException sqlException) {
            throw new RuntimeException("Encountered unexpected SQLException while setting up unit test DB: ",
                    sqlException);
        } catch (IOException ioException) {
            throw new RuntimeException("Encountered unexpected IOException while setting up unit test DB: ",
                    ioException);
        }
    }

    public static void truncateTc() throws SQLException {
        truncateTable("security_associations");
    }

    public static void truncateTm() throws SQLException {
        truncateTable("security_associations_tm");
    }

    public static void truncateAos() throws SQLException {
        truncateTable("security_associations_aos");
    }

    private static void truncateTable(String tableName) throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_H_2_MEM_TEST, SADB_USER, PASSWORD)) {
            conn.createStatement().execute("TRUNCATE TABLE sadb.%s".formatted(tableName));
        }
    }

    public static void dropTc() throws SQLException {
        dropTable("security_associations");
    }

    public static void dropTm() throws SQLException {
        dropTable("security_associations_tm");
    }

    public static void dropAos() throws SQLException {
        dropTable("security_associations_aos");
    }

    private static void dropTable(String tableName) throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_H_2_MEM_TEST, SADB_USER, PASSWORD)) {
            conn.createStatement().execute("DROP TABLE sadb.%s".formatted(tableName));
        }
    }

    /**
     * After each test, truncate the sample DB
     *
     * @throws SQLException
     */
    @After
    public void afterTest() throws SQLException {
        truncateTc();
        truncateTm();
        truncateAos();
    }
}
