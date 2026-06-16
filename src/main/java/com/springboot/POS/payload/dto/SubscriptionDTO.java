package com.springboot.POS.payload.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDTO {
    private Long storeId;
    private String storeName;
    private String subscriptionPlan;
    private LocalDateTime subscriptionPurchaseDate;
    private LocalDateTime subscriptionExpiry;
    private String subscriptionStatus;
    private Integer daysRemaining;
    private Integer subscriptionRenewalCount;
    private LocalDateTime lastSubscriptionRenewal;
    private Double annualPrice;
    private Double monthlyPrice;
}
