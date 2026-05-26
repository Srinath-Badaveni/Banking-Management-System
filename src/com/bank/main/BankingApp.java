package com.bank.main;

import com.bank.database.DatabaseConnection;
import com.bank.exceptions.*;
import com.bank.models.Account;
import com.bank.models.Account.AccountType;
import com.bank.models.Customer;
import com.bank.models.Transaction;
import com.bank.services.BankService;
import com.bank.utils.ConsoleUtil;
import com.bank.utils.FileLogger;
import com.bank.utils.InputValidator;
import com.bank.utils.PasswordUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * BankingApp - Application entry point and menu-driven controller.
 *
 * Flow:
 *   Main Menu
 *     ├── 1. Create Account  → collects customer + account info, persists
 *     ├── 2. Login           → authenticates, then shows Banking Menu
 *     │       ├── 1. Check Balance
 *     │       ├── 2. Deposit
 *     │       ├── 3. Withdraw
 *     │       ├── 4. Transfer
 *     │       ├── 5. Transaction History
 *     │       ├── 6. Account Details
 *     │       └── 7. Logout
 *     └── 3. Exit
 */
public class BankingApp {

    // ── Application state ─────────────────────────────────────────────────────
    private static final BankService bankService = new BankService();
    private static final Scanner     scanner     = new Scanner(System.in);

    /** The account that is currently logged in (null when unauthenticated). */
    private static Account loggedInAccount = null;

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Initialise file logger
        FileLogger.init();

        // Display startup banner
        ConsoleUtil.printBanner();

        // Verify DB connectivity before accepting user input
        if (!DatabaseConnection.testConnection()) {
            ConsoleUtil.printError("Cannot connect to the database. " +
                "Please check your MySQL server and credentials in DatabaseConnection.java");
            System.exit(1);
        }
        ConsoleUtil.printSuccess("Database connection verified.");

        // Main application loop
        boolean running = true;
        while (running) {
            try {
                ConsoleUtil.printMainMenu();
                String input = scanner.nextLine().trim();
                int choice   = InputValidator.validateMenuChoice(input, 1, 3);

                switch (choice) {
                    case 1 -> handleCreateAccount();
                    case 2 -> handleLogin();
                    case 3 -> { running = false; handleExit(); }
                }
            } catch (InvalidInputException e) {
                ConsoleUtil.printError(e.getMessage());
            } catch (Exception e) {
                ConsoleUtil.printError("Unexpected error: " + e.getMessage());
                FileLogger.logError("Unhandled exception in main loop", e);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CREATE ACCOUNT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Collects all required information and creates a new customer + account.
     * Validates each field before proceeding to the next.
     */
    private static void handleCreateAccount() {
        ConsoleUtil.printSectionHeader("CREATE NEW ACCOUNT");

        try {
            // ── Customer details ───────────────────────────────────────────────
            System.out.print("  Full Name          : ");
            String name = InputValidator.validateName(scanner.nextLine());

            System.out.print("  Email Address      : ");
            String email = InputValidator.validateEmail(scanner.nextLine());

            System.out.print("  Phone Number       : ");
            String phone = InputValidator.validatePhone(scanner.nextLine());

            System.out.print("  Address            : ");
            String address = InputValidator.validateAddress(scanner.nextLine());

            // ── Account details ────────────────────────────────────────────────
            System.out.println("\n  Account Types:");
            System.out.println("    1. SAVINGS  (min balance ₹500)");
            System.out.println("    2. CURRENT  (min balance ₹1,000)");
            System.out.print("  Choose Account Type [1/2] : ");
            int typeChoice = InputValidator.validateMenuChoice(scanner.nextLine(), 1, 2);
            AccountType accountType = (typeChoice == 1) ? AccountType.SAVINGS : AccountType.CURRENT;

            BigDecimal minDeposit = accountType == AccountType.SAVINGS
                    ? Account.MIN_SAVINGS_BALANCE : Account.MIN_CURRENT_BALANCE;
            System.out.printf("  Initial Deposit (min ₹%.2f) : ", minDeposit);
            BigDecimal initialDeposit = InputValidator.validateAmount(scanner.nextLine());

            // ── PIN setup ──────────────────────────────────────────────────────
            System.out.print("  Set 4-digit PIN    : ");
            String pin = InputValidator.validatePin(scanner.nextLine());

            System.out.print("  Confirm PIN        : ");
            String confirmPin = InputValidator.validatePin(scanner.nextLine());

            if (!pin.equals(confirmPin)) {
                ConsoleUtil.printError("PINs do not match. Account creation cancelled.");
                return;
            }

            String pinHash = PasswordUtil.hash(pin);

            // ── Confirm before submitting ──────────────────────────────────────
            System.out.println();
            ConsoleUtil.printSingleLine();
            System.out.println("  Review your details:");
            System.out.println("    Name         : " + name);
            System.out.println("    Email        : " + email);
            System.out.println("    Phone        : " + phone);
            System.out.println("    Account Type : " + accountType);
            System.out.printf( "    Opening Dep  : ₹%,.2f%n", initialDeposit);
            ConsoleUtil.printSingleLine();
            System.out.print("  Confirm and create account? [Y/N] : ");
            String confirm = scanner.nextLine().trim();

            if (!confirm.equalsIgnoreCase("Y")) {
                ConsoleUtil.printWarning("Account creation cancelled.");
                return;
            }

            // ── Call service ───────────────────────────────────────────────────
            Customer customer = new Customer(null, name, email, phone, address);
            Account  created  = bankService.createAccount(customer, accountType,
                                                          initialDeposit, pinHash);

            // ── Success display ────────────────────────────────────────────────
            ConsoleUtil.printSuccess("Account created successfully!");
            System.out.println();
            ConsoleUtil.printDoubleLine();
            System.out.println("  *** SAVE THESE DETAILS SECURELY ***");
            ConsoleUtil.printDoubleLine();
            System.out.println(created.getSummary());
            ConsoleUtil.printDoubleLine();
            ConsoleUtil.printWarning("Your account number is: " + created.getAccountNumber());

        } catch (InvalidInputException e) {
            ConsoleUtil.printError("Validation error — " + e.getMessage());
        } catch (BankException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (SQLException e) {
            ConsoleUtil.printError("Database error: " + e.getMessage());
            FileLogger.logError("SQLException during account creation", e);
        }

        ConsoleUtil.pressEnterToContinue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LOGIN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Authenticates user with account number + PIN.
     * On success, enters the inner Banking Menu loop.
     */
    private static void handleLogin() {
        ConsoleUtil.printSectionHeader("LOGIN");

        try {
            System.out.print("  Account Number : ");
            String accountNumber = scanner.nextLine().trim().toUpperCase();

            System.out.print("  PIN (4 digits) : ");
            String pin = scanner.nextLine().trim();

            // Authenticate — throws AuthenticationException on failure
            loggedInAccount = bankService.getAccountService()
                                         .authenticate(accountNumber, pin);
            loggedInAccount = bankService.checkBalance(accountNumber); // refresh with name

            ConsoleUtil.printSuccess("Welcome back, " + loggedInAccount.getCustomerName() + "!");
            FileLogger.logAudit("LOGIN_SUCCESS", accountNumber,
                                "User=" + loggedInAccount.getCustomerName());

            // Enter the authenticated banking menu loop
            runBankingMenu();

        } catch (AuthenticationException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (BankException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (SQLException e) {
            ConsoleUtil.printError("Database error during login: " + e.getMessage());
            FileLogger.logError("SQLException during login", e);
        } finally {
            loggedInAccount = null; // always clear session on exit
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BANKING MENU (authenticated session)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Inner menu loop — runs while the user is logged in. */
    private static void runBankingMenu() {
        boolean sessionActive = true;

        while (sessionActive) {
            // Always show the latest balance in the menu header
            refreshLoggedInAccount();

            ConsoleUtil.printBankingMenu(
                loggedInAccount.getAccountNumber(),
                loggedInAccount.getCustomerName()
            );

            try {
                String input = scanner.nextLine().trim();
                int choice   = InputValidator.validateMenuChoice(input, 1, 7);

                switch (choice) {
                    case 1 -> handleCheckBalance();
                    case 2 -> handleDeposit();
                    case 3 -> handleWithdraw();
                    case 4 -> handleTransfer();
                    case 5 -> handleTransactionHistory();
                    case 6 -> handleAccountDetails();
                    case 7 -> { sessionActive = false; handleLogout(); }
                }
            } catch (InvalidInputException e) {
                ConsoleUtil.printError(e.getMessage());
            } catch (Exception e) {
                ConsoleUtil.printError("Unexpected error: " + e.getMessage());
                FileLogger.logError("Unhandled exception in banking menu", e);
            }
        }
    }

    // ── Silently refresh the in-memory account state ──────────────────────────

    private static void refreshLoggedInAccount() {
        if (loggedInAccount == null) return;
        try {
            loggedInAccount = bankService.checkBalance(loggedInAccount.getAccountNumber());
        } catch (Exception ignored) { /* keep stale copy if DB unreachable */ }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CHECK BALANCE
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleCheckBalance() {
        ConsoleUtil.printSectionHeader("BALANCE ENQUIRY");
        try {
            Account account = bankService.checkBalance(loggedInAccount.getAccountNumber());
            System.out.println();
            System.out.printf("  Account Number : %s%n", account.getAccountNumber());
            System.out.printf("  Account Type   : %s%n", account.getAccountType());
            ConsoleUtil.printDoubleLine();
            System.out.printf("  Available Balance  :  " +
                              ConsoleUtil.GREEN + ConsoleUtil.BOLD +
                              "₹%,.2f" + ConsoleUtil.RESET + "%n",
                              account.getBalance());
            System.out.printf("  Minimum Balance    :  ₹%,.2f%n", account.getMinimumBalance());
            System.out.printf("  Withdrawable Amt   :  ₹%,.2f%n", account.getWithdrawableAmount());
            ConsoleUtil.printDoubleLine();
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.pressEnterToContinue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DEPOSIT
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleDeposit() {
        ConsoleUtil.printSectionHeader("DEPOSIT MONEY");
        try {
            System.out.print("  Enter deposit amount (₹) : ");
            BigDecimal amount = InputValidator.validateAmount(scanner.nextLine());

            System.out.print("  Description (optional)   : ");
            String description = scanner.nextLine().trim();

            System.out.printf("%n  Confirm deposit of ₹%,.2f? [Y/N] : ", amount);
            if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
                ConsoleUtil.printWarning("Deposit cancelled."); return;
            }

            Account updated = bankService.deposit(
                loggedInAccount.getAccountNumber(), amount, description);
            loggedInAccount = updated;

            ConsoleUtil.printSuccess(String.format(
                "₹%,.2f deposited successfully!", amount));
            System.out.printf("  New Balance : " + ConsoleUtil.GREEN + "₹%,.2f" +
                              ConsoleUtil.RESET + "%n", updated.getBalance());

        } catch (InvalidInputException e) {
            ConsoleUtil.printError("Validation: " + e.getMessage());
        } catch (BankException | SQLException e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.pressEnterToContinue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  WITHDRAW
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleWithdraw() {
        ConsoleUtil.printSectionHeader("WITHDRAW MONEY");
        try {
            System.out.printf("  Current Balance  : ₹%,.2f%n",
                              loggedInAccount.getBalance());
            System.out.printf("  Max Withdrawable : ₹%,.2f%n",
                              loggedInAccount.getWithdrawableAmount());
            System.out.print("\n  Enter withdrawal amount (₹) : ");
            BigDecimal amount = InputValidator.validateAmount(scanner.nextLine());

            System.out.print("  Description (optional)       : ");
            String description = scanner.nextLine().trim();

            System.out.printf("%n  Confirm withdrawal of ₹%,.2f? [Y/N] : ", amount);
            if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
                ConsoleUtil.printWarning("Withdrawal cancelled."); return;
            }

            Account updated = bankService.withdraw(
                loggedInAccount.getAccountNumber(), amount, description);
            loggedInAccount = updated;

            ConsoleUtil.printSuccess(String.format(
                "₹%,.2f withdrawn successfully!", amount));
            System.out.printf("  New Balance : " + ConsoleUtil.YELLOW + "₹%,.2f" +
                              ConsoleUtil.RESET + "%n", updated.getBalance());

        } catch (InsufficientFundsException e) {
            ConsoleUtil.printError(e.getMessage());
            ConsoleUtil.printInfo(String.format(
                "Maximum you can withdraw right now: ₹%,.2f",
                loggedInAccount.getWithdrawableAmount()));
        } catch (InvalidInputException e) {
            ConsoleUtil.printError("Validation: " + e.getMessage());
        } catch (BankException | SQLException e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.pressEnterToContinue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TRANSFER
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleTransfer() {
        ConsoleUtil.printSectionHeader("TRANSFER FUNDS");
        try {
            System.out.printf("  Current Balance : ₹%,.2f%n",
                              loggedInAccount.getBalance());

            System.out.print("\n  Destination Account Number : ");
            String destination = InputValidator.validateAccountNumber(scanner.nextLine());

            System.out.print("  Transfer Amount (₹)        : ");
            BigDecimal amount = InputValidator.validateAmount(scanner.nextLine());

            System.out.print("  Description (optional)     : ");
            String description = scanner.nextLine().trim();

            // Show destination account holder name for confirmation
            try {
                Account dest = bankService.checkBalance(destination);
                System.out.println("\n  Transfer To : " + dest.getCustomerName() +
                                   " (" + destination + ")");
            } catch (BankException e) {
                ConsoleUtil.printError("Destination account not found: " + destination);
                return;
            }

            System.out.printf("%n  Confirm transfer of ₹%,.2f to %s? [Y/N] : ",
                              amount, destination);
            if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
                ConsoleUtil.printWarning("Transfer cancelled."); return;
            }

            Account updated = bankService.transfer(
                loggedInAccount.getAccountNumber(), destination, amount, description);
            loggedInAccount = updated;

            ConsoleUtil.printSuccess(String.format(
                "₹%,.2f transferred to %s successfully!", amount, destination));
            System.out.printf("  Your New Balance : " + ConsoleUtil.YELLOW + "₹%,.2f" +
                              ConsoleUtil.RESET + "%n", updated.getBalance());

        } catch (InsufficientFundsException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (InvalidInputException e) {
            ConsoleUtil.printError("Validation: " + e.getMessage());
        } catch (BankException | SQLException e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.pressEnterToContinue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TRANSACTION HISTORY
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleTransactionHistory() {
        ConsoleUtil.printSectionHeader("TRANSACTION HISTORY");

        System.out.println("  View:");
        System.out.println("    1. Last 10 transactions");
        System.out.println("    2. Last 25 transactions");
        System.out.println("    3. All transactions");
        System.out.print("  Choose [1-3] : ");

        try {
            int choice = InputValidator.validateMenuChoice(scanner.nextLine(), 1, 3);
            List<Transaction> transactions;
            String accountNumber = loggedInAccount.getAccountNumber();

            switch (choice) {
                case 1 -> transactions = bankService.getRecentTransactions(accountNumber, 10);
                case 2 -> transactions = bankService.getRecentTransactions(accountNumber, 25);
                default -> transactions = bankService.getTransactionHistory(accountNumber);
            }

            ConsoleUtil.printTransactionTable(transactions);

            // Offer to export to CSV
            if (!transactions.isEmpty()) {
                System.out.print("  Export these transactions to file? [Y/N] : ");
                if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
                    for (Transaction t : transactions) {
                        FileLogger.logTransaction(t);
                    }
                    ConsoleUtil.printSuccess("Transactions exported to: " +
                        FileLogger.getTodayTransactionLogPath());
                }
            }

        } catch (InvalidInputException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (SQLException e) {
            ConsoleUtil.printError("Could not fetch history: " + e.getMessage());
        }
        ConsoleUtil.pressEnterToContinue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACCOUNT DETAILS
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleAccountDetails() {
        ConsoleUtil.printSectionHeader("ACCOUNT DETAILS");
        try {
            Account account = bankService.checkBalance(loggedInAccount.getAccountNumber());
            Customer customer = bankService.getAccountService()
                                           .getCustomerById(account.getCustomerId());
            System.out.println();
            ConsoleUtil.printDoubleLine();
            System.out.println("  ACCOUNT INFORMATION");
            ConsoleUtil.printSingleLine();
            System.out.println(account.getSummary());
            System.out.println();
            ConsoleUtil.printSingleLine();
            System.out.println("  CUSTOMER INFORMATION");
            ConsoleUtil.printSingleLine();
            System.out.println(customer.getSummary());
            ConsoleUtil.printDoubleLine();
        } catch (BankException | SQLException e) {
            ConsoleUtil.printError(e.getMessage());
        }
        ConsoleUtil.pressEnterToContinue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOGOUT
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleLogout() {
        if (loggedInAccount != null) {
            FileLogger.logAudit("LOGOUT", loggedInAccount.getAccountNumber(), "Session ended");
            ConsoleUtil.printSuccess("Logged out successfully. Goodbye, " +
                                     loggedInAccount.getCustomerName() + "!");
        }
        loggedInAccount = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EXIT
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleExit() {
        System.out.println();
        ConsoleUtil.printDoubleLine();
        System.out.println("  Thank you for using the Java Banking Management System.");
        System.out.println("  Goodbye!");
        ConsoleUtil.printDoubleLine();
        FileLogger.logInfo("Application exited normally.");
        DatabaseConnection.closeConnection();
        scanner.close();
    }
}
