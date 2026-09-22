package com.springboot.POS.controller;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.modal.StoreRegistrationRequest;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.repository.StoreRegistrationRequestRepository;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.EmailService;
import com.springboot.POS.service.PaymentService;
import com.springboot.POS.service.StoreRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.springboot.POS.repository.StoreRepository;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicController {

    private final StoreRegistrationRequestRepository storeRegistrationRequestRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;
    private final StoreRegistrationService registrationService;
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
            request.setStatus("PENDING");
            request.setPaymentStatus("PENDING");
            
            // Set defaults for estimated values
            if (request.getEstimatedBranches() == null || request.getEstimatedBranches() < 1) {
                request.setEstimatedBranches(1);
            }
            if (request.getEstimatedUsers() == null || request.getEstimatedUsers() < 1) {
                request.setEstimatedUsers(1);
            }
            
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
                log.warn("Failed to send email notification: {}", emailException.getMessage());
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

    @PostMapping("/complete-payment")
    public ResponseEntity<ApiResponse> completePayment(@RequestBody PaymentCompletionRequest req) {
        try {
            StoreRegistrationRequest registration = storeRegistrationRequestRepository.findByEmail(req.getEmail()).orElse(null);
            if (registration == null) {
                ApiResponse r = new ApiResponse();
                r.setMessage("No registration found for this email.");
                return ResponseEntity.badRequest().body(r);
            }
            if ("APPROVED".equals(registration.getStatus()) &&
                    userRepository.findByEmail(req.getEmail())
                            .map(u -> "active".equalsIgnoreCase(u.getStatus())).orElse(false)) {
                ApiResponse r = new ApiResponse();
                r.setMessage("Store already activated.");
                return ResponseEntity.badRequest().body(r);
            }

            // Record payment details on registration
            registration.setPaymentStatus("COMPLETED");
            registration.setPaymentMethod(req.getPaymentMethod());
            registration.setTransactionId(req.getTransactionId());
            storeRegistrationRequestRepository.save(registration);

            // Activate user and store
            userRepository.findByEmail(req.getEmail()).ifPresent(user -> {
                user.setStatus("active");
                userRepository.save(user);
                if (user.getStore() != null) {
                    user.getStore().setStatus(com.springboot.POS.domain.StoreStatus.ACTIVE);
                    user.getStore().setSubscriptionStatus("ACTIVE");
                    user.getStore().setSubscriptionPurchaseDate(java.time.LocalDateTime.now());
                    user.getStore().setSubscriptionExpiry(java.time.LocalDateTime.now().plusYears(1));
                    storeRepository.save(user.getStore());
                }
            });

            // If user didn't exist yet (payment before admin approval path), fully approve
            if (userRepository.findByEmail(req.getEmail()).isEmpty()) {
                registration.setStatus("PAYMENT_PENDING");
                storeRegistrationRequestRepository.save(registration);
                registrationService.approveRequestWithOverride(registration.getId(), null, true);
            }

            // Send login credentials email
            try {
                emailService.sendStoreRegistrationApproved(
                    req.getEmail(), registration.getOwnerName(),
                    registration.getStoreName(), req.getEmail()
                );
            } catch (Exception ignored) {}

            ApiResponse response = new ApiResponse();
            response.setMessage("Payment received. You can now log in with your registered email and password.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse response = new ApiResponse();
            response.setMessage("Payment processing failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    public static class PaymentCompletionRequest {
        private String email;
        private String storeName;
        private String paymentMethod;
        private String transactionId;
        private String plan;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getPlan() { return plan; }
        public void setPlan(String plan) { this.plan = plan; }
    }
}