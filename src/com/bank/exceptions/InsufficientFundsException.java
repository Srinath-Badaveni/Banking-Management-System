package com.bank.exceptions;

import java.math.BigDecimal;

/**
 * InsufficientFundsException - Thrown when a withdrawal or transfer
 * amount exceeds the account's available balance.
 */
public class InsufficientFundsException extends BankException {

    private final BigDecimal requestedAmount;
    private final BigDecimal availableBalance;

    public InsufficientFundsException(BigDecimal requested, BigDecimal available) {
        super("INSUFFICIENT_FUNDS",
              String.format("Insufficient funds. Requested: ₹%.2f | Available: ₹%.2f",
                            requested, available));
        this.requestedAmount = requested;
        this.availableBalance = available;
    }

    public BigDecimal getRequestedAmount()  { return requestedAmount;  }
    public BigDecimal getAvailableBalance() { return availableBalance; }
}
