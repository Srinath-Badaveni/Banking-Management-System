package com.bank.utils;

import com.bank.models.Transaction;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * FileLogger - Writes transaction and application events to rolling daily log files.
 *
 * Log directory  : logs/
 * Transaction log: logs/transactions_YYYY-MM-DD.csv
 * Application log: logs/app_YYYY-MM-DD.log
 *
 * File handling strategy:
 *   - Files are appended to (not overwritten) across sessions.
 *   - A new file is created each calendar day automatically.
 *   - CSV header is written only when the file is created fresh.
 */
public final class FileLogger {

    private static final String LOG_DIR           = "logs";
    private static final String TXN_FILE_PREFIX   = "transactions_";
    private static final String APP_FILE_PREFIX   = "app_";
    private static final String CSV_EXTENSION     = ".csv";
    private static final String LOG_EXTENSION     = ".log";

    private static final String CSV_HEADER =
        "TransactionID,AccountNumber,Type,Amount,BalanceAfter,Description,ReferenceAccount,DateTime";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** java.util.logging logger for application events. */
    private static final Logger APP_LOGGER = Logger.getLogger("BankingApp");
    private static boolean loggerInitialised = false;

    private FileLogger() {}

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Sets up the log directory and java.util.logging FileHandler.
     * Safe to call multiple times — initialises only once.
     */
    public static synchronized void init() {
        if (loggerInitialised) return;
        try {
            Path dir = Paths.get(LOG_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            // Rolling daily app log via JUL
            String appLogPath = LOG_DIR + "/" + APP_FILE_PREFIX
                                + LocalDate.now().format(DATE_FMT) + LOG_EXTENSION;
            FileHandler fh = new FileHandler(appLogPath, true);  // append mode
            fh.setFormatter(new SimpleFormatter());
            APP_LOGGER.addHandler(fh);
            APP_LOGGER.setLevel(Level.ALL);
            APP_LOGGER.setUseParentHandlers(false);   // suppress console output from JUL
            loggerInitialised = true;
        } catch (IOException e) {
            System.err.println("[FileLogger] Could not initialise log directory: " + e.getMessage());
        }
    }

    // ── Transaction logging ───────────────────────────────────────────────────

    /**
     * Appends a single Transaction record to today's CSV log file.
     * Creates the file with a CSV header if it doesn't yet exist.
     *
     * @param transaction the completed transaction to record
     */
    public static void logTransaction(Transaction transaction) {
        if (transaction == null) return;
        init();

        Path csvPath = Paths.get(LOG_DIR,
                TXN_FILE_PREFIX + LocalDate.now().format(DATE_FMT) + CSV_EXTENSION);
        boolean isNewFile = !Files.exists(csvPath);

        try (BufferedWriter writer = Files.newBufferedWriter(
                csvPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            if (isNewFile) {
                writer.write(CSV_HEADER);
                writer.newLine();
            }
            writer.write(transaction.toCsvLine());
            writer.newLine();

        } catch (IOException e) {
            System.err.println("[FileLogger] Failed to write transaction log: " + e.getMessage());
        }
    }

    // ── Application event logging ─────────────────────────────────────────────

    /** Logs an informational application event. */
    public static void logInfo(String message) {
        init();
        APP_LOGGER.info(message);
    }

    /** Logs a warning event (e.g. failed login attempt). */
    public static void logWarning(String message) {
        init();
        APP_LOGGER.warning(message);
    }

    /** Logs an error event with stack trace. */
    public static void logError(String message, Throwable cause) {
        init();
        APP_LOGGER.log(Level.SEVERE, message, cause);
    }

    /**
     * Writes a structured audit entry for sensitive operations
     * (login, logout, account creation) to the app log.
     */
    public static void logAudit(String eventType, String accountNumber, String details) {
        init();
        String entry = String.format("[AUDIT] %s | Event=%-20s | Account=%-14s | %s",
                LocalDateTime.now().format(TS_FMT),
                eventType, accountNumber, details);
        APP_LOGGER.info(entry);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Returns the path of today's transaction CSV log file.
     * Useful for displaying the log path to the user.
     */
    public static String getTodayTransactionLogPath() {
        return LOG_DIR + "/" + TXN_FILE_PREFIX
               + LocalDate.now().format(DATE_FMT) + CSV_EXTENSION;
    }
}
