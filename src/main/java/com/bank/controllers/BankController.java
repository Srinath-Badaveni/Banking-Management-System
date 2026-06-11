package com.bank.controllers;

import com.bank.models.Account;
import com.bank.models.Customer;
import com.bank.models.Transaction;
import com.bank.services.BankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BankController {

    @Autowired
    private BankService bankService;

    @GetMapping("/customers")
    public List<Customer> getCustomers() {
        return bankService.getAllCustomers();
    }

    @PostMapping("/customers")
    public Customer createCustomer(@RequestBody Customer customer) {
        return bankService.createCustomer(customer);
    }

    @GetMapping("/customers/{id}")
    public Customer getCustomer(@PathVariable String id) {
        return bankService.getCustomer(id);
    }

    @GetMapping("/customers/{id}/accounts")
    public List<Account> getCustomerAccounts(@PathVariable String id) {
        return bankService.getAccountsForCustomer(id);
    }

    @PostMapping("/customers/{id}/accounts")
    public Account createAccount(@PathVariable String id, @RequestBody Map<String, String> payload) {
        Account.AccountType type = Account.AccountType.valueOf(payload.get("accountType"));
        BigDecimal initialBalance = new BigDecimal(payload.get("initialBalance"));
        String pinHash = payload.get("pinHash");
        return bankService.createAccount(id, type, initialBalance, pinHash);
    }

    @GetMapping("/accounts/{accountNumber}")
    public Account getAccount(@PathVariable String accountNumber) {
        return bankService.getAccount(accountNumber);
    }

    @PostMapping("/accounts/{accountNumber}/transactions")
    public Transaction performTransaction(@PathVariable String accountNumber, @RequestBody Map<String, String> payload) {
        BigDecimal amount = new BigDecimal(payload.get("amount"));
        Transaction.TransactionType type = Transaction.TransactionType.valueOf(payload.get("transactionType"));
        String description = payload.get("description");
        String pin = payload.get("pin");
        return bankService.performTransaction(accountNumber, amount, type, description, pin);
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    public List<Transaction> getTransactions(@PathVariable String accountNumber) {
        return bankService.getTransactions(accountNumber);
    }
}
