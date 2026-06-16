package com.springboot.POS.controller;

import com.springboot.POS.payload.dto.SubscriptionDTO;
import com.springboot.POS.payload.dto.SubscriptionNotificationDTO;
import com.springboot.POS.payload.dto.SubscriptionStatsDTO;
import com.springboot.POS.payload.request.SubscriptionRenewalRequest;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // Store subscription endpoints
    @GetMapping("/stores/{storeId}/subscription")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    public ResponseEntity<SubscriptionDTO> getStoreSubscription(@PathVariable Long storeId) {
        SubscriptionDTO subscription = subscriptionService.getStoreSubscription(storeId);
        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/store/subscription/current")
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<SubscriptionDTO> getCurrentSubscription(
            @RequestHeader("Authorization") String jwt) {
        SubscriptionDTO subscription = subscriptionService.getCurrentSubscription(jwt);
        return ResponseEntity.ok(subscription);
    }

    @PostMapping("/stores/{storeId}/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionDTO> createSubscription(
            @PathVariable Long storeId,
            @RequestBody Map<String, Object> request) {
        String plan = (String) request.get("plan");
        Map<String, Object> paymentDetails = (Map<String, Object>) request.get("paymentDetails");

        SubscriptionDTO subscription = subscriptionService.createSubscription(storeId, plan, paymentDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @PostMapping("/stores/{storeId}/subscription/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    public ResponseEntity<SubscriptionDTO> renewSubscription(
            @PathVariable Long storeId,
            @RequestBody SubscriptionRenewalRequest request) {
        SubscriptionDTO subscription = subscriptionService.renewSubscription(
                storeId, request.getPlan(), request.getPaymentDetails());
        return ResponseEntity.ok(subscription);
    }

    @PutMapping("/stores/{storeId}/subscription/plan")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    public ResponseEntity<SubscriptionDTO> updateSubscriptionPlan(
            @PathVariable Long storeId,
            @RequestBody Map<String, String> request) {
        String plan = request.get("plan");
        SubscriptionDTO subscription = subscriptionService.updateSubscriptionPlan(storeId, plan);
        return ResponseEntity.ok(subscription);
    }

    @PatchMapping("/stores/{storeId}/subscription/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> suspendSubscription(
            @PathVariable Long storeId,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        subscriptionService.suspendSubscription(storeId, reason);
        ApiResponse response = new ApiResponse();
        response.setMessage("Subscription suspended successfully");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/stores/{storeId}/subscription/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> reactivateSubscription(@PathVariable Long storeId) {
        subscriptionService.reactivateSubscription(storeId);
        ApiResponse response = new ApiResponse();
        response.setMessage("Subscription reactivated successfully");
        return ResponseEntity.ok(response);
    }

    // Admin endpoints
    @GetMapping("/admin/subscriptions/expiring")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionDTO>> getExpiringSubscriptions(
            @RequestParam(defaultValue = "60") Integer days) {
        List<SubscriptionDTO> subscriptions = subscriptionService.getExpiringSubscriptions(days);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/admin/subscriptions/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionStatsDTO> getSubscriptionStats() {
        SubscriptionStatsDTO stats = subscriptionService.getSubscriptionStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/admin/subscriptions/update-statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateSubscriptionStatuses() {
        subscriptionService.updateSubscriptionStatuses();
        ApiResponse response = new ApiResponse();
        response.setMessage("Subscription statuses updated successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/subscriptions/generate-notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> generateExpirationNotifications() {
        subscriptionService.generateExpirationNotifications();
        ApiResponse response = new ApiResponse();
        response.setMessage("Notifications generated successfully");
        return ResponseEntity.ok(response);
    }

    // Notification endpoints
    @GetMapping("/stores/{storeId}/subscription/notifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    public ResponseEntity<List<SubscriptionNotificationDTO>> getSubscriptionNotifications(
            @PathVariable Long storeId) {
        List<SubscriptionNotificationDTO> notifications =
                subscriptionService.getSubscriptionNotifications(storeId);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/subscription/notifications/{notificationId}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    public ResponseEntity<ApiResponse> markNotificationAsRead(@PathVariable Long notificationId) {
        subscriptionService.markNotificationAsRead(notificationId);
        ApiResponse response = new ApiResponse();
        response.setMessage("Notification marked as read");
        return ResponseEntity.ok(response);
    }
}
