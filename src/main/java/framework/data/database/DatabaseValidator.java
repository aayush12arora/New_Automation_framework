package framework.data.database;

import framework.utilities.LoggerUtil;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Dedicated class for database validations.
 * Runs read-only queries and returns results the tests can verify.
 * Keeps all SQL/JDBC handling out of tests and page objects.
 */
public class DatabaseValidator {

    private static final Logger LOG = LoggerUtil.getLogger(DatabaseValidator.class);

    /**
     * @return the value of {@code column} from the first row of the query,
     *         or {@code null} if the query returned no rows.
     */
    public String querySingleValue(String sql, String column) {
        LOG.info("DB query (single value): {}", sql);
        try (Connection connection = DatabaseConnection.open();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getString(column) : null;
        } catch (SQLException e) {
            throw new IllegalStateException("Database query failed: " + sql, e);
        }
    }

    /** @return {@code true} if the query returns at least one row. */
    public boolean recordExists(String sql) {
        LOG.info("DB query (existence): {}", sql);
        try (Connection connection = DatabaseConnection.open();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        } catch (SQLException e) {
            throw new IllegalStateException("Database query failed: " + sql, e);
        }
    }

    /** @return number of rows returned by the query. */
    public int rowCount(String sql) {
        LOG.info("DB query (row count): {}", sql);
        try (Connection connection = DatabaseConnection.open();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            int count = 0;
            while (resultSet.next()) {
                count++;
            }
            return count;
        } catch (SQLException e) {
            throw new IllegalStateException("Database query failed: " + sql, e);
        }
    }
}
