package com.springboot.POS.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusDTO {
    
    private Long registrationId;
    private String registrationStatus;
    private String registrationPaymentStatus;
    private String storeName;
    private String ownerName;
    private String subscriptionPlan;
    private Double registrationAmount;
    
    // Payment record details
    private boolean hasPaymentRecord;
    private Long paymentId;
    private String paymentStatus;
    private String transactionId;
    private Double paymentAmount;
    private String paymentMethod;
    private String paymentGatewayReference;
    private LocalDateTime paymentCreatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    
    // Admin decision flags
    private boolean canApprove;
    private boolean canMarkPaymentCompleted;
    private boolean requiresManualIntervention;
    
    // Status messages
    private String statusMessage;
    private String recommendedAction;
    private String error;
    
    // Convenience methods
    public boolean isPaymentCompleted() {
        return "COMPLETED".equals(paymentStatus);
    }
    
    public boolean isPaymentPending() {
        return "PENDING".equals(paymentStatus);
    }
    
    public boolean isPaymentFailed() {
        return "FAILED".equals(paymentStatus);
    }
    
    public boolean isRegistrationApproved() {
        return "APPROVED".equals(registrationStatus);
    }
    
    public boolean isRegistrationPending() {
        return "PENDING".equals(registrationStatus);
    }
    
    public boolean isPaymentExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}