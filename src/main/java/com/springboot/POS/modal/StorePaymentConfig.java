package com.springboot.POS.modal;

import com.springboot.POS.domain.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_payment_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorePaymentConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;
    
    @Column(nullable = false)
    private Boolean isEnabled = true;
    
    // For eSewa
    private String esewaSettlementId;
    private String esewaSecretKey;
    
    // For Khalti
    private String khaltiPublicKey;
    private String khaltiSecretKey;
    
    // For Card payments
    private String cardProcessorName; // Stripe, PayPal, etc.
    private String cardApiKey;
    private String cardSecretKey;
    
    // Bank transfer details
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String ifscCode;
    
    @Column(name = "created_at")
    @org.hibernate.annotations.CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @org.hibernate.annotations.UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private String notes; // Additional instructions for payment method
}