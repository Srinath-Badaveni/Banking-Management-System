package com.bank.utils;

import com.bank.models.Transaction;
import java.util.List;

/**
 * ConsoleUtil - Static helpers for all console output formatting.
 *
 * Centralises ASCII art, separator lines, tables, and colour codes so
 * the rest of the application stays free of formatting concerns.
 */
public final class ConsoleUtil {

    // ── ANSI colour codes ─────────────────────────────────────────────────────
    public static final String RESET   = "\u001B[0m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String CYAN    = "\u001B[36m";
    public static final String BOLD    = "\u001B[1m";

    private ConsoleUtil() {}

    // ── Banner ────────────────────────────────────────────────────────────────

    /** Prints the application startup banner. */
    public static void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                              ║");
        System.out.println("║        🏦  JAVA BANKING MANAGEMENT SYSTEM  🏦               ║");
        System.out.println("║                  Core Java + JDBC + MySQL                    ║");
        System.out.println("║                      Version 1.0.0                           ║");
        System.out.println("║                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    // ── Menus ─────────────────────────────────────────────────────────────────

    /** Main (unauthenticated) menu. */
    public static void printMainMenu() {
        System.out.println(BLUE + BOLD);
        System.out.println("┌─────────────────────────────────┐");
        System.out.println("│           MAIN MENU             │");
        System.out.println("├─────────────────────────────────┤");
        System.out.println("│  1. Create New Account          │");
        System.out.println("│  2. Login                       │");
        System.out.println("│  3. Exit                        │");
        System.out.println("└─────────────────────────────────┘");
        System.out.println(RESET);
        System.out.print("  Enter your choice: ");
    }

    /** Authenticated (banking) menu shown after login. */
    public static void printBankingMenu(String accountNumber, String holderName) {
        System.out.println(GREEN + BOLD);
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.printf( "│  Account : %-29s│%n", accountNumber);
        System.out.printf( "│  Holder  : %-29s│%n", holderName);
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  BANKING MENU                           │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  1. Check Balance                       │");
        System.out.println("│  2. Deposit Money                       │");
        System.out.println("│  3. Withdraw Money                      │");
        System.out.println("│  4. Transfer Funds                      │");
        System.out.println("│  5. Transaction History                 │");
        System.out.println("│  6. Account Details                     │");
        System.out.println("│  7. Logout                              │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println(RESET);
        System.out.print("  Enter your choice: ");
    }

    // ── Section headers ───────────────────────────────────────────────────────

    public static void printSectionHeader(String title) {
        System.out.println();
        System.out.println(YELLOW + BOLD + "  ── " + title + " " + "─".repeat(Math.max(0, 50 - title.length())) + RESET);
        System.out.println();
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    public static void printSuccess(String message) {
        System.out.println(GREEN + BOLD + "\n  ✔  " + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(RED + BOLD + "\n  ✘  " + message + RESET);
    }

    public static void printWarning(String message) {
        System.out.println(YELLOW + BOLD + "\n  ⚠  " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(CYAN + "  ℹ  " + message + RESET);
    }

    // ── Separators ────────────────────────────────────────────────────────────

    public static void printDoubleLine() {
        System.out.println("══════════════════════════════════════════════════════════════");
    }

    public static void printSingleLine() {
        System.out.println("──────────────────────────────────────────────────────────────");
    }

    // ── Transaction history table ─────────────────────────────────────────────

    /**
     * Prints a formatted ASCII table of transactions.
     * Handles the case when the list is empty.
     */
    public static void printTransactionTable(List<Transaction> transactions) {
        System.out.println();
        if (transactions == null || transactions.isEmpty()) {
            printWarning("No transactions found for this account.");
            return;
        }

        String header = String.format(
            "| %-17s | %-20s | %-18s | %-15s | %-14s | %-14s |",
            "Transaction ID", "Date & Time", "Type", "Amount", "Balance After", "Reference"
        );
        String divider = "+" + "─".repeat(19) + "+" + "─".repeat(22) + "+"
                       + "─".repeat(20) + "+" + "─".repeat(17) + "+"
                       + "─".repeat(16) + "+" + "─".repeat(16) + "+";

        System.out.println(CYAN + divider);
        System.out.println(header);
        System.out.println(divider + RESET);

        for (Transaction t : transactions) {
            // Colour-code credit/debit rows
            String color = t.getTransactionType().isCredit() ? GREEN : RED;
            System.out.println(color + t.toTableRow() + RESET);
        }

        System.out.println(CYAN + divider + RESET);
        System.out.printf("%n  Total records: %d%n%n", transactions.size());
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    /** Prompts the user to press Enter before continuing. */
    public static void pressEnterToContinue() {
        System.out.print("\n  Press ENTER to continue...");
        try { System.in.read(); } catch (Exception ignored) {}
        // Flush any leftover newlines
        try { while (System.in.available() > 0) System.in.read(); } catch (Exception ignored) {}
    }

    /** Clears the virtual terminal (works in most Unix terminals). */
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
