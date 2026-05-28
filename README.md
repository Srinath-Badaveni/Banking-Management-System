# 🏦 Java Banking Management System

A complete **console-based Banking Management System** built with **Core Java 17**, **JDBC**, and **MySQL**. Demonstrates OOP principles, layered architecture, secure credential storage, and full ACID transaction management.

---

## 📁 Project Structure

```
banking-management-system/
├── src/
│   └── com/bank/
│       ├── main/
│       │   └── BankingApp.java          ← Entry point & menu controller
│       ├── models/
│       │   ├── Account.java             ← Account entity (SAVINGS/CURRENT)
│       │   ├── Customer.java            ← Customer entity
│       │   └── Transaction.java         ← Immutable transaction record
│       ├── database/
│       │   └── DatabaseConnection.java  ← Singleton JDBC connection manager
│       ├── services/
│       │   ├── AccountService.java      ← Account/Customer CRUD (DAO layer)
│       │   ├── TransactionService.java  ← Transaction CRUD (DAO layer)
│       │   └── BankService.java         ← Business logic + ACID operations
│       ├── utils/
│       │   ├── InputValidator.java      ← Centralised input validation
│       │   ├── ConsoleUtil.java         ← ASCII menus, tables, colours
│       │   ├── FileLogger.java          ← Rolling daily CSV + app logs
│       │   └── PasswordUtil.java        ← SHA-256 PIN hashing
│       └── exceptions/
│           ├── BankException.java               ← Base exception
│           ├── InsufficientFundsException.java  ← Balance check failure
│           ├── AccountNotFoundException.java    ← Missing account
│           ├── AuthenticationException.java     ← Login failure
│           └── InvalidInputException.java       ← Validation failure
├── sql/
│   └── schema.sql           ← Database DDL + sample data
├── logs/                    ← Created automatically at runtime
│   ├── transactions_YYYY-MM-DD.csv
│   └── app_YYYY-MM-DD.log
├── pom.xml                  ← Maven build file (Java 17, MySQL Connector 8)
└── README.md
```

---

## ⚙️ Prerequisites

| Tool | Minimum Version |
|------|----------------|
| Java (JDK) | 17+ |
| Maven | 3.8+ |
| MySQL Server | 8.0+ |

---

## 🚀 Setup & Run

### Step 1 — Clone / extract the project

```bash
cd banking-management-system
```

### Step 2 — Create the MySQL database

```bash
mysql -u root -p < sql/schema.sql
```

This creates:
- Database `banking_db`
- Tables `customers`, `accounts`, `transactions`
- Helpful views and a stored procedure
- Three sample accounts for immediate testing

### Step 3 — Configure database credentials

Edit **`src/com/bank/database/DatabaseConnection.java`**:

```java
private static final String DB_URL      = "jdbc:mysql://localhost:3306/banking_db...";
private static final String DB_USER     = "root";         // ← your MySQL user
private static final String DB_PASSWORD = "your_password"; // ← your MySQL password
```

### Step 4 — Build the project

```bash
mvn clean package
```

This produces `target/banking-system.jar` (fat JAR with MySQL connector bundled).

### Step 5 — Run the application

```bash
java -cp "bin;C:\Users\Srinath\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" com.bank.main.BankingApp
```

Or run directly with Maven:

```bash
mvn exec:java -Dexec.mainClass="com.bank.main.BankingApp"
```

---

## 🧪 Sample Test Accounts

After running `schema.sql`, three accounts are pre-loaded:

| Account Number  | Holder       | Balance     | PIN  |
|-----------------|--------------|-------------|------|
| ACC000000001    | Arjun Sharma | ₹25,000.00  | 1234 |
| ACC000000002    | Priya Patel  | ₹50,000.00  | 1234 |
| ACC000000003    | Ravi Kumar   | ₹1,00,000.00| 1234 |

---

## 📺 Sample Console Output

```
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║        🏦  JAVA BANKING MANAGEMENT SYSTEM  🏦               ║
║                  Core Java + JDBC + MySQL                    ║
║                      Version 1.0.0                           ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝

  ✔  Database connection verified.

┌─────────────────────────────────┐
│           MAIN MENU             │
├─────────────────────────────────┤
│  1. Create New Account          │
│  2. Login                       │
│  3. Exit                        │
└─────────────────────────────────┘

  Enter your choice: 2

  ── LOGIN ──────────────────────────────────────────────────

  Account Number : ACC000000001
  PIN (4 digits) : ****

  ✔  Welcome back, Arjun Sharma!

┌─────────────────────────────────────────┐
│  Account : ACC000000001                 │
│  Holder  : Arjun Sharma                 │
├─────────────────────────────────────────┤
│  BANKING MENU                           │
├─────────────────────────────────────────┤
│  1. Check Balance                       │
│  2. Deposit Money                       │
│  3. Withdraw Money                      │
│  4. Transfer Funds                      │
│  5. Transaction History                 │
│  6. Account Details                     │
│  7. Logout                              │
└─────────────────────────────────────────┘

  Enter your choice: 2

  ── DEPOSIT MONEY ──────────────────────────────────────────

  Enter deposit amount (₹) : 5000
  Description (optional)   : Salary credit

  Confirm deposit of ₹5,000.00? [Y/N] : Y

  ✔  ₹5,000.00 deposited successfully!
  New Balance : ₹30,000.00
```

### Transfer example

```
  ── TRANSFER FUNDS ─────────────────────────────────────────

  Current Balance : ₹30,000.00

  Destination Account Number : ACC000000002
  Transfer Amount (₹)        : 2500
  Description (optional)     : Rent payment

  Transfer To : Priya Patel (ACC000000002)

  Confirm transfer of ₹2,500.00 to ACC000000002? [Y/N] : Y

  ✔  ₹2,500.00 transferred to ACC000000002 successfully!
  Your New Balance : ₹27,000.00
```

### Transaction history table

```
+───────────────────+──────────────────────+────────────────────+──────────────────+────────────────+
| Transaction ID    | Date & Time          | Type               | Amount           | Balance After  |
+───────────────────+──────────────────────+────────────────────+──────────────────+────────────────+
| TXN20240915...    | 15-Sep-2024 14:30:22 | Transfer Out (-)   | -₹    2,500.00   | ₹   27,000.00  |
| TXN20240915...    | 15-Sep-2024 14:25:10 | Deposit      (+)   | +₹    5,000.00   | ₹   30,000.00  |
| TXN20240101001    | 01-Jan-2024 00:00:00 | Deposit      (+)   | +₹   25,000.00   | ₹   25,000.00  |
+───────────────────+──────────────────────+────────────────────+──────────────────+────────────────+

  Total records: 3
```

---

## 🔐 Security Features

- **PIN hashing**: 4-digit PINs stored as SHA-256 hex digests — never in plain text
- **Constant-time comparison**: `MessageDigest.isEqual()` prevents timing attacks
- **PreparedStatement**: All queries use parameterised statements — SQL injection prevention
- **Minimum balance enforcement**: Withdrawals respect per-account-type minimums
- **Account status guards**: BLOCKED/INACTIVE accounts are rejected at login

---

## 🏗️ Design Patterns & Java Features Used

| Feature | Where Used |
|---------|-----------|
| Singleton | `DatabaseConnection` |
| Layered Architecture | models → services → controller |
| ACID Transactions | `BankService` (manual commit/rollback) |
| Custom Exceptions | 5-class exception hierarchy |
| Collections Framework | `ArrayList<Transaction>` for history |
| BigDecimal | All monetary values |
| PreparedStatement | All DB operations |
| Enums | `AccountType`, `AccountStatus`, `TransactionType` |
| File Handling | `FileLogger` (java.nio, java.util.logging) |
| ANSI Console Colors | `ConsoleUtil` |

---

## 📋 Database Tables

### customers
| Column | Type | Notes |
|--------|------|-------|
| customer_id | VARCHAR(12) PK | Auto-generated CUST000001 |
| full_name | VARCHAR(100) | |
| email | VARCHAR(100) UNIQUE | |
| phone | VARCHAR(15) UNIQUE | Indian 10-digit |
| address | TEXT | |
| created_at | TIMESTAMP | |

### accounts
| Column | Type | Notes |
|--------|------|-------|
| account_number | VARCHAR(14) PK | Auto-generated ACC000000001 |
| customer_id | VARCHAR(12) FK | References customers |
| account_type | ENUM | SAVINGS / CURRENT |
| balance | DECIMAL(15,2) | Always >= min_balance |
| pin_hash | VARCHAR(64) | SHA-256 of 4-digit PIN |
| status | ENUM | ACTIVE / INACTIVE / BLOCKED |

### transactions
| Column | Type | Notes |
|--------|------|-------|
| transaction_id | VARCHAR(20) PK | TXN + timestamp + rand |
| account_number | VARCHAR(14) FK | References accounts |
| transaction_type | ENUM | DEPOSIT / WITHDRAWAL / TRANSFER_IN / TRANSFER_OUT |
| amount | DECIMAL(15,2) | Always > 0 |
| balance_after | DECIMAL(15,2) | Snapshot at transaction time |
| description | VARCHAR(255) | Optional memo |
| reference_account | VARCHAR(14) | Counter-party for transfers |
| transaction_date | TIMESTAMP | |

---

## 📝 Log Files

| File | Location | Content |
|------|----------|---------|
| Transaction CSV | `logs/transactions_YYYY-MM-DD.csv` | Every financial operation |
| Application log | `logs/app_YYYY-MM-DD.log` | Login, logout, errors, audit |

Logs roll over to a new file each calendar day automatically.

---

## 🛠️ Troubleshooting

| Error | Solution |
|-------|----------|
| `Cannot connect to the database` | Check MySQL is running; verify credentials in `DatabaseConnection.java` |
| `MySQL JDBC driver not found` | Run `mvn clean package` to bundle the connector |
| `Access denied for user` | Grant privileges: `GRANT ALL ON banking_db.* TO 'root'@'localhost';` |
| Compilation errors on Java < 17 | Update `pom.xml` source/target to `11` and replace switch expressions |

---

## 📜 License

This project is for educational purposes. Free to use and modify.
