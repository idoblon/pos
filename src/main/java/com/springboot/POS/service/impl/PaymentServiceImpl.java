package com.springboot.POS.service.impl;

import com.springboot.POS.modal.SubscriptionPayment;
import com.springboot.POS.modal.StoreRegistrationRequest;
import com.springboot.POS.payload.dto.PaymentStatusDTO;
import com.springboot.POS.repository.SubscriptionPaymentRepository;
import com.springboot.POS.repository.StoreRegistrationRequestRepository;
import com.springboot.POS.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final SubscriptionPaymentRepository paymentRepository;
    private final StoreRegistrationRequestRepository registrationRepository;
    
    @Value("${app.esewa.merchant-id}")
    private String esewaSettlementId;
    
    @Value("${app.esewa.secret-key}")
    private String esewaSecretKey;
    
    @Value("${app.esewa.service-url}")
    private String esewaServiceUrl;
    
    @Value("${app.khalti.public-key}")
    private String khaltiPublicKey;
    
    @Value("${app.khalti.secret-key}")
    private String khaltiSecretKey;
    
    @Value("${app.payment.success-url}")
    private String successUrl;
    
    @Value("${app.payment.failure-url}")
    private String failureUrl;

    // Subscription plan pricing
    private static final Map<String, Double> SUBSCRIPTION_PRICES = Map.of(
        "BASIC", 3500.0,
        "PROFESSIONAL", 7000.0,
        "ENTERPRISE", 10000.0
    );

    @Override
    public SubscriptionPayment createPaymentRequest(Long registrationRequestId, String paymentMethod) {
        StoreRegistrationRequest registration = registrationRepository.findById(registrationRequestId)
                .orElseThrow(() -> new RuntimeException("Registration request not found"));

        // Check if payment already exists
        if (paymentRepository.findByRegistrationRequestId(registrationRequestId).isPresent()) {
            throw new RuntimeException("Payment request already exists for this registration");
        }

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setRegistrationRequestId(registrationRequestId);
        payment.setSubscriptionPlan(registration.getSubscriptionPlan());
        payment.setAmount(getSubscriptionAmount(registration.getSubscriptionPlan()));
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId(generateTransactionId());
        payment.setPaymentStatus("PENDING");
        
        // Set subscription period (1 month)
        payment.setSubscriptionStartDate(LocalDateTime.now());
        payment.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
        payment.setIsRecurring(true);

        SubscriptionPayment savedPayment = paymentRepository.save(payment);
        
        // Update registration request
        registration.setPaymentStatus("PENDING");
        registration.setSubscriptionAmount(payment.getAmount());
        registration.setPaymentMethod(paymentMethod);
        registration.setTransactionId(payment.getTransactionId());
        registration.setStatus("PAYMENT_PENDING");
        registrationRepository.save(registration);
        
        return savedPayment;
    }

    @Override
    public boolean verifyAndProcessPayment(String transactionId, String paymentGatewayReference) {
        SubscriptionPayment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        try {
            // In real implementation, verify with payment gateway
            boolean isPaymentValid = verifyWithPaymentGateway(payment.getPaymentMethod(), paymentGatewayReference);
            
            if (isPaymentValid) {
                payment.setPaymentStatus("COMPLETED");
                payment.setPaidAt(LocalDateTime.now());
                payment.setPaymentGatewayReference(paymentGatewayReference);
                paymentRepository.save(payment);
                
                // Update registration request
                StoreRegistrationRequest registration = registrationRepository.findById(payment.getRegistrationRequestId())
                        .orElseThrow(() -> new RuntimeException("Registration request not found"));
                registration.setPaymentStatus("COMPLETED");
                registration.setStatus("PENDING"); // Ready for admin approval
                registrationRepository.save(registration);
                
                return true;
            } else {
                markPaymentFailed(transactionId, "Payment verification failed");
                return false;
            }
        } catch (Exception e) {
            markPaymentFailed(transactionId, "Payment processing error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public SubscriptionPayment getPaymentByRegistrationId(Long registrationRequestId) {
        return paymentRepository.findByRegistrationRequestId(registrationRequestId)
                .orElse(null);
    }

    @Override
    public Double getSubscriptionAmount(String subscriptionPlan) {
        return SUBSCRIPTION_PRICES.getOrDefault(subscriptionPlan, 3500.0);
    }

    @Override
    public String generatePaymentUrl(SubscriptionPayment payment) {
        switch (payment.getPaymentMethod().toUpperCase()) {
            case "ESEWA":
                return generateEsewaUrl(payment);
            case "KHALTI":
                return generateKhaltiUrl(payment);
            default:
                throw new RuntimeException("Unsupported payment method: " + payment.getPaymentMethod());
        }
    }

    @Override
    public void markPaymentFailed(String transactionId, String reason) {
        SubscriptionPayment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setPaymentStatus("FAILED");
        payment.setPaymentGatewayResponse(reason);
        paymentRepository.save(payment);
        
        // Update registration request
        StoreRegistrationRequest registration = registrationRepository.findById(payment.getRegistrationRequestId())
                .orElseThrow(() -> new RuntimeException("Registration request not found"));
        registration.setPaymentStatus("FAILED");
        registrationRepository.save(registration);
    }

    @Override
    public boolean hasValidPayment(Long registrationRequestId) {
        return paymentRepository.findByRegistrationRequestIdAndPaymentStatus(registrationRequestId, "COMPLETED")
                .stream().findFirst().isPresent();
    }

    @Override
    public void adminMarkPaymentCompleted(Long registrationRequestId, String adminReference, Long adminId) {
        try {
            // Get registration request
            StoreRegistrationRequest registration = registrationRepository.findById(registrationRequestId)
                    .orElseThrow(() -> new RuntimeException("Registration request not found with ID: " + registrationRequestId));
            
            // Get or create payment record
            SubscriptionPayment payment = getPaymentByRegistrationId(registrationRequestId);
            
            if (payment == null) {
                // Create new payment record
                payment = new SubscriptionPayment();
                payment.setRegistrationRequestId(registrationRequestId);
                payment.setSubscriptionPlan(registration.getSubscriptionPlan());
                payment.setAmount(getSubscriptionAmount(registration.getSubscriptionPlan()));
                payment.setPaymentMethod("ADMIN_OVERRIDE");
                payment.setTransactionId(generateTransactionId());
                payment.setCurrency("NPR");
                payment.setIsRecurring(true);
                payment.setSubscriptionStartDate(LocalDateTime.now());
                payment.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
            }
            
            // Validate payment is not already completed
            if ("COMPLETED".equals(payment.getPaymentStatus())) {
                throw new RuntimeException("Payment is already marked as completed");
            }
            
            // Mark payment as completed
            payment.setPaymentStatus("COMPLETED");
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaymentGatewayReference(adminReference != null ? adminReference : "ADMIN_MANUAL_" + System.currentTimeMillis());
            payment.setPaymentGatewayResponse(String.format("Manually verified by admin ID: %d at %s", adminId, LocalDateTime.now()));
            
            // Save payment
            SubscriptionPayment savedPayment = paymentRepository.save(payment);
            
            // Update registration request
            registration.setPaymentStatus("COMPLETED");
            registration.setTransactionId(savedPayment.getTransactionId());
            registration.setSubscriptionAmount(savedPayment.getAmount());
            
            // Set status to PENDING only if currently PAYMENT_PENDING
            if ("PAYMENT_PENDING".equals(registration.getStatus())) {
                registration.setStatus("PENDING");
            }
            
            registrationRepository.save(registration);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to mark payment as completed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentStatusDTO getPaymentStatusForAdmin(Long registrationRequestId) {
        try {
            // Get registration request
            StoreRegistrationRequest registration = registrationRepository.findById(registrationRequestId)
                    .orElseThrow(() -> new RuntimeException("Registration request not found"));
            
            // Get payment record
            SubscriptionPayment payment = getPaymentByRegistrationId(registrationRequestId);
            
            // Build DTO
            PaymentStatusDTO.PaymentStatusDTOBuilder builder = PaymentStatusDTO.builder()
                    .registrationId(registrationRequestId)
                    .registrationStatus(registration.getStatus())
                    .registrationPaymentStatus(registration.getPaymentStatus())
                    .storeName(registration.getStoreName())
                    .ownerName(registration.getOwnerName())
                    .subscriptionPlan(registration.getSubscriptionPlan())
                    .registrationAmount(registration.getSubscriptionAmount());
            
            if (payment != null) {
                builder
                    .hasPaymentRecord(true)
                    .paymentId(payment.getId())
                    .paymentStatus(payment.getPaymentStatus())
                    .transactionId(payment.getTransactionId())
                    .paymentAmount(payment.getAmount())
                    .paymentMethod(payment.getPaymentMethod())
                    .paymentGatewayReference(payment.getPaymentGatewayReference())
                    .paymentCreatedAt(payment.getCreatedAt())
                    .paidAt(payment.getPaidAt())
                    .expiresAt(payment.getExpiresAt());
            } else {
                builder.hasPaymentRecord(false);
            }
            
            PaymentStatusDTO dto = builder.build();
            
            // Set decision flags and messages
            dto.setCanApprove(hasValidPayment(registrationRequestId));
            dto.setCanMarkPaymentCompleted(canMarkPaymentCompleted(registrationRequestId));
            dto.setRequiresManualIntervention(
                !dto.isCanApprove() && 
                ("PENDING".equals(registration.getStatus()) || "PAYMENT_PENDING".equals(registration.getStatus()))
            );
            
            // Set status message
            if (dto.isPaymentCompleted()) {
                dto.setStatusMessage("Payment completed successfully");
                dto.setRecommendedAction("Ready for approval");
            } else if (dto.isPaymentPending() || !dto.isHasPaymentRecord()) {
                dto.setStatusMessage("Payment not completed");
                dto.setRecommendedAction("Mark payment as completed or approve with override");
            } else if (dto.isPaymentFailed()) {
                dto.setStatusMessage("Payment failed");
                dto.setRecommendedAction("Create new payment or approve with override");
            }
            
            return dto;
            
        } catch (Exception e) {
            return PaymentStatusDTO.builder()
                    .registrationId(registrationRequestId)
                    .error("Failed to retrieve payment status: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean canMarkPaymentCompleted(Long registrationRequestId) {
        try {
            SubscriptionPayment payment = getPaymentByRegistrationId(registrationRequestId);
            
            // Can mark as completed if:
            // 1. No payment record exists, OR
            // 2. Payment exists but is not already completed
            return payment == null || !"COMPLETED".equals(payment.getPaymentStatus());
        } catch (Exception e) {
            return false;
        }
    }

    // Private helper methods
    
    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateEsewaUrl(SubscriptionPayment payment) {
        // eSewa Test Environment URL
        return String.format("%s?tAmt=%.0f&amt=%.0f&txAmt=0&psc=0&pdc=0&scd=%s&pid=%s&su=%s&fu=%s", 
            esewaServiceUrl,
            payment.getAmount(),
            payment.getAmount(),
            esewaSettlementId,
            payment.getTransactionId(),
            successUrl + "?txnId=" + payment.getTransactionId(),
            failureUrl + "?txnId=" + payment.getTransactionId()
        );
    }

    private String generateKhaltiUrl(SubscriptionPayment payment) {
        // For Khalti, return JSON config for frontend integration
        return String.format(
            "{\"public_key\": \"%s\", \"product_identity\": \"%s\", \"product_name\": \"POS System Subscription\", \"amount\": %d}",
            khaltiPublicKey,
            payment.getTransactionId(),
            payment.getAmount().intValue() * 100
        );
    }

    private boolean verifyWithPaymentGateway(String paymentMethod, String gatewayReference) {
        // In real implementation, make API calls to verify payment
        // For now, simulate verification (90% success rate for testing)
        return Math.random() > 0.1;
    }
}