package com.bank.utils;

import com.bank.exceptions.InvalidInputException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * InputValidator - Centralised validation utilities.
 *
 * All methods either return the sanitised value or throw
 * InvalidInputException so callers never receive bad data.
 */
public final class InputValidator {

    // ── Regex patterns ────────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[6-9]\\d{9}$");                  // Indian mobile number
    private static final Pattern NAME_PATTERN  =
            Pattern.compile("^[A-Za-z][A-Za-z .'\\-]{1,98}[A-Za-z]$");
    private static final Pattern ACC_NUM_PATTERN =
            Pattern.compile("^ACC\\d{9}$");

    /** Utility class — no instances. */
    private InputValidator() {}

    // ── String validators ─────────────────────────────────────────────────────

    /**
     * Validates a full name (2-100 chars, letters/spaces/hyphens/apostrophes).
     *
     * @return trimmed name
     */
    public static String validateName(String name) throws InvalidInputException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Name", "must not be empty");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new InvalidInputException("Name", "must be between 2 and 100 characters");
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("Name",
                "can only contain letters, spaces, hyphens, apostrophes, and dots");
        }
        return trimmed;
    }

    /**
     * Validates an email address.
     *
     * @return lower-cased, trimmed email
     */
    public static String validateEmail(String email) throws InvalidInputException {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidInputException("Email", "must not be empty");
        }
        String trimmed = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("Email", "'" + trimmed + "' is not a valid email address");
        }
        return trimmed;
    }

    /**
     * Validates a 10-digit Indian mobile number starting with 6-9.
     *
     * @return digits-only string
     */
    public static String validatePhone(String phone) throws InvalidInputException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new InvalidInputException("Phone", "must not be empty");
        }
        String digits = phone.trim().replaceAll("\\s+", "");
        if (!PHONE_PATTERN.matcher(digits).matches()) {
            throw new InvalidInputException("Phone",
                "must be a 10-digit Indian mobile number starting with 6-9");
        }
        return digits;
    }

    /**
     * Validates an account number (format: ACC + 9 digits).
     */
    public static String validateAccountNumber(String acc) throws InvalidInputException {
        if (acc == null || acc.trim().isEmpty()) {
            throw new InvalidInputException("Account Number", "must not be empty");
        }
        String trimmed = acc.trim().toUpperCase();
        if (!ACC_NUM_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("Account Number",
                "must follow the format ACC########  (ACC + 9 digits), e.g. ACC000000001");
        }
        return trimmed;
    }

    /**
     * Validates a 4-digit numeric PIN.
     *
     * @return the validated PIN string (not hashed here)
     */
    public static String validatePin(String pin) throws InvalidInputException {
        if (pin == null || pin.isEmpty()) {
            throw new InvalidInputException("PIN", "must not be empty");
        }
        if (!pin.matches("\\d{4}")) {
            throw new InvalidInputException("PIN", "must be exactly 4 digits (0-9)");
        }
        return pin;
    }

    // ── Monetary validators ───────────────────────────────────────────────────

    /**
     * Parses and validates a monetary amount string.
     * Must be a positive number with at most 2 decimal places,
     * between 1.00 and 1,00,000.00.
     */
    public static BigDecimal validateAmount(String amountStr) throws InvalidInputException {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            throw new InvalidInputException("Amount", "must not be empty");
        }
        String cleaned = amountStr.trim().replace(",", ""); // accept comma-formatted input
        BigDecimal amount;
        try {
            amount = new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Amount", "'" + amountStr + "' is not a valid number");
        }

        // Must be positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount", "must be greater than zero");
        }

        // Max 2 decimal places
        if (amount.scale() > 2) {
            throw new InvalidInputException("Amount", "must have at most 2 decimal places");
        }

        // Upper limit per transaction
        if (amount.compareTo(new BigDecimal("100000.00")) > 0) {
            throw new InvalidInputException("Amount",
                "single transaction limit is ₹1,00,000.00");
        }

        // Minimum meaningful amount
        if (amount.compareTo(new BigDecimal("1.00")) < 0) {
            throw new InvalidInputException("Amount", "minimum transaction amount is ₹1.00");
        }

        return amount;
    }

    /**
     * Parses a menu choice integer within an inclusive range.
     */
    public static int validateMenuChoice(String input, int min, int max)
            throws InvalidInputException {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidInputException("Choice", "must not be empty");
        }
        int choice;
        try {
            choice = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Choice", "'" + input + "' is not a valid number");
        }
        if (choice < min || choice > max) {
            throw new InvalidInputException("Choice",
                "must be between " + min + " and " + max);
        }
        return choice;
    }

    /**
     * Ensures a free-text address is non-null and within a reasonable length.
     */
    public static String validateAddress(String address) throws InvalidInputException {
        if (address == null || address.trim().isEmpty()) {
            throw new InvalidInputException("Address", "must not be empty");
        }
        String trimmed = address.trim();
        if (trimmed.length() > 500) {
            throw new InvalidInputException("Address", "must not exceed 500 characters");
        }
        return trimmed;
    }
}
