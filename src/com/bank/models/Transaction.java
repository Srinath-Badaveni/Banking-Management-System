package com.bank.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Transaction - Represents a single financial operation on an account.
 *
 * Maps directly to the `transactions` table.
 * Immutable by design — a recorded transaction should never be mutated.
 */
public class Transaction {

    // ── Enum ──────────────────────────────────────────────────────────────────
    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER_IN,
        TRANSFER_OUT;

        /** Returns a friendly display label. */
        public String label() {
            switch (this) {
                case DEPOSIT:       return "Deposit      (+)";
                case WITHDRAWAL:    return "Withdrawal   (-)";
                case TRANSFER_IN:   return "Transfer In  (+)";
                case TRANSFER_OUT:  return "Transfer Out (-)";
                default:            return name();
            }
        }

        /** True for credits (money coming in). */
        public boolean isCredit() {
            return this == DEPOSIT || this == TRANSFER_IN;
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private final String          transactionId;
    private final String          accountNumber;
    private final TransactionType transactionType;
    private final BigDecimal      amount;
    private final BigDecimal      balanceAfter;
    private final String          description;
    private final String          referenceAccount;   // only for transfers
    private final LocalDateTime   transactionDate;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    // ── Constructor ───────────────────────────────────────────────────────────

    public Transaction(String transactionId, String accountNumber,
                       TransactionType transactionType, BigDecimal amount,
                       BigDecimal balanceAfter, String description,
                       String referenceAccount, LocalDateTime transactionDate) {
        this.transactionId    = transactionId;
        this.accountNumber    = accountNumber;
        this.transactionType  = transactionType;
        this.amount           = amount;
        this.balanceAfter     = balanceAfter;
        this.description      = description;
        this.referenceAccount = referenceAccount;
        this.transactionDate  = transactionDate;
    }

    // ── Getters (no setters — immutable) ──────────────────────────────────────

    public String          getTransactionId()    { return transactionId;    }
    public String          getAccountNumber()    { return accountNumber;    }
    public TransactionType getTransactionType()  { return transactionType;  }
    public BigDecimal      getAmount()           { return amount;           }
    public BigDecimal      getBalanceAfter()     { return balanceAfter;     }
    public String          getDescription()      { return description;      }
    public String          getReferenceAccount() { return referenceAccount; }
    public LocalDateTime   getTransactionDate()  { return transactionDate;  }

    // ── Display ───────────────────────────────────────────────────────────────

    /**
     * Returns one table row (padded) for use in the transaction history view.
     * Format: TxnID | Date | Type | Amount | Balance | Ref
     */
    public String toTableRow() {
        String ref  = (referenceAccount != null && !referenceAccount.isEmpty())
                      ? referenceAccount : "-";
        String sign = transactionType.isCredit() ? "+" : "-";

        return String.format("| %-17s | %-20s | %-18s | %s₹%,12.2f | ₹%,12.2f | %-14s |",
            transactionId,
            transactionDate.format(FMT),
            transactionType.label(),
            sign,
            amount,
            balanceAfter,
            ref
        );
    }

    /** CSV line for file logging. */
    public String toCsvLine() {
        return String.join(",",
            transactionId,
            accountNumber,
            transactionType.name(),
            amount.toPlainString(),
            balanceAfter.toPlainString(),
            description != null ? description : "",
            referenceAccount != null ? referenceAccount : "",
            transactionDate.format(FMT)
        );
    }

    @Override
    public String toString() {
        return "Transaction{id='" + transactionId + "', type=" + transactionType +
               ", amount=" + amount + ", date=" + transactionDate.format(FMT) + "}";
    }
}
