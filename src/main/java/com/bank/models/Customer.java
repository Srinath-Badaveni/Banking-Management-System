package com.bank.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(name = "customer_id", length = 12, nullable = false)
    private String customerId;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 15, nullable = false, unique = true)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
