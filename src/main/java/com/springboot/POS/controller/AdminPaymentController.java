package com.springboot.POS.controller;

import com.springboot.POS.modal.StoreRegistrationRequest;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.PaymentStatusDTO;
import com.springboot.POS.repository.StoreRegistrationRequestRepository;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.PaymentService;
import com.springboot.POS.service.StoreRegistrationService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminPaymentController {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final StoreRegistrationService registrationService;
    private final PaymentService paymentService;
    private final UserService userService;
    private final StoreRegistrationRequestRepository storeRegistrationRequestRepository;
    private final UserRepository userRepository;

    /**
     * Admin payment status lookup. Explicit ADMIN guard (URL filter also
     * requires it). Pre-login callers must use
     * {@code GET /api/public/store-payment/status} instead.
     */
    @GetMapping("/store-payment/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStorePaymentStatus(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String email) {
        try {
            StoreRegistrationRequest reg = null;
            if (email != null && !email.isBlank()) {
                reg = storeRegistrationRequestRepository.findByEmail(email).orElse(null);
            }
            if (reg == null) {
                return ResponseEntity.ok(java.util.Map.of(
                    "status", "NOT_FOUND",
                    "message", "No registration found."
                ));
            }
            // Check if user is already active
            boolean paid = userRepository.findByEmail(reg.getEmail())
                    .map(u -> "active".equalsIgnoreCase(u.getStatus())).orElse(false);
            if (paid) {
                return ResponseEntity.ok(java.util.Map.of("status", "PAID", "message", "Payment completed."));
            }
            String paymentLink = frontendUrl + "/payment-required?email=" + reg.getEmail();
            return ResponseEntity.ok(java.util.Map.of(
                "status", "PENDING",
                "message", "Payment pending.",
                "plan", reg.getSubscriptionPlan() != null ? reg.getSubscriptionPlan() : "BASIC",
                "storeName", reg.getStoreName() != null ? reg.getStoreName() : "",
                "paymentLink", paymentLink
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("status", "FAILED", "message", e.getMessage()));
        }
    }

    /**
     * Registrations awaiting payment (status = PAYMENT_PENDING) — used by the
     * admin store-payment simulation screen.
     */
    @GetMapping("/store-requests/payment-pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StoreRegistrationRequest>> getPaymentPendingStoreRequests() {
        return ResponseEntity.ok(
                storeRegistrationRequestRepository.findByStatusOrderByCreatedAtDesc("PAYMENT_PENDING"));
    }

    /**
     * Manually record an offline/simulated store payment: marks the registration's
     * payment as completed so the request becomes ready for approval.
     */
    @PostMapping("/store-payment/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> completeStorePayment(
            @RequestBody StorePaymentCompleteRequest request,
            @RequestHeader("Authorization") String jwt) {
        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            }
            User admin = userService.getUserFromJwtToken(jwt);
            StoreRegistrationRequest reg = storeRegistrationRequestRepository
                    .findByEmail(request.getEmail()).orElse(null);
            if (reg == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No registration found for " + request.getEmail()));
            }
            String reference = request.getPaymentDetails() != null
                    && request.getPaymentDetails().getTransactionId() != null
                    && !request.getPaymentDetails().getTransactionId().isBlank()
                    ? request.getPaymentDetails().getTransactionId()
                    : "MANUAL-" + System.currentTimeMillis();
            if (paymentService.canMarkPaymentCompleted(reg.getId())) {
                paymentService.adminMarkPaymentCompleted(reg.getId(), reference, admin.getId());
            }
            return ResponseEntity.ok(Map.of(
                    "paymentId", reg.getId(),
                    "message", "Payment completed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Failed to complete payment: " + e.getMessage()));
        }
    }

    /**
     * Approve store registration with optional payment override
     */
    @PostMapping("/registration-requests/{id}/approve-with-override")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> approveRequestWithOverride(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean skipPaymentCheck,
            @RequestHeader("Authorization") String jwt) {
        
        try {
            User admin = userService.getUserFromJwtToken(jwt);
            
            // Validate registration exists
            var request = registrationService.getRequestById(id);
            if (request == null) {
                return ResponseEntity.badRequest().body(
                    createErrorResponse("Registration request not found")
                );
            }
            
            // Use the new override method
            registrationService.approveRequestWithOverride(id, admin.getId(), skipPaymentCheck);
            
            String message = skipPaymentCheck ? 
                "Store registration approved successfully with payment override. Store admin credentials have been sent via email." :
                "Store registration approved successfully. Store admin credentials have been sent via email.";
                
            return ResponseEntity.ok(createSuccessResponse(message));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                createErrorResponse("Failed to approve registration: " + e.getMessage())
            );
        }
    }

    /**
     * Manually mark payment as completed
     */
    @PostMapping("/payments/{registrationId}/mark-completed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> markPaymentCompleted(
            @PathVariable Long registrationId,
            @RequestBody @Valid PaymentCompletionRequest request,
            @RequestHeader("Authorization") String jwt) {
        
        try {
            User admin = userService.getUserFromJwtToken(jwt);
            
            // Validate registration exists
            var registration = registrationService.getRequestById(registrationId);
            if (registration == null) {
                return ResponseEntity.badRequest().body(
                    createErrorResponse("Registration request not found")
                );
            }
            
            // Check if payment can be marked as completed
            if (!paymentService.canMarkPaymentCompleted(registrationId)) {
                return ResponseEntity.badRequest().body(
                    createErrorResponse("Payment is already completed")
                );
            }
            
            paymentService.adminMarkPaymentCompleted(
                registrationId, 
                request.getReference(), 
                admin.getId()
            );
            
            return ResponseEntity.ok(createSuccessResponse(
                "Payment marked as completed successfully. Store registration is now ready for approval."
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                createErrorResponse("Failed to mark payment as completed: " + e.getMessage())
            );
        }
    }

    /**
     * Get comprehensive payment status for admin
     */
    @GetMapping("/payments/{registrationId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentStatusDTO> getPaymentStatus(
            @PathVariable Long registrationId,
            @RequestHeader("Authorization") String jwt) {
        
        try {
            User admin = userService.getUserFromJwtToken(jwt);
            PaymentStatusDTO statusDTO = paymentService.getPaymentStatusForAdmin(registrationId);
            return ResponseEntity.ok(statusDTO);
            
        } catch (Exception e) {
            PaymentStatusDTO errorDTO = PaymentStatusDTO.builder()
                    .registrationId(registrationId)
                    .error("Failed to retrieve payment status: " + e.getMessage())
                    .build();
            return ResponseEntity.ok(errorDTO);
        }
    }

    /**
     * Check if payment can be manually completed
     */
    @GetMapping("/payments/{registrationId}/can-mark-completed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CanMarkCompletedResponse> canMarkPaymentCompleted(
            @PathVariable Long registrationId,
            @RequestHeader("Authorization") String jwt) {
        
        try {
            User admin = userService.getUserFromJwtToken(jwt);
            boolean canMark = paymentService.canMarkPaymentCompleted(registrationId);
            
            return ResponseEntity.ok(new CanMarkCompletedResponse(
                registrationId, canMark, canMark ? "Payment can be marked as completed" : "Payment is already completed"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(new CanMarkCompletedResponse(
                registrationId, false, "Error checking payment status: " + e.getMessage()
            ));
        }
    }

    // Helper methods
    private ApiResponse createSuccessResponse(String message) {
        ApiResponse response = new ApiResponse();
        response.setMessage(message);
        return response;
    }

    private ApiResponse createErrorResponse(String message) {
        ApiResponse response = new ApiResponse();
        response.setMessage(message);
        return response;
    }

    // DTO Classes
    /** Request body of POST /api/admin/store-payment/complete (admin payment simulation). */
    public static class StorePaymentCompleteRequest {
        private Long storeId;
        private String storeName;
        private String ownerName;
        @NotBlank(message = "Email is required")
        private String email;
        private String phone;
        private String subscriptionPlan;
        private PaymentDetails paymentDetails;

        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getSubscriptionPlan() { return subscriptionPlan; }
        public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }
        public PaymentDetails getPaymentDetails() { return paymentDetails; }
        public void setPaymentDetails(PaymentDetails paymentDetails) { this.paymentDetails = paymentDetails; }
    }

    public static class PaymentDetails {
        private Double amount;
        private String method;
        private String transactionId;

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    }
    public static class PaymentCompletionRequest {
        @NotBlank(message = "Reference is required")
        private String reference;
        
        private String notes;
        
        // Constructors
        public PaymentCompletionRequest() {}
        
        public PaymentCompletionRequest(String reference, String notes) {
            this.reference = reference;
            this.notes = notes;
        }
        
        // Getters and setters
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class CanMarkCompletedResponse {
        private Long registrationId;
        private boolean canMarkCompleted;
        private String message;
        
        // Constructors
        public CanMarkCompletedResponse() {}
        
        public CanMarkCompletedResponse(Long registrationId, boolean canMarkCompleted, String message) {
            this.registrationId = registrationId;
            this.canMarkCompleted = canMarkCompleted;
            this.message = message;
        }
        
        // Getters and setters
        public Long getRegistrationId() { return registrationId; }
        public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
        public boolean isCanMarkCompleted() { return canMarkCompleted; }
        public void setCanMarkCompleted(boolean canMarkCompleted) { this.canMarkCompleted = canMarkCompleted; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}