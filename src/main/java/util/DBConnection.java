package util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central JDBC connection factory.
 *
 * <p>Reads {@code db.properties} from the classpath (WEB-INF/classes) once, when the class
 * is first loaded. Values can be overridden by the environment variables
 * {@code DB_URL}, {@code DB_USER} and {@code DB_PASSWORD}, so credentials never need to be
 * hard-coded or exposed to the frontend.</p>
 */
public final class DBConnection {

    private static final Logger LOG = Logger.getLogger(DBConnection.class.getName());

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found on the classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        URL = envOr("DB_URL", props.getProperty("db.url"));
        USER = envOr("DB_USER", props.getProperty("db.user"));
        PASSWORD = envOr("DB_PASSWORD", props.getProperty("db.password"));

        try {
            // Explicit driver registration - required inside some servlet containers.
            Class.forName(props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {
    }

    /** Returns the environment variable if set, otherwise the fallback value. */
    private static String envOr(String envName, String fallback) {
        String value = System.getenv(envName);
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    /**
     * Opens a new JDBC connection. Callers must close it (use try-with-resources).
     *
     * @return a new, auto-commit connection to MySQL
     * @throws SQLException if MySQL is unreachable or credentials are wrong
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Rolls back a transaction, logging (not throwing) any secondary error. */
    public static void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Rollback failed", e);
        }
    }
}
