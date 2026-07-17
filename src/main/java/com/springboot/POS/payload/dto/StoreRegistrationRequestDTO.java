package com.springboot.POS.payload.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreRegistrationRequestDTO {
    private Long id;
    private String ownerName;
    private String email;
    private String phone;
    private String password;
    private String storeName;
    private String storeDescription;
    private String storeType;
    private String storeAddress;
    private String subscriptionPlan;
    private String status;
    private String rejectionReason;
    private Integer estimatedBranches;
    private Integer estimatedUsers;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Long createdStoreId;
    private Long createdUserId;
}
