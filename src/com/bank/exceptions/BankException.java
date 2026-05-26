package com.bank.exceptions;

/**
 * BankException - Base custom exception for the Banking Management System.
 *
 * All domain-specific exceptions extend this class so callers can catch
 * either a specific sub-type or the whole hierarchy with one catch block.
 */
public class BankException extends Exception {

    private final String errorCode;

    /** General bank exception with a descriptive message. */
    public BankException(String message) {
        super(message);
        this.errorCode = "BANK_ERR";
    }

    /** Exception with an application-level error code for programmatic handling. */
    public BankException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Wraps a lower-level cause while preserving context. */
    public BankException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BANK_ERR";
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + getMessage();
    }
}
