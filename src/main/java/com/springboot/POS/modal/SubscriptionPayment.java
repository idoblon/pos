package com.springboot.POS.modal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long registrationRequestId;
    
    @Column(nullable = false)
    private String subscriptionPlan; // BASIC, PROFESSIONAL, ENTERPRISE
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(nullable = false)
    private String currency = "NPR";
    
    @Column(nullable = false)
    private String paymentMethod; // ESEWA, KHALTI, BANK_TRANSFER
    
    @Column(nullable = false)
    private String paymentStatus = "PENDING"; // PENDING, COMPLETED, FAILED, REFUNDED
    
    @Column(unique = true)
    private String transactionId;
    
    private String paymentGatewayReference;
    
    private String paymentGatewayResponse;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    
    private LocalDateTime expiresAt;
    
    // For subscription billing
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private Boolean isRecurring = false;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // Payment expires after 24 hours
        expiresAt = LocalDateTime.now().plusDays(1);
    }
}