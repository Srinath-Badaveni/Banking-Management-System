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
    public Account createAccount(String customerId, Account.AccountType type, BigDecimal initialBalance, String pinHash) {
        Customer customer = getCustomer(customerId);
        Account account = new Account();
        account.setAccountNumber("ACC" + UUID.randomUUID().toString().substring(0, 11).toUpperCase());
        account.setCustomer(customer);
        account.setAccountType(type);
        account.setBalance(initialBalance);
        account.setPinHash(pinHash);
        account.setStatus(Account.AccountStatus.ACTIVE);
        return accountRepository.save(account);
    }

    @Transactional
    public Transaction performTransaction(String accountNumber, BigDecimal amount, Transaction.TransactionType type, String description) {
        Account account = getAccount(accountNumber);
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
