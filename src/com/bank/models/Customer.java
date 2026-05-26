package com.bank.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Customer - Represents a bank customer (person who owns one or more accounts).
 *
 * Maps directly to the `customers` table.
 */
public class Customer {

    // ── Fields ───────────────────────────────────────────────────────────────
    private String        customerId;
    private String        fullName;
    private String        email;
    private String        phone;
    private String        address;
    private LocalDateTime createdAt;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Used when creating a brand-new customer (before DB insert). */
    public Customer(String customerId, String fullName,
                    String email, String phone, String address) {
        this.customerId = customerId;
        this.fullName   = fullName;
        this.email      = email;
        this.phone      = phone;
        this.address    = address;
        this.createdAt  = LocalDateTime.now();
    }

    /** Full constructor — used when hydrating from a ResultSet. */
    public Customer(String customerId, String fullName, String email,
                    String phone, String address, LocalDateTime createdAt) {
        this(customerId, fullName, email, phone, address);
        this.createdAt  = createdAt;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String        getCustomerId()  { return customerId;  }
    public String        getFullName()    { return fullName;    }
    public String        getEmail()       { return email;       }
    public String        getPhone()       { return phone;       }
    public String        getAddress()     { return address;     }
    public LocalDateTime getCreatedAt()   { return createdAt;   }

    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setFullName(String fullName)     { this.fullName   = fullName;   }
    public void setEmail(String email)           { this.email      = email;      }
    public void setPhone(String phone)           { this.phone      = phone;      }
    public void setAddress(String address)       { this.address    = address;    }

    // ── Utility ──────────────────────────────────────────────────────────────

    /** Returns a human-readable summary card for the customer. */
    public String getSummary() {
        return String.format(
            "%-20s : %s%n" +
            "%-20s : %s%n" +
            "%-20s : %s%n" +
            "%-20s : %s%n" +
            "%-20s : %s%n" +
            "%-20s : %s",
            "Customer ID",   customerId,
            "Name",          fullName,
            "Email",         email,
            "Phone",         phone,
            "Address",       address,
            "Member Since",  createdAt != null ? createdAt.format(FMT) : "N/A"
        );
    }

    @Override
    public String toString() {
        return "Customer{id='" + customerId + "', name='" + fullName + "', email='" + email + "'}";
    }
}
