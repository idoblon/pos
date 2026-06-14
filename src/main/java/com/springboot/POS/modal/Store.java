package com.springboot.POS.modal;


import com.springboot.POS.domain.StoreStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode

public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    // ADDED: Owner information from registration
    @Column(name = "owner_name")
    private String fullName;
    
    @Column(name = "store_address", columnDefinition = "TEXT")
    private String storeAddress;
    
    // ADDED: Subscription information from registration
    @Column(name = "subscription_plan")
    private String subscriptionPlan; // BASIC, PROFESSIONAL, ENTERPRISE
    
    @Column(name = "estimated_branches")
    private Integer estimatedBranches;
    
    @Column(name = "estimated_users")
    private Integer estimatedUsers;
    
    // ADDED: Revenue tracking
    @Column(name = "total_revenue")
    private Double totalRevenue = 0.0;

    @OneToOne
    private User storeAdmin;

    @Column(name = "created_at")
    @org.hibernate.annotations.CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @org.hibernate.annotations.UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // ADDED: Approval tracking
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "registration_request_id")
    private Long registrationRequestId;

    private String description;

    private String storeType;

    private StoreStatus status;

    @Embedded
    private StoreContact contact = new StoreContact();

    @PrePersist
    protected void onCreate(){
        if (status == null) {
            status = StoreStatus.PENDING;
        }
        if (totalRevenue == null) {
            totalRevenue = 0.0;
        }
        // Ensure contact is initialized with non-null values
        if (contact == null) {
            contact = new StoreContact();
        }
        // Preserve existing email - don't overwrite with empty string
        if (contact.getEmail() == null) {
            contact.setEmail("");
        }
        if (contact.getAddress() == null) {
            contact.setAddress("");
        }
        if (contact.getPhone() == null) {
            contact.setPhone("");
        }
        // Ensure subscription plan defaults to BASIC if null
        if (subscriptionPlan == null || subscriptionPlan.trim().isEmpty()) {
            subscriptionPlan = "BASIC";
        }
    }

}
