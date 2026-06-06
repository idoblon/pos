package com.springboot.POS.controller;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.modal.StoreRegistrationRequest;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.repository.StoreRegistrationRequestRepository;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.EmailService;
import com.springboot.POS.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicController {

    private final StoreRegistrationRequestRepository storeRegistrationRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.admin.email}")
    private String fallbackAdminEmail;

    @PostMapping("/store-registration-request")
    public ResponseEntity<ApiResponse> submitStoreRegistrationRequest(
            @RequestBody StoreRegistrationRequest request) {
        
        try {
            // Check if email already exists
            if (storeRegistrationRequestRepository.existsByEmail(request.getEmail())) {
                ApiResponse response = new ApiResponse();
                response.setMessage("Email already registered. Please use a different email.");
                return ResponseEntity.badRequest().body(response);
            }

            // Encrypt password
            request.setPassword(passwordEncoder.encode(request.getPassword()));
            
            // Calculate and set subscription amount
            Double subscriptionAmount = paymentService.getSubscriptionAmount(request.getSubscriptionPlan());
            request.setSubscriptionAmount(subscriptionAmount);
            
            // Set default statuses
            request.setStatus("PAYMENT_PENDING");
            request.setPaymentStatus("PENDING");
            
            // Save registration request
            storeRegistrationRequestRepository.save(request);
            
            // Send email notification to all admins
            try {
                List<User> admins = userRepository.findByRole(UserRole.ROLE_ADMIN);
                
                if (admins.isEmpty()) {
                    // Use fallback admin email if no admin users found
                    emailService.sendStoreRegistrationNotification(
                        fallbackAdminEmail,
                        request.getStoreName(),
                        request.getOwnerName(),
                        request.getEmail(),
                        request.getSubscriptionPlan()
                    );
                } else {
                    // Send to all admin users
                    for (User admin : admins) {
                        emailService.sendStoreRegistrationNotification(
                            admin.getEmail(),
                            request.getStoreName(),
                            request.getOwnerName(),
                            request.getEmail(),
                            request.getSubscriptionPlan()
                        );
                    }
                }
            } catch (Exception emailException) {
                // Log email error but don't fail the registration
                System.err.println("Failed to send email notification: " + emailException.getMessage());
                emailException.printStackTrace();
            }
            
            ApiResponse response = new ApiResponse();
            response.setMessage("Store registration request submitted successfully. Please complete payment to proceed. You will receive an email once your request is reviewed.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            ApiResponse response = new ApiResponse();
            response.setMessage("Failed to submit registration request: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}