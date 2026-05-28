package com.bank.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection - Thread-safe singleton that manages the JDBC connection.
 *
 * Configuration is read from constants; in production these would be loaded
 * from a properties file or environment variables.
 *
 * Pattern: Singleton with lazy initialization.
 */
public class DatabaseConnection {

    // ── Configuration ─────────────────────────────────────────────────────────
    private static final String DB_HOST = getEnvOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = getEnvOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = getEnvOrDefault("DB_NAME", "banking_db");
    private static final String DB_USER = getEnvOrDefault("DB_USER", "root");
    private static final String DB_PASSWORD = getEnvOrDefault("DB_PASSWORD", "root123");
    private static final String DB_SSL = getEnvOrDefault("DB_SSL", "false");

    private static final String DB_URL = "jdbc:mysql://localhost:3306/banking_db?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true&autoReconnect=true";

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null && !val.trim().isEmpty() ? val.trim() : defaultValue;
    }

    // ── Singleton state ───────────────────────────────────────────────────────
    private static Connection connection = null;

    /** Private constructor prevents direct instantiation. */
    private DatabaseConnection() {
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the shared JDBC Connection, creating it if necessary.
     *
     * @throws SQLException when the driver is missing or the server is unreachable.
     */
    public static synchronized Connection getConnection() throws SQLException {
        try {
            // Re-create connection if it was never opened or has been closed/timed-out
            if (connection == null || connection.isClosed()) {
                Class.forName(DRIVER_CLASS);
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                connection.setAutoCommit(true); // explicit transactions managed per service
                System.out.println("[DB] Connection established successfully.");
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL JDBC driver not found on classpath. " +
                            "Add mysql-connector-java-*.jar to your build path.\n" +
                            "Cause: " + e.getMessage(),
                    e);
        }
        return connection;
    }

    /**
     * Closes the shared connection gracefully.
     * Call this during application shutdown.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("[DB] Connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("[DB] Warning: could not close connection — " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Rolls back the current transaction silently.
     * Used in catch blocks to undo partial operations.
     */
    public static void rollback() {
        try {
            if (connection != null && !connection.isClosed()
                    && !connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Rollback failed: " + e.getMessage());
        }
    }

    /**
     * Quick connectivity check — useful at startup.
     *
     * @return true when the database is reachable.
     */
    public static boolean testConnection() {
        try {
            Connection c = getConnection();
            return c != null && !c.isClosed() && c.isValid(3);
        } catch (SQLException e) {
            System.err.println("[DB] Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
