package com.bank.exceptions;

/**
 * AuthenticationException - Thrown when login credentials (account
 * number + PIN) do not match or the account is not in ACTIVE status.
 */
public class AuthenticationException extends BankException {

    public AuthenticationException(String message) {
        super("AUTH_FAILED", message);
    }
}
