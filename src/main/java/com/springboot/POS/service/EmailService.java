package com.springboot.POS.service;

import com.springboot.POS.payload.dto.EmailRequest;

public interface EmailService {
    void sendAccountCreatedEmail(EmailRequest request);
    void sendWelcomeEmail(EmailRequest request);
    void sendPasswordResetEmail(EmailRequest request);
    void sendOrderConfirmationEmail(EmailRequest request);
    void sendRefundConfirmationEmail(EmailRequest request);
    void sendShiftReportEmail(EmailRequest request);
    
    // Restock request emails
    void sendRestockRequestEmail(String toEmail, String toName, String branchName, String productName, Integer requestedQty, Integer currentStock);
    void sendRestockApprovedEmail(String toEmail, String toName, String productName, Integer approvedQty);
    void sendRestockRejectedEmail(String toEmail, String toName, String productName, String reason);
    void sendRestockFulfilledEmail(String toEmail, String toName, String productName, Integer fulfilledQty, Integer newStock);
    
    // Store registration emails
    void sendStoreRegistrationNotification(String adminEmail, String storeName, String ownerName, String ownerEmail, String subscriptionPlan);
    void sendStoreRegistrationApproved(String applicantEmail, String ownerName, String storeName, String loginEmail, String tempPassword);
    void sendStoreRegistrationRejected(String applicantEmail, String ownerName, String storeName, String rejectionReason);
}
