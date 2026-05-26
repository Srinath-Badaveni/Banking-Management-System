-- ============================================================
--  Banking Management System - MySQL Schema
--  Author: Banking System Project
--  Description: Complete database schema with tables, indexes,
--               and sample data for the Banking Management System
-- ============================================================

-- Create and select the database
CREATE DATABASE IF NOT EXISTS banking_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE banking_db;

-- ============================================================
-- TABLE: customers
-- Stores personal information about each bank customer
-- ============================================================
CREATE TABLE IF NOT EXISTS customers (
    customer_id   VARCHAR(12)  NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL,
    phone         VARCHAR(15)  NOT NULL,
    address       TEXT,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_customers    PRIMARY KEY (customer_id),
    CONSTRAINT uq_customer_email UNIQUE (email),
    CONSTRAINT uq_customer_phone UNIQUE (phone)
);

-- ============================================================
-- TABLE: accounts
-- Stores bank account details linked to customers
-- Supports SAVINGS and CURRENT account types
-- ============================================================
CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(14)    NOT NULL,
    customer_id    VARCHAR(12)    NOT NULL,
    account_type   ENUM('SAVINGS','CURRENT') NOT NULL DEFAULT 'SAVINGS',
    balance        DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    pin_hash       VARCHAR(64)    NOT NULL,   -- SHA-256 hashed 4-digit PIN
    status         ENUM('ACTIVE','INACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_accounts        PRIMARY KEY (account_number),
    CONSTRAINT fk_account_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_balance        CHECK (balance >= 0.00)
);

-- Index to speed up customer-to-account lookups
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_accounts_status   ON accounts(status);

-- ============================================================
-- TABLE: transactions
-- Complete audit trail of every financial operation
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id    VARCHAR(20)    NOT NULL,
    account_number    VARCHAR(14)    NOT NULL,
    transaction_type  ENUM(
        'DEPOSIT',
        'WITHDRAWAL',
        'TRANSFER_IN',
        'TRANSFER_OUT'
    )                              NOT NULL,
    amount            DECIMAL(15,2) NOT NULL,
    balance_after     DECIMAL(15,2) NOT NULL,
    description       VARCHAR(255),
    reference_account VARCHAR(14),             -- counter-party account for transfers
    transaction_date  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_transactions    PRIMARY KEY (transaction_id),
    CONSTRAINT fk_txn_account
        FOREIGN KEY (account_number) REFERENCES accounts(account_number)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_txn_amount     CHECK (amount > 0)
);

-- Indexes for fast history lookups
CREATE INDEX idx_txn_account  ON transactions(account_number);
CREATE INDEX idx_txn_date     ON transactions(transaction_date);
CREATE INDEX idx_txn_type     ON transactions(transaction_type);

-- ============================================================
-- VIEWS  (optional helpers)
-- ============================================================

-- Full account view joining customer name
CREATE OR REPLACE VIEW v_account_details AS
    SELECT
        a.account_number,
        c.customer_id,
        c.full_name,
        c.email,
        c.phone,
        a.account_type,
        a.balance,
        a.status,
        a.created_at
    FROM accounts  a
    JOIN customers c ON a.customer_id = c.customer_id;

-- Recent transactions view (last 30 days)
CREATE OR REPLACE VIEW v_recent_transactions AS
    SELECT *
    FROM transactions
    WHERE transaction_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    ORDER BY transaction_date DESC;

-- ============================================================
-- STORED PROCEDURE: sp_get_account_summary
-- ============================================================
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS sp_get_account_summary(IN p_account_number VARCHAR(14))
BEGIN
    SELECT
        a.account_number,
        c.full_name,
        a.account_type,
        a.balance,
        a.status,
        COUNT(t.transaction_id) AS total_transactions
    FROM accounts  a
    JOIN customers c ON a.customer_id = c.customer_id
    LEFT JOIN transactions t ON a.account_number = t.account_number
    WHERE a.account_number = p_account_number
    GROUP BY a.account_number, c.full_name, a.account_type, a.balance, a.status;
END$$
DELIMITER ;

-- ============================================================
-- SAMPLE DATA (for testing)
-- PIN for all sample accounts: 1234  (SHA-256 hash below)
-- SHA-256("1234") = 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4
-- ============================================================

INSERT INTO customers (customer_id, full_name, email, phone, address) VALUES
    ('CUST000001', 'Arjun Sharma',   'arjun.sharma@email.com',  '9876543210', '12 MG Road, Mumbai, Maharashtra'),
    ('CUST000002', 'Priya Patel',    'priya.patel@email.com',   '9812345678', '45 Brigade Road, Bengaluru, Karnataka'),
    ('CUST000003', 'Ravi Kumar',     'ravi.kumar@email.com',    '9765432109', '78 Anna Salai, Chennai, Tamil Nadu');

INSERT INTO accounts (account_number, customer_id, account_type, balance, pin_hash, status) VALUES
    ('ACC000000001', 'CUST000001', 'SAVINGS', 25000.00,
     '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'ACTIVE'),
    ('ACC000000002', 'CUST000002', 'SAVINGS', 50000.00,
     '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'ACTIVE'),
    ('ACC000000003', 'CUST000003', 'CURRENT', 100000.00,
     '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'ACTIVE');

INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, balance_after, description) VALUES
    ('TXN20240101001', 'ACC000000001', 'DEPOSIT',    25000.00, 25000.00, 'Initial deposit'),
    ('TXN20240101002', 'ACC000000002', 'DEPOSIT',    50000.00, 50000.00, 'Initial deposit'),
    ('TXN20240101003', 'ACC000000003', 'DEPOSIT',   100000.00,100000.00, 'Initial deposit');

-- ============================================================
-- END OF SCHEMA
-- ============================================================
