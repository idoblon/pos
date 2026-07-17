package com.springboot.POS.modal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_registration_request")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
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
    private String password;

    @Column(nullable = false)
    private String storeName;

    @Column(columnDefinition = "TEXT")
    private String storeDescription;

    private String storeType;

    @Column(columnDefinition = "TEXT")
    private String storeAddress;

    @Column(nullable = false)
    private String subscriptionPlan = "BASIC";

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private Integer estimatedBranches;
    private Integer estimatedUsers;

    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private Double subscriptionAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private Long approvedByAdminId;
    private Long createdStoreId;
    private Long createdUserId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
