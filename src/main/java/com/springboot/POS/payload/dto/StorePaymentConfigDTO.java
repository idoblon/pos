package com.springboot.POS.payload.dto;

import com.springboot.POS.domain.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorePaymentConfigDTO {
    private Long id;
    private Long storeId;
    private PaymentType paymentType;
    private Boolean isEnabled;
    
    // eSewa
    private String esewaSettlementId;
    private String esewaSecretKey;
    
    // Khalti
    private String khaltiPublicKey;
    private String khaltiSecretKey;
    
    // Card
    private String cardProcessorName;
    private String cardApiKey;
    private String cardSecretKey;
    
    // Bank
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String ifscCode;
    
    private String notes;
}