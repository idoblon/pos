package com.springboot.POS.payload.request;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRenewalRequest {
    private String plan;
    private Map<String, Object> paymentDetails;
}
