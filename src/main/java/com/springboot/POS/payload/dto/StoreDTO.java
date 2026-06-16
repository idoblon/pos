package com.springboot.POS.payload.dto;

import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.modal.StoreContact;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoreDTO {

    private Long id;

    private String brand;

    private UserDTO storeAdmin;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String description;

    private String storeType;

    private StoreStatus status;

    private StoreContact contact = new StoreContact();
    
    // Registration and subscription fields
    private String fullName;
    private String email;
    private String phone;
    private String storeAddress;
    private String subscriptionPlan;
    private LocalDateTime subscriptionPurchaseDate;
    private LocalDateTime subscriptionExpiry;
    private String subscriptionStatus;
    private Integer subscriptionRenewalCount;
    private Integer estimatedBranches;
    private Integer estimatedUsers;
    private Double totalRevenue;
    private LocalDateTime approvedAt;
    private Long registrationRequestId;

}
