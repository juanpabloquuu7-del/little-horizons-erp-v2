package com.littlehorizons.erp.util;

import com.littlehorizons.erp.config.AppConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized error logger. Appends timestamped entries with stack traces to the configured log file.
 */
public final class LoggerUtility {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LoggerUtility() {
    }

    public static synchronized void logError(String context, Throwable error) {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        String entry = """
                [%s] ERROR en %s: %s
                %s
                ----------------------------------------
                """.formatted(LocalDateTime.now().format(TIMESTAMP), context, error.getMessage(), trace);
        try {
            Path logFile = Path.of(AppConfig.getInstance().get("paths.logFile", "logs/error_log.txt"));
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            Files.writeString(logFile, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ioe) {
            System.err.println("No se pudo escribir el log: " + ioe.getMessage());
        }
    }
}
