package com.springboot.POS.modal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_change_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionChangeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long storeId;
    
    private String storeName;
    private String ownerName;
    private String email;
    private String phone;
    
    @Column(nullable = false)
    private String currentPlan;
    
    @Column(nullable = false)
    private String requestedPlan;
    
    @Column(nullable = false)
    private String changeType; // UPGRADE or DOWNGRADE
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(nullable = false)
    private String paymentMethod;
    
    @Column(nullable = false)
    private String paymentReference;
    
    @Column(nullable = false)
    private String status = "PAID"; // PAID, APPROVED, REJECTED
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    
    private Long approvedBy;
    private Long rejectedBy;
    private String rejectionReason;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
