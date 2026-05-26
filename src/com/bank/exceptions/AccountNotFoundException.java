package com.bank.exceptions;

/**
 * AccountNotFoundException - Thrown when a lookup for an account
 * number returns no result from the database.
 */
public class AccountNotFoundException extends BankException {

    private final String accountNumber;

    public AccountNotFoundException(String accountNumber) {
        super("ACCOUNT_NOT_FOUND",
              "Account not found: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() { return accountNumber; }
}
