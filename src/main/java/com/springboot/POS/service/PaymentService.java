package com.springboot.POS.service;

import com.springboot.POS.modal.SubscriptionPayment;
import com.springboot.POS.modal.StoreRegistrationRequest;

public interface PaymentService {
    
    // Create payment request for registration
    SubscriptionPayment createPaymentRequest(Long registrationRequestId, String paymentMethod);
    
    // Process payment verification (eSewa/Khalti callback)
    boolean verifyAndProcessPayment(String transactionId, String paymentGatewayReference);
    
    // Get payment details
    SubscriptionPayment getPaymentByRegistrationId(Long registrationRequestId);
    
    // Calculate subscription amount based on plan
    Double getSubscriptionAmount(String subscriptionPlan);
    
    // Generate payment URL for eSewa/Khalti
    String generatePaymentUrl(SubscriptionPayment payment);
    
    // Mark payment as failed
    void markPaymentFailed(String transactionId, String reason);
    
    // Check if registration request has valid payment
    boolean hasValidPayment(Long registrationRequestId);
}