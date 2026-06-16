package com.springboot.POS.payload.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionSuspendRequest {
    private String reason;
}
