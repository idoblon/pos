package com.springboot.POS.controller;

import com.springboot.POS.modal.SubscriptionPayment;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @RequestBody PaymentRequest request) {

        try {
            SubscriptionPayment payment = paymentService.createPaymentRequest(
                    request.getRegistrationRequestId(),
                    request.getPaymentMethod()
            );

            String paymentUrl = paymentService.generatePaymentUrl(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", payment.getTransactionId());
            response.put("amount", payment.getAmount());
            response.put("paymentUrl", paymentUrl);
            response.put("message", "Payment request created successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to initiate payment: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyPayment(
            @RequestBody PaymentVerificationRequest request) {

        try {
            boolean isPaymentValid = paymentService.verifyAndProcessPayment(
                    request.getTransactionId(),
                    request.getPaymentGatewayReference()
            );

            ApiResponse response = new ApiResponse();
            if (isPaymentValid) {
                response.setMessage("Payment verified successfully. Registration is now ready for admin approval.");
                return ResponseEntity.ok(response);
            } else {
                response.setMessage("Payment verification failed. Please contact support.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            ApiResponse response = new ApiResponse();
            response.setMessage("Payment verification error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/status/{registrationRequestId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable Long registrationRequestId) {

        SubscriptionPayment payment = paymentService.getPaymentByRegistrationId(registrationRequestId);

        Map<String, Object> response = new HashMap<>();
        if (payment != null) {
            response.put("exists", true);
            response.put("transactionId", payment.getTransactionId());
            response.put("amount", payment.getAmount());
            response.put("status", payment.getPaymentStatus());
            response.put("paymentMethod", payment.getPaymentMethod());
        } else {
            response.put("exists", false);
            response.put("message", "No payment found for this registration");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback/esewa")
    public ResponseEntity<ApiResponse> esewaCallback(
            @RequestParam String oid,
            @RequestParam String amt,
            @RequestParam String refId) {

        try {
            boolean isPaymentValid = paymentService.verifyAndProcessPayment(oid, refId);

            ApiResponse response = new ApiResponse();
            if (isPaymentValid) {
                response.setMessage("Payment verified successfully");
                return ResponseEntity.ok(response);
            } else {
                response.setMessage("Payment verification failed");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            ApiResponse response = new ApiResponse();
            response.setMessage("Payment callback error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/callback/khalti")
    public ResponseEntity<ApiResponse> khaltiCallback(
            @RequestBody KhaltiCallbackRequest request) {

        try {
            boolean isPaymentValid = paymentService.verifyAndProcessPayment(
                    request.getProductIdentity(),
                    request.getIdx()
            );

            ApiResponse response = new ApiResponse();
            if (isPaymentValid) {
                response.setMessage("Payment verified successfully");
                return ResponseEntity.ok(response);
            } else {
                response.setMessage("Payment verification failed");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            ApiResponse response = new ApiResponse();
            response.setMessage("Payment callback error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getSubscriptionPlans() {
        Map<String, Object> plans = new HashMap<>();
        plans.put("BASIC", Map.of("name", "Basic", "price", 2999.0, "currency", "NPR"));
        plans.put("PROFESSIONAL", Map.of("name", "Professional", "price", 5999.0, "currency", "NPR"));
        plans.put("ENTERPRISE", Map.of("name", "Enterprise", "price", 12999.0, "currency", "NPR"));

        Map<String, Object> response = new HashMap<>();
        response.put("plans", plans);
        return ResponseEntity.ok(response);
    }

    // ─── Inner Request DTOs ───────────────────────────────────────────────────

    public static class PaymentRequest {
        private Long registrationRequestId;
        private String paymentMethod;

        public Long getRegistrationRequestId() { return registrationRequestId; }
        public void setRegistrationRequestId(Long id) { this.registrationRequestId = id; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class PaymentVerificationRequest {
        private String transactionId;
        private String paymentGatewayReference;

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getPaymentGatewayReference() { return paymentGatewayReference; }
        public void setPaymentGatewayReference(String ref) { this.paymentGatewayReference = ref; }
    }

    public static class KhaltiCallbackRequest {
        private String idx;
        private String productIdentity;
        private String productName;
        private String amount;
        private String mobile;
        private String token;

        public String getIdx() { return idx; }
        public void setIdx(String idx) { this.idx = idx; }
        public String getProductIdentity() { return productIdentity; }
        public void setProductIdentity(String productIdentity) { this.productIdentity = productIdentity; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}