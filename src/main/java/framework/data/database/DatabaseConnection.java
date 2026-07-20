package framework.data.database;

import framework.constants.FrameworkConstants;
import framework.utilities.LoggerUtil;
import framework.utilities.PropertyManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens JDBC connections using the SQL settings from {@code framework.properties}.
 * Driver-agnostic: it relies on the standard {@link java.sql} API, so any JDBC
 * driver on the classpath (MySQL, PostgreSQL, etc.) is picked up automatically.
 */
public final class DatabaseConnection {

    private static final Logger LOG = LoggerUtil.getLogger(DatabaseConnection.class);

    private DatabaseConnection() {
        // Prevent instantiation.
    }

    /** @return a new open connection; caller is responsible for closing it. */
    public static Connection open() {
        String url = PropertyManager.get(FrameworkConstants.DB_URL);
        try {
            Connection connection = DriverManager.getConnection(
                    url,
                    PropertyManager.get(FrameworkConstants.DB_USERNAME),
                    PropertyManager.get(FrameworkConstants.DB_PASSWORD));
            LOG.info("Opened database connection to {}", url);
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to open database connection to " + url, e);
        }
    }
}
