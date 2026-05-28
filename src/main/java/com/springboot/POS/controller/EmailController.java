package com.springboot.POS.controller;

import com.springboot.POS.payload.dto.EmailRequest;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/account-created")
    public ResponseEntity<ApiResponse> sendAccountCreatedEmail(@RequestBody EmailRequest request) {
        emailService.sendAccountCreatedEmail(request);
        ApiResponse response = new ApiResponse();
        response.setMessage("Account creation email sent successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/welcome")
    public ResponseEntity<ApiResponse> sendWelcomeEmail(@RequestBody EmailRequest request) {
        emailService.sendWelcomeEmail(request);
        ApiResponse response = new ApiResponse();
        response.setMessage("Welcome email sent successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse> sendPasswordResetEmail(@RequestBody EmailRequest request) {
        emailService.sendPasswordResetEmail(request);
        ApiResponse response = new ApiResponse();
        response.setMessage("Password reset email sent successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/order-confirmation")
    public ResponseEntity<ApiResponse> sendOrderConfirmationEmail(@RequestBody EmailRequest request) {
        emailService.sendOrderConfirmationEmail(request);
        ApiResponse response = new ApiResponse();
        response.setMessage("Order confirmation email sent successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund-confirmation")
    public ResponseEntity<ApiResponse> sendRefundConfirmationEmail(@RequestBody EmailRequest request) {
        emailService.sendRefundConfirmationEmail(request);
        ApiResponse response = new ApiResponse();
        response.setMessage("Refund confirmation email sent successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/shift-report")
    public ResponseEntity<ApiResponse> sendShiftReportEmail(@RequestBody EmailRequest request) {
        emailService.sendShiftReportEmail(request);
        ApiResponse response = new ApiResponse();
        response.setMessage("Shift report email sent successfully");
        return ResponseEntity.ok(response);
    }
}