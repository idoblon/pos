package com.springboot.POS.payload.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionNotificationDTO {
    private Long id;
    private Long storeId;
    private String storeName;
    private String type;
    private String title;
    private String message;
    private String priority;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Integer daysRemaining;
}
