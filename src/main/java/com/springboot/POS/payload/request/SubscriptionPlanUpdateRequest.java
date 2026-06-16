package com.springboot.POS.payload.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanUpdateRequest {
    private String plan;
}
