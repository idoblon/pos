package com.springboot.POS.service;

import com.springboot.POS.payload.dto.EmailRequest;

public interface EmailService {
    void sendAccountCreatedEmail(EmailRequest request);
    void sendWelcomeEmail(EmailRequest request);
    void sendPasswordResetEmail(EmailRequest request);
    void sendOrderConfirmationEmail(EmailRequest request);
    void sendRefundConfirmationEmail(EmailRequest request);
    void sendShiftReportEmail(EmailRequest request);
}
