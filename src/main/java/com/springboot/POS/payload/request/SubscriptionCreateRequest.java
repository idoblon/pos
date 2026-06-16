package com.springboot.POS.payload.request;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCreateRequest {
    private String plan;
    private LocalDateTime purchaseDate;
    private Map<String, Object> paymentDetails;
}
