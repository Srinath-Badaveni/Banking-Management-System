package com.bank.services;

import com.bank.models.Account;
import com.bank.models.Customer;
import com.bank.models.Transaction;
import com.bank.repositories.AccountRepository;
import com.bank.repositories.CustomerRepository;
import com.bank.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service
public class BankService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomer(String id) {
        return customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing PIN", e);
        }
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customer.getCustomerId() == null || customer.getCustomerId().isEmpty()) {
            customer.setCustomerId("CUST" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        return customerRepository.save(customer);
    }

    public List<Account> getAccountsForCustomer(String customerId) {
        return accountRepository.findByCustomerCustomerId(customerId);
    }

    public Account getAccount(String accountNumber) {
        return accountRepository.findById(accountNumber).orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Transactional
    public Account createAccount(String customerId, Account.AccountType type, BigDecimal initialBalance,
            String pinHash) {
        Customer customer = getCustomer(customerId);
        Account account = new Account();
        account.setAccountNumber("ACC" + UUID.randomUUID().toString().substring(0, 11).toUpperCase());
        account.setCustomer(customer);
        account.setAccountType(type);
        account.setBalance(initialBalance);
        account.setPinHash(hashPin(pinHash));
        account.setStatus(Account.AccountStatus.ACTIVE);
        Account savedAccount = accountRepository.save(account);

        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            Transaction transaction = new Transaction();
            transaction.setTransactionId("TXN" + UUID.randomUUID().toString().substring(0, 17).toUpperCase());
            transaction.setAccount(savedAccount);
            transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
            transaction.setAmount(initialBalance);
            transaction.setBalanceAfter(initialBalance);
            transaction.setDescription("Initial Deposit");
            transactionRepository.save(transaction);
        }

        return savedAccount;
    }

    @Transactional
    public Transaction performTransaction(String accountNumber, BigDecimal amount, Transaction.TransactionType type,
            String description, String pin) {
        Account account = getAccount(accountNumber);

        // Verify PIN

        String hashedPin = hashPin(pin);
        if (!account.getPinHash().equals(hashedPin)) {
            throw new RuntimeException("Invalid PIN");
        }

        if (type == Transaction.TransactionType.WITHDRAWAL || type == Transaction.TransactionType.TRANSFER_OUT) {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient funds");
            }
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            account.setBalance(account.getBalance().add(amount));
        }
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN" + UUID.randomUUID().toString().substring(0, 17).toUpperCase());
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactions(String accountNumber) {
        return transactionRepository.findByAccountAccountNumberOrderByTransactionDateDesc(accountNumber);
    }
}
