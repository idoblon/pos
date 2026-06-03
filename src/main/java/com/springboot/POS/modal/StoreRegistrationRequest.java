package com.springboot.POS.modal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_registration_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreRegistrationRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String ownerName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String phone;
    
    @Column(nullable = false)
    private String password; // Will be encrypted
    
    @Column(nullable = false)
    private String storeName;
    
    @Column(columnDefinition = "TEXT")
    private String storeDescription;
    
    private String storeType;
    
    @Column(columnDefinition = "TEXT")
    private String storeAddress;
    
    @Column(nullable = false)
    private String subscriptionPlan; // BASIC, PROFESSIONAL, ENTERPRISE
    
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
    
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime processedAt;
    
    private Long approvedByAdminId;
    
    private Long createdStoreId; // Store ID after approval
    
    private Long createdUserId; // User ID after approval
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
