package com.bank.exceptions;

/**
 * InvalidInputException - Thrown when user-supplied data fails
 * format or business-rule validation before a database call.
 */
public class InvalidInputException extends BankException {

    private final String fieldName;

    public InvalidInputException(String fieldName, String reason) {
        super("INVALID_INPUT", "Invalid " + fieldName + ": " + reason);
        this.fieldName = fieldName;
    }

    public String getFieldName() { return fieldName; }
}
