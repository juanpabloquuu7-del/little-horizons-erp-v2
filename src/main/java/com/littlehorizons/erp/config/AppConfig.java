package com.littlehorizons.erp.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Centralized application configuration (Singleton).
 * Loads defaults from the classpath and lets an external config.properties override them.
 */
public final class AppConfig {

    private static final String RESOURCE_NAME = "config.properties";
    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties properties = new Properties();

    private AppConfig() {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(RESOURCE_NAME)) {
            if (in != null) {
                properties.load(in);
            }
            Path external = Path.of(RESOURCE_NAME);
            if (Files.isRegularFile(external)) {
                try (InputStream ext = Files.newInputStream(external)) {
                    properties.load(ext);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar config.properties", e);
        }
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String get(String key) {
        return properties.getProperty(key, "");
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /** Academic year used by the application: the configured one, or the current calendar year. */
    public int getAcademicYear() {
        int configured = getInt("business.academicYear", 0);
        return configured > 0 ? configured : java.time.LocalDate.now().getYear();
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, "").trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
