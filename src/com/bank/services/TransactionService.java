package com.bank.services;

import com.bank.database.DatabaseConnection;
import com.bank.models.Transaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionService - Data-access layer for the transactions table.
 *
 * Responsibilities:
 *   - Record new transactions (insert)
 *   - Retrieve transaction history for an account
 *   - Generate unique transaction IDs
 */
public class TransactionService {

    // ── SQL statements ────────────────────────────────────────────────────────

    private static final String INSERT_TRANSACTION =
        "INSERT INTO transactions " +
        "(transaction_id, account_number, transaction_type, amount, balance_after, " +
        " description, reference_account) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    /** Fetch all transactions for an account, newest first. */
    private static final String SELECT_ALL_BY_ACCOUNT =
        "SELECT transaction_id, account_number, transaction_type, amount, balance_after, " +
        "       description, reference_account, transaction_date " +
        "FROM   transactions " +
        "WHERE  account_number = ? " +
        "ORDER  BY transaction_date DESC";

    /** Fetch last N transactions. */
    private static final String SELECT_RECENT_BY_ACCOUNT =
        "SELECT transaction_id, account_number, transaction_type, amount, balance_after, " +
        "       description, reference_account, transaction_date " +
        "FROM   transactions " +
        "WHERE  account_number = ? " +
        "ORDER  BY transaction_date DESC " +
        "LIMIT  ?";

    /** Fetch transactions in a date range. */
    private static final String SELECT_BY_DATE_RANGE =
        "SELECT transaction_id, account_number, transaction_type, amount, balance_after, " +
        "       description, reference_account, transaction_date " +
        "FROM   transactions " +
        "WHERE  account_number = ? " +
        "  AND  transaction_date BETWEEN ? AND ? " +
        "ORDER  BY transaction_date DESC";

    /** Count all transactions for an account (for reporting). */
    private static final String COUNT_BY_ACCOUNT =
        "SELECT COUNT(*) FROM transactions WHERE account_number = ?";

    // ── Transaction ID generator ──────────────────────────────────────────────

    /**
     * Generates a unique transaction ID: TXN + yyyyMMddHHmmss + 3 random digits.
     * Example: TXN20240915143022947
     */
    public String generateTransactionId() {
        java.time.format.DateTimeFormatter fmt =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        int rand = (int)(Math.random() * 900) + 100;   // 3-digit suffix
        return "TXN" + LocalDateTime.now().format(fmt) + rand;
    }

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Persists a new transaction record.
     *
     * @param txn the completed Transaction to record
     * @throws SQLException on any DB error
     */
    public void recordTransaction(Transaction txn) throws SQLException {
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(INSERT_TRANSACTION)) {
            ps.setString(1, txn.getTransactionId());
            ps.setString(2, txn.getAccountNumber());
            ps.setString(3, txn.getTransactionType().name());
            ps.setBigDecimal(4, txn.getAmount());
            ps.setBigDecimal(5, txn.getBalanceAfter());
            ps.setString(6, txn.getDescription());
            // reference_account is nullable — use setNull when absent
            if (txn.getReferenceAccount() != null && !txn.getReferenceAccount().isEmpty()) {
                ps.setString(7, txn.getReferenceAccount());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }
            ps.executeUpdate();
        }
    }

    // ── Read operations ───────────────────────────────────────────────────────

    /**
     * Returns ALL transactions for an account, newest first.
     * Uses an ArrayList (Collections Framework) as the result container.
     *
     * @param accountNumber the account to query
     * @return list of Transaction objects (may be empty, never null)
     */
    public List<Transaction> getTransactionHistory(String accountNumber)
            throws SQLException {
        List<Transaction> history = new ArrayList<>();
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(SELECT_ALL_BY_ACCOUNT)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(mapTransaction(rs));
                }
            }
        }
        return history;
    }

    /**
     * Returns the most recent {@code limit} transactions for an account.
     *
     * @param accountNumber the account to query
     * @param limit         maximum rows to return
     */
    public List<Transaction> getRecentTransactions(String accountNumber, int limit)
            throws SQLException {
        List<Transaction> history = new ArrayList<>();
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(SELECT_RECENT_BY_ACCOUNT)) {
            ps.setString(1, accountNumber);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(mapTransaction(rs));
                }
            }
        }
        return history;
    }

    /**
     * Returns transactions within an inclusive date range.
     *
     * @param accountNumber the account to query
     * @param from          start of range (inclusive)
     * @param to            end of range (inclusive)
     */
    public List<Transaction> getTransactionsByDateRange(
            String accountNumber, LocalDateTime from, LocalDateTime to)
            throws SQLException {
        List<Transaction> history = new ArrayList<>();
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(SELECT_BY_DATE_RANGE)) {
            ps.setString(1, accountNumber);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(mapTransaction(rs));
                }
            }
        }
        return history;
    }

    /**
     * Returns the total number of transactions for an account.
     */
    public int getTransactionCount(String accountNumber) throws SQLException {
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(COUNT_BY_ACCOUNT)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ── ResultSet mapper ──────────────────────────────────────────────────────

    /** Maps the current ResultSet row to a Transaction object. */
    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("transaction_date");
        LocalDateTime date = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        return new Transaction(
            rs.getString("transaction_id"),
            rs.getString("account_number"),
            Transaction.TransactionType.valueOf(rs.getString("transaction_type")),
            rs.getBigDecimal("amount"),
            rs.getBigDecimal("balance_after"),
            rs.getString("description"),
            rs.getString("reference_account"),
            date
        );
    }
}
