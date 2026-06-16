package com.springboot.POS.payload.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatsDTO {
    private Long totalStores;
    private Long activeSubscriptions;
    private Long expiringCount;
    private Long expiredCount;
    private Long suspendedCount;
    private Double totalRevenue;
    private Long basicCount;
    private Long professionalCount;
    private Long enterpriseCount;
}
