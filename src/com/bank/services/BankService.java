package com.bank.services;

import com.bank.database.DatabaseConnection;
import com.bank.exceptions.*;
import com.bank.models.Account;
import com.bank.models.Transaction;
import com.bank.models.Transaction.TransactionType;
import com.bank.utils.FileLogger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BankService - Core business logic layer.
 *
 * This is the single authoritative service for financial operations.
 * Every operation that touches money runs inside a JDBC transaction
 * (manual commit / rollback) to guarantee atomicity.
 *
 * Delegates data access to AccountService and TransactionService.
 */
public class BankService {

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final AccountService     accountService;
    private final TransactionService transactionService;

    public BankService() {
        this.accountService     = new AccountService();
        this.transactionService = new TransactionService();
    }

    // ── Account & Customer creation ───────────────────────────────────────────

    /**
     * Registers a new customer + account in a single atomic operation.
     *
     * @param customer      the fully-populated Customer to create
     * @param accountType   SAVINGS or CURRENT
     * @param initialDeposit must be >= minimum balance for the chosen type
     * @param pinHash       SHA-256 hash of the chosen 4-digit PIN
     * @return the newly created Account
     */
    public Account createAccount(com.bank.models.Customer customer,
                                 Account.AccountType accountType,
                                 BigDecimal initialDeposit,
                                 String pinHash)
            throws BankException, SQLException {

        // ── Business-rule validation ──────────────────────────────────────────
        BigDecimal minBalance = accountType == Account.AccountType.SAVINGS
                ? Account.MIN_SAVINGS_BALANCE : Account.MIN_CURRENT_BALANCE;

        if (initialDeposit.compareTo(minBalance) < 0) {
            throw new InvalidInputException("Initial Deposit",
                "Minimum opening balance for " + accountType +
                " account is ₹" + minBalance.toPlainString());
        }

        Connection conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);  // begin transaction

        try {
            // 1. Generate IDs
            String customerId    = accountService.generateCustomerId();
            String accountNumber = accountService.generateAccountNumber();

            customer.setCustomerId(customerId);

            // 2. Persist customer
            accountService.createCustomer(customer);

            // 3. Persist account
            Account account = new Account(accountNumber, customerId,
                                          accountType, initialDeposit, pinHash);
            accountService.createAccount(account);

            // 4. Record opening deposit transaction
            String txnId = transactionService.generateTransactionId();
            Transaction openingDeposit = new Transaction(
                txnId, accountNumber, TransactionType.DEPOSIT,
                initialDeposit, initialDeposit,
                "Account opening deposit", null, LocalDateTime.now());
            transactionService.recordTransaction(openingDeposit);

            conn.commit();

            // 5. Enrich account with customer name for display
            account.setCustomerName(customer.getFullName());

            // 6. File log
            FileLogger.logTransaction(openingDeposit);
            FileLogger.logInfo("New account created: " + accountNumber +
                               " for " + customer.getFullName());

            return account;

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    /**
     * Credits an amount to the specified account.
     *
     * @param accountNumber target account
     * @param amount        must be > 0 and <= MAX_TRANSACTION_LIMIT
     * @param description   optional memo
     * @return updated Account after deposit
     */
    public Account deposit(String accountNumber, BigDecimal amount, String description)
            throws BankException, SQLException {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount", "must be greater than zero");
        }

        Connection conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);

        try {
            // 1. Load and lock the account row
            Account account = accountService.getAccountByNumber(accountNumber);

            // 2. Status guard
            if (!account.isActive()) {
                throw new AuthenticationException("Account is not ACTIVE and cannot receive deposits.");
            }

            // 3. Calculate new balance
            BigDecimal newBalance = account.getBalance().add(amount);

            // 4. Persist balance
            accountService.updateBalance(accountNumber, newBalance);
            account.setBalance(newBalance);

            // 5. Record transaction
            String txnId = transactionService.generateTransactionId();
            String memo  = (description != null && !description.isBlank())
                           ? description : "Cash deposit";
            Transaction txn = new Transaction(txnId, accountNumber,
                TransactionType.DEPOSIT, amount, newBalance, memo, null, LocalDateTime.now());
            transactionService.recordTransaction(txn);

            conn.commit();

            // 6. File log
            FileLogger.logTransaction(txn);
            FileLogger.logInfo("Deposit: acc=" + accountNumber + " amt=" + amount);

            return account;

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Withdrawal ────────────────────────────────────────────────────────────

    /**
     * Debits an amount from the specified account.
     *
     * Rules:
     *   - Balance after withdrawal must remain >= minimum balance for account type.
     *   - Single withdrawal <= MAX_TRANSACTION_LIMIT.
     *
     * @param accountNumber source account
     * @param amount        amount to withdraw
     * @param description   optional memo
     * @return updated Account after withdrawal
     */
    public Account withdraw(String accountNumber, BigDecimal amount, String description)
            throws BankException, SQLException {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount", "must be greater than zero");
        }

        Connection conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);

        try {
            Account account = accountService.getAccountByNumber(accountNumber);

            if (!account.isActive()) {
                throw new AuthenticationException("Account is not ACTIVE and cannot process withdrawals.");
            }

            // Minimum balance check
            BigDecimal minBalance  = account.getMinimumBalance();
            BigDecimal newBalance  = account.getBalance().subtract(amount);

            if (newBalance.compareTo(minBalance) < 0) {
                // Tell the user exactly how much they can withdraw
                throw new InsufficientFundsException(amount, account.getWithdrawableAmount());
            }

            // Persist
            accountService.updateBalance(accountNumber, newBalance);
            account.setBalance(newBalance);

            String txnId = transactionService.generateTransactionId();
            String memo  = (description != null && !description.isBlank())
                           ? description : "Cash withdrawal";
            Transaction txn = new Transaction(txnId, accountNumber,
                TransactionType.WITHDRAWAL, amount, newBalance, memo, null, LocalDateTime.now());
            transactionService.recordTransaction(txn);

            conn.commit();

            FileLogger.logTransaction(txn);
            FileLogger.logInfo("Withdrawal: acc=" + accountNumber + " amt=" + amount);

            return account;

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Fund Transfer ─────────────────────────────────────────────────────────

    /**
     * Transfers funds between two accounts atomically.
     *
     * Both the debit (TRANSFER_OUT) and credit (TRANSFER_IN) rows are
     * inserted in the same DB transaction so the ledger never goes out of sync.
     *
     * @param fromAccount source account number
     * @param toAccount   destination account number
     * @param amount      amount to transfer
     * @param description optional memo
     * @return updated source Account after transfer
     */
    public Account transfer(String fromAccount, String toAccount,
                            BigDecimal amount, String description)
            throws BankException, SQLException {

        if (fromAccount.equalsIgnoreCase(toAccount)) {
            throw new InvalidInputException("Account",
                "Source and destination accounts must be different");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount", "must be greater than zero");
        }

        Connection conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);

        try {
            // 1. Load both accounts (verify destination exists before debiting source)
            Account source = accountService.getAccountByNumber(fromAccount);
            Account dest   = accountService.getAccountByNumber(toAccount);

            if (!source.isActive()) {
                throw new AuthenticationException("Source account is not ACTIVE.");
            }
            if (!dest.isActive()) {
                throw new AuthenticationException("Destination account " + toAccount + " is not ACTIVE.");
            }

            // 2. Minimum balance check on source
            BigDecimal srcNewBalance  = source.getBalance().subtract(amount);
            if (srcNewBalance.compareTo(source.getMinimumBalance()) < 0) {
                throw new InsufficientFundsException(amount, source.getWithdrawableAmount());
            }

            BigDecimal destNewBalance = dest.getBalance().add(amount);
            String memo = (description != null && !description.isBlank())
                          ? description : "Fund transfer";

            // 3. Update balances
            accountService.updateBalance(fromAccount, srcNewBalance);
            accountService.updateBalance(toAccount,   destNewBalance);

            source.setBalance(srcNewBalance);
            dest.setBalance(destNewBalance);

            // 4. Record TRANSFER_OUT for source
            String outTxnId = transactionService.generateTransactionId();
            Transaction outTxn = new Transaction(
                outTxnId, fromAccount, TransactionType.TRANSFER_OUT,
                amount, srcNewBalance,
                memo + " → " + toAccount, toAccount, LocalDateTime.now());
            transactionService.recordTransaction(outTxn);

            // 5. Record TRANSFER_IN for destination
            String inTxnId = transactionService.generateTransactionId();
            Transaction inTxn = new Transaction(
                inTxnId, toAccount, TransactionType.TRANSFER_IN,
                amount, destNewBalance,
                memo + " ← " + fromAccount, fromAccount, LocalDateTime.now());
            transactionService.recordTransaction(inTxn);

            conn.commit();

            // 6. File logs for both sides
            FileLogger.logTransaction(outTxn);
            FileLogger.logTransaction(inTxn);
            FileLogger.logInfo("Transfer: " + fromAccount + " → " + toAccount +
                               " amt=" + amount);

            return source;

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Balance enquiry ───────────────────────────────────────────────────────

    /**
     * Retrieves the current balance of an account.
     *
     * @param accountNumber the account to query
     * @return the Account object (with latest balance)
     */
    public Account checkBalance(String accountNumber)
            throws BankException, SQLException {
        return accountService.getAccountByNumber(accountNumber);
    }

    // ── Transaction history ───────────────────────────────────────────────────

    /** Returns all transactions for an account, newest first. */
    public List<Transaction> getTransactionHistory(String accountNumber)
            throws SQLException {
        return transactionService.getTransactionHistory(accountNumber);
    }

    /** Returns only the most recent {@code limit} transactions. */
    public List<Transaction> getRecentTransactions(String accountNumber, int limit)
            throws SQLException {
        return transactionService.getRecentTransactions(accountNumber, limit);
    }

    // ── Accessors for sub-services ────────────────────────────────────────────

    public AccountService     getAccountService()     { return accountService;     }
    public TransactionService getTransactionService() { return transactionService; }
}
