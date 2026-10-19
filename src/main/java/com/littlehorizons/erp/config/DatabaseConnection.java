package com.littlehorizons.erp.config;

import com.littlehorizons.erp.util.CustomExceptions.DataAccessException;
import com.littlehorizons.erp.util.LoggerUtility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton that centralizes JDBC connection settings.
 * Each call to {@link #getConnection()} returns a fresh connection that the caller
 * must close (use try-with-resources).
 */
public final class DatabaseConnection {

    private static final DatabaseConnection INSTANCE = new DatabaseConnection();

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() {
        AppConfig config = AppConfig.getInstance();
        this.url = config.get("db.url");
        this.user = config.get("db.user");
        String envPassword = System.getenv("LH_DB_PASSWORD");
        this.password = envPassword != null ? envPassword : config.get("db.password");
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            LoggerUtility.logError("DatabaseConnection.getConnection", e);
            throw new DataAccessException("No fue posible conectar con la base de datos. Verifique config.properties y que MySQL esté activo.", e);
        }
    }
}
