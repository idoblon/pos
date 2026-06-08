package com.springboot.POS.controller;

import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.PaymentStatusDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.PaymentService;
import com.springboot.POS.service.StoreRegistrationService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AdminPaymentController {

    private final StoreRegistrationService registrationService;
    private final PaymentService paymentService;
    private final UserService userService;

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