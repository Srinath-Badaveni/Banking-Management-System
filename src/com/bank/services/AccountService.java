package com.bank.services;

import com.bank.database.DatabaseConnection;
import com.bank.exceptions.*;
import com.bank.models.Account;
import com.bank.models.Customer;
import com.bank.utils.FileLogger;
import com.bank.utils.PasswordUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * AccountService - Data-access layer for accounts and customers.
 *
 * Responsibilities:
 *   - Persist and retrieve Customer records
 *   - Persist and retrieve Account records
 *   - Authenticate users via account number + PIN
 *   - Update account balance
 *
 * All DB operations use PreparedStatement to prevent SQL injection.
 */
public class AccountService {

    // ── SQL statements ────────────────────────────────────────────────────────

    private static final String INSERT_CUSTOMER =
        "INSERT INTO customers (customer_id, full_name, email, phone, address) " +
        "VALUES (?, ?, ?, ?, ?)";

    private static final String INSERT_ACCOUNT =
        "INSERT INTO accounts (account_number, customer_id, account_type, balance, pin_hash, status) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_ACCOUNT_BY_NUMBER =
        "SELECT a.account_number, a.customer_id, a.account_type, a.balance, " +
        "       a.pin_hash, a.status, a.created_at, c.full_name " +
        "FROM   accounts a " +
        "JOIN   customers c ON a.customer_id = c.customer_id " +
        "WHERE  a.account_number = ?";

    private static final String SELECT_CUSTOMER_BY_ID =
        "SELECT customer_id, full_name, email, phone, address, created_at " +
        "FROM   customers WHERE customer_id = ?";

    private static final String UPDATE_BALANCE =
        "UPDATE accounts SET balance = ? WHERE account_number = ?";

    private static final String CHECK_EMAIL_EXISTS =
        "SELECT COUNT(*) FROM customers WHERE email = ?";

    private static final String CHECK_PHONE_EXISTS =
        "SELECT COUNT(*) FROM customers WHERE phone = ?";

    // ── ID generators ─────────────────────────────────────────────────────────

    /**
     * Generates a unique customer ID: CUST + 6-digit zero-padded sequence.
     * Uses the current max customer_id from the DB, increments by 1.
     */
    public String generateCustomerId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(customer_id, 5) AS UNSIGNED)), 0) " +
                     "FROM customers";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            long seq = rs.next() ? rs.getLong(1) + 1 : 1;
            return String.format("CUST%06d", seq);
        }
    }

    /**
     * Generates a unique account number: ACC + 9-digit zero-padded sequence.
     */
    public String generateAccountNumber() throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(account_number, 4) AS UNSIGNED)), 0) " +
                     "FROM accounts";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            long seq = rs.next() ? rs.getLong(1) + 1 : 1;
            return String.format("ACC%09d", seq);
        }
    }

    // ── Customer operations ───────────────────────────────────────────────────

    /**
     * Inserts a new customer row.
     *
     * @throws BankException if email or phone is already registered
     */
    public void createCustomer(Customer customer) throws BankException, SQLException {
        // Check for duplicate email
        if (isEmailRegistered(customer.getEmail())) {
            throw new InvalidInputException("Email",
                "'" + customer.getEmail() + "' is already registered");
        }
        // Check for duplicate phone
        if (isPhoneRegistered(customer.getPhone())) {
            throw new InvalidInputException("Phone",
                "'" + customer.getPhone() + "' is already registered");
        }

        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(INSERT_CUSTOMER)) {
            ps.setString(1, customer.getCustomerId());
            ps.setString(2, customer.getFullName());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getPhone());
            ps.setString(5, customer.getAddress());
            ps.executeUpdate();
            FileLogger.logAudit("CREATE_CUSTOMER", customer.getCustomerId(),
                                "Name=" + customer.getFullName());
        }
    }

    /**
     * Retrieves a Customer by ID.
     *
     * @throws AccountNotFoundException if no matching customer exists
     */
    public Customer getCustomerById(String customerId) throws BankException, SQLException {
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(SELECT_CUSTOMER_BY_ID)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AccountNotFoundException(customerId);
                }
                return mapCustomer(rs);
            }
        }
    }

    // ── Account operations ────────────────────────────────────────────────────

    /**
     * Inserts a new account row.
     * The PIN is already hashed before this call.
     */
    public void createAccount(Account account) throws BankException, SQLException {
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(INSERT_ACCOUNT)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getCustomerId());
            ps.setString(3, account.getAccountType().name());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getPinHash());
            ps.setString(6, account.getStatus().name());
            ps.executeUpdate();
            FileLogger.logAudit("CREATE_ACCOUNT", account.getAccountNumber(),
                                "Type=" + account.getAccountType() +
                                " | InitialBalance=" + account.getBalance());
        }
    }

    /**
     * Retrieves an Account by account number, joined with customer name.
     *
     * @throws AccountNotFoundException if the account does not exist
     */
    public Account getAccountByNumber(String accountNumber)
            throws BankException, SQLException {
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(SELECT_ACCOUNT_BY_NUMBER)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AccountNotFoundException(accountNumber);
                }
                return mapAccount(rs);
            }
        }
    }

    /**
     * Authenticates a user by account number and plain-text PIN.
     *
     * @return the authenticated Account on success
     * @throws AuthenticationException if credentials are wrong or account is not ACTIVE
     */
    public Account authenticate(String accountNumber, String pin)
            throws BankException, SQLException {
        Account account;
        try {
            account = getAccountByNumber(accountNumber);
        } catch (AccountNotFoundException e) {
            // Mask the distinction between "not found" and "wrong PIN"
            throw new AuthenticationException("Invalid account number or PIN.");
        }

        if (account.getStatus() == Account.AccountStatus.BLOCKED) {
            throw new AuthenticationException("Account " + accountNumber +
                " is BLOCKED. Please visit your nearest branch.");
        }
        if (account.getStatus() == Account.AccountStatus.INACTIVE) {
            throw new AuthenticationException("Account " + accountNumber +
                " is INACTIVE. Please contact customer support.");
        }
        if (!PasswordUtil.verify(pin, account.getPinHash())) {
            FileLogger.logWarning("Failed login attempt for account: " + accountNumber);
            throw new AuthenticationException("Invalid account number or PIN.");
        }

        FileLogger.logAudit("LOGIN", accountNumber, "Authentication successful");
        return account;
    }

    /**
     * Persists an updated balance to the database.
     * Called by BankService after a deposit / withdrawal / transfer.
     */
    public void updateBalance(String accountNumber, BigDecimal newBalance)
            throws SQLException {
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(UPDATE_BALANCE)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, accountNumber);
            ps.executeUpdate();
        }
    }

    // ── Uniqueness checks ─────────────────────────────────────────────────────

    private boolean isEmailRegistered(String email) throws SQLException {
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(CHECK_EMAIL_EXISTS)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean isPhoneRegistered(String phone) throws SQLException {
        try (PreparedStatement ps =
                 DatabaseConnection.getConnection().prepareStatement(CHECK_PHONE_EXISTS)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ── ResultSet mappers ─────────────────────────────────────────────────────

    /** Maps the current ResultSet row to an Account object. */
    private Account mapAccount(ResultSet rs) throws SQLException {
        Account account = new Account(
            rs.getString("account_number"),
            rs.getString("customer_id"),
            Account.AccountType.valueOf(rs.getString("account_type")),
            rs.getBigDecimal("balance"),
            rs.getString("pin_hash"),
            Account.AccountStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
        // Enrich with customer name (from the JOIN)
        String name = rs.getString("full_name");
        if (name != null) account.setCustomerName(name);
        return account;
    }

    /** Maps the current ResultSet row to a Customer object. */
    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        return new Customer(
            rs.getString("customer_id"),
            rs.getString("full_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("address"),
            created
        );
    }
}
