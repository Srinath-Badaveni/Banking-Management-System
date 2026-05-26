package com.bank.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Account - Represents a bank account.
 *
 * Maps directly to the `accounts` table.
 * Uses BigDecimal for monetary values to avoid floating-point errors.
 */
public class Account {

    // ── Enums ─────────────────────────────────────────────────────────────────
    public enum AccountType   { SAVINGS, CURRENT }
    public enum AccountStatus { ACTIVE, INACTIVE, BLOCKED }

    // ── Constants ─────────────────────────────────────────────────────────────
    /** Minimum balance required for SAVINGS accounts. */
    public static final BigDecimal MIN_SAVINGS_BALANCE = new BigDecimal("500.00");
    /** Minimum balance required for CURRENT accounts. */
    public static final BigDecimal MIN_CURRENT_BALANCE = new BigDecimal("1000.00");
    /** Maximum single transaction limit. */
    public static final BigDecimal MAX_TRANSACTION_LIMIT = new BigDecimal("100000.00");

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    // ── Fields ────────────────────────────────────────────────────────────────
    private String        accountNumber;
    private String        customerId;
    private AccountType   accountType;
    private BigDecimal    balance;
    private String        pinHash;          // SHA-256 of the raw 4-digit PIN
    private AccountStatus status;
    private LocalDateTime createdAt;

    // Optional: customer name for display (populated via JOIN, not stored here)
    private String customerName;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Create a new account before DB insert. */
    public Account(String accountNumber, String customerId,
                   AccountType accountType, BigDecimal initialBalance,
                   String pinHash) {
        this.accountNumber = accountNumber;
        this.customerId    = customerId;
        this.accountType   = accountType;
        this.balance       = initialBalance;
        this.pinHash       = pinHash;
        this.status        = AccountStatus.ACTIVE;
        this.createdAt     = LocalDateTime.now();
    }

    /** Full constructor — used when hydrating from a ResultSet. */
    public Account(String accountNumber, String customerId,
                   AccountType accountType, BigDecimal balance,
                   String pinHash, AccountStatus status,
                   LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.customerId    = customerId;
        this.accountType   = accountType;
        this.balance       = balance;
        this.pinHash       = pinHash;
        this.status        = status;
        this.createdAt     = createdAt;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String        getAccountNumber() { return accountNumber; }
    public String        getCustomerId()    { return customerId;    }
    public AccountType   getAccountType()   { return accountType;   }
    public BigDecimal    getBalance()       { return balance;       }
    public String        getPinHash()       { return pinHash;       }
    public AccountStatus getStatus()        { return status;        }
    public LocalDateTime getCreatedAt()     { return createdAt;     }
    public String        getCustomerName()  { return customerName;  }

    public void setBalance(BigDecimal balance)         { this.balance      = balance;  }
    public void setStatus(AccountStatus status)        { this.status       = status;   }
    public void setCustomerName(String customerName)   { this.customerName = customerName; }

    // ── Business helpers ──────────────────────────────────────────────────────

    /** Returns true only when the account can process transactions. */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /** Minimum balance rule depending on account type. */
    public BigDecimal getMinimumBalance() {
        return accountType == AccountType.SAVINGS
               ? MIN_SAVINGS_BALANCE : MIN_CURRENT_BALANCE;
    }

    /**
     * Returns the maximum amount that can be withdrawn right now,
     * keeping the account above its minimum balance.
     */
    public BigDecimal getWithdrawableAmount() {
        return balance.subtract(getMinimumBalance());
    }

    // ── Display ───────────────────────────────────────────────────────────────

    /** Returns a formatted account summary card. */
    public String getSummary() {
        return String.format(
            "%-22s : %s%n" +
            "%-22s : %s%n" +
            "%-22s : %s%n" +
            "%-22s : ₹%,.2f%n" +
            "%-22s : %s%n" +
            "%-22s : %s",
            "Account Number",   accountNumber,
            "Account Holder",   customerName != null ? customerName : customerId,
            "Account Type",     accountType,
            "Current Balance",  balance,
            "Status",           status,
            "Opened On",        createdAt != null ? createdAt.format(FMT) : "N/A"
        );
    }

    @Override
    public String toString() {
        return "Account{number='" + accountNumber + "', type=" + accountType +
               ", balance=" + balance + ", status=" + status + "}";
    }
}
