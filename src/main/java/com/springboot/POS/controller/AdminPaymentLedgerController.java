package com.springboot.POS.controller;

import com.springboot.POS.modal.StoreRegistrationRequest;
import com.springboot.POS.modal.SubscriptionPayment;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.repository.StoreRegistrationRequestRepository;
import com.springboot.POS.repository.SubscriptionPaymentRepository;
import com.springboot.POS.service.AdminAuditService;
import com.springboot.POS.service.PaymentService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Server-backed payment ledger for the admin Payments page.
 * Replaces the frontend's localStorage-derived revenue list with a paginated
 * query over {@link SubscriptionPayment} joined to registration requests.
 * Showcase-safe: read-only list plus confirm/void actions that delegate to
 * the existing {@link PaymentService} flows.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentLedgerController {

    private final SubscriptionPaymentRepository paymentRepository;
    private final StoreRegistrationRequestRepository registrationRepository;
    private final PaymentService paymentService;
    private final UserService userService;
    private final AdminAuditService auditService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> ledger(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<SubscriptionPayment> all = (status != null && !status.isBlank())
                ? paymentRepository.findByPaymentStatus(status.toUpperCase())
                : paymentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Map<String, Object>> rows = all.stream().map(this::toRow).collect(Collectors.toList());
        int from = Math.min(page * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        Page<Map<String, Object>> result = new PageImpl<>(rows.subList(from, to),
                PageRequest.of(page, safeSize), rows.size());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", result.getContent());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        body.put("totalRevenue", rows.stream()
                .filter(r -> "COMPLETED".equals(r.get("paymentStatus")))
                .mapToDouble(r -> ((Number) r.getOrDefault("amount", 0)).doubleValue()).sum());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{registrationId}/confirm")
    public ResponseEntity<ApiResponse> confirm(
            @PathVariable Long registrationId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader("Authorization") String jwt) {
        Long adminId = userService.getUserFromJwtToken(jwt).getId();
        String reference = body != null ? body.getOrDefault("reference", null) : null;
        paymentService.adminMarkPaymentCompleted(registrationId, reference, adminId);
        auditService.record(adminId, "PAYMENT_CONFIRM", "subscriptionPayment", registrationId,
                "Reference: " + reference);
        ApiResponse response = new ApiResponse();
        response.setMessage("Payment confirmed and recorded in ledger");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{registrationId}/void")
    public ResponseEntity<ApiResponse> voidPayment(
            @PathVariable Long registrationId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader("Authorization") String jwt) {
        String reason = body != null ? body.getOrDefault("reason", "Voided by admin") : "Voided by admin";
        paymentService.markPaymentFailed(
                paymentRepository.findByRegistrationRequestId(registrationId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found")).getTransactionId(),
                reason);
        auditService.record(userService.getUserFromJwtToken(jwt).getId(),
                "PAYMENT_VOID", "subscriptionPayment", registrationId, "Reason: " + reason);
        ApiResponse response = new ApiResponse();
        response.setMessage("Payment voided");
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toRow(SubscriptionPayment payment) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", payment.getId());
        row.put("registrationId", payment.getRegistrationRequestId());
        row.put("subscriptionPlan", payment.getSubscriptionPlan());
        row.put("amount", payment.getAmount());
        row.put("currency", payment.getCurrency());
        row.put("paymentMethod", payment.getPaymentMethod());
        row.put("paymentStatus", payment.getPaymentStatus());
        row.put("transactionId", payment.getTransactionId());
        row.put("paymentGatewayReference", payment.getPaymentGatewayReference());
        row.put("createdAt", payment.getCreatedAt());
        row.put("paidAt", payment.getPaidAt());
        registrationRepository.findById(payment.getRegistrationRequestId()).ifPresent(reg -> {
            row.put("storeName", reg.getStoreName());
            row.put("ownerName", reg.getOwnerName());
            row.put("email", reg.getEmail());
            row.put("registrationStatus", reg.getStatus());
        });
        return row;
    }
}
