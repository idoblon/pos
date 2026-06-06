package com.springboot.POS.service.impl;

import com.springboot.POS.modal.SubscriptionPayment;
import com.springboot.POS.modal.StoreRegistrationRequest;
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
        "BASIC", 2999.0,
        "PROFESSIONAL", 5999.0,
        "ENTERPRISE", 12999.0
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
        return SUBSCRIPTION_PRICES.getOrDefault(subscriptionPlan, 2999.0);
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