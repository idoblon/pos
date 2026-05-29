package com.springboot.POS.service.impl;

import com.springboot.POS.payload.dto.EmailRequest;
import com.springboot.POS.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendAccountCreatedEmail(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject("Welcome to POS System - " + request.getStoreName());
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Welcome to POS System!\n\n" +
            "Your store '%s' has been successfully registered.\n\n" +
            "Login Details:\n" +
            "Email: %s\n" +
            "Role: %s\n\n" +
            "You can now login and start managing your store.\n\n" +
            "Best regards,\nPOS System Team",
            request.getStoreName(), request.getStoreName(), 
            request.getTo(), request.getRole()
        ));
        message.setFrom("posproofficial@gmail.com");
        mailSender.send(message);
    }

    @Override
    public void sendWelcomeEmail(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject("Welcome to " + request.getStoreName());
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Welcome to %s!\n\n" +
            "We're excited to have you on board.\n\n" +
            "Best regards,\nPOS System",
            request.getStoreName(), request.getStoreName()
        ));
        message.setFrom("posproofficial@gmail.com");
        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject("Password Reset Request");
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your password has been reset.\n\n" +
            "New Password: %s\n\n" +
            "Please change your password after login.\n\n" +
            "Best regards,\nPOS System",
            request.getUserName(), request.getPassword()
        ));
        mailSender.send(message);
    }

    @Override
    public void sendOrderConfirmationEmail(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject("Order Confirmation - " + request.getOrderNumber());
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your order has been confirmed.\n\n" +
            "Order Number: %s\n" +
            "Amount: $%.2f\n" +
            "Store: %s\n\n" +
            "Thank you for your purchase!\n\n" +
            "Best regards,\n%s",
            request.getUserName(), request.getOrderNumber(), 
            request.getAmount(), request.getStoreName(), request.getStoreName()
        ));
        mailSender.send(message);
    }

    @Override
    public void sendRefundConfirmationEmail(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject("Refund Processed - " + request.getOrderNumber());
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your refund has been processed.\n\n" +
            "Order Number: %s\n" +
            "Refund Amount: $%.2f\n" +
            "Store: %s\n\n" +
            "The amount will be credited to your account within 5-7 business days.\n\n" +
            "Best regards,\n%s",
            request.getUserName(), request.getOrderNumber(), 
            request.getAmount(), request.getStoreName(), request.getStoreName()
        ));
        mailSender.send(message);
    }

    @Override
    public void sendShiftReportEmail(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject("Shift Report - " + request.getBranchName());
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your shift has ended.\n\n" +
            "Branch: %s\n" +
            "Total Sales: $%.2f\n\n" +
            "Please review your shift report in the system.\n\n" +
            "Best regards,\n%s",
            request.getUserName(), request.getBranchName(), 
            request.getAmount(), request.getStoreName()
        ));
        mailSender.send(message);
    }

    @Override
    public void sendRestockRequestEmail(String toEmail, String toName, String branchName, String productName, Integer requestedQty, Integer currentStock) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("New Restock Request - " + branchName);
        message.setText(String.format(
            "Hello %s,\n\n" +
            "A new restock request has been submitted.\n\n" +
            "Branch: %s\n" +
            "Product: %s\n" +
            "Current Stock: %d\n" +
            "Requested Quantity: %d\n\n" +
            "Please review and approve/reject this request in the system.\n\n" +
            "Best regards,\nPOS System",
            toName, branchName, productName, currentStock, requestedQty
        ));
        message.setFrom("posproofficial@gmail.com");
        mailSender.send(message);
    }

    @Override
    public void sendRestockApprovedEmail(String toEmail, String toName, String productName, Integer approvedQty) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Restock Request Approved - " + productName);
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your restock request has been approved!\n\n" +
            "Product: %s\n" +
            "Approved Quantity: %d\n\n" +
            "The stock will be delivered to your branch soon.\n\n" +
            "Best regards,\nPOS System",
            toName, productName, approvedQty
        ));
        message.setFrom("posproofficial@gmail.com");
        mailSender.send(message);
    }

    @Override
    public void sendRestockRejectedEmail(String toEmail, String toName, String productName, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Restock Request Rejected - " + productName);
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your restock request has been rejected.\n\n" +
            "Product: %s\n" +
            "Reason: %s\n\n" +
            "Please contact your store manager for more information.\n\n" +
            "Best regards,\nPOS System",
            toName, productName, reason
        ));
        message.setFrom("posproofficial@gmail.com");
        mailSender.send(message);
    }

    @Override
    public void sendRestockFulfilledEmail(String toEmail, String toName, String productName, Integer fulfilledQty, Integer newStock) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Restock Fulfilled - " + productName);
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your restock request has been fulfilled!\n\n" +
            "Product: %s\n" +
            "Quantity Added: %d\n" +
            "New Stock Level: %d\n\n" +
            "The inventory has been updated in your branch.\n\n" +
            "Best regards,\nPOS System",
            toName, productName, fulfilledQty, newStock
        ));
        message.setFrom("posproofficial@gmail.com");
        mailSender.send(message);
    }
}
