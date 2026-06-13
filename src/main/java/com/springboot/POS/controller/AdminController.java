package com.springboot.POS.controller;

import com.springboot.POS.modal.StoreRegistrationRequest;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.StoreRegistrationService;
import com.springboot.POS.service.StoreService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final StoreRegistrationService registrationService;
    private final StoreService storeService;
    private final UserService userService;

    /**
     * Get store registration requests with optional status filter
     * Matches frontend expectation: GET /api/admin/store-requests?status=PENDING
     */
    @GetMapping("/store-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StoreRegistrationRequest>> getStoreRequests(
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        log.info("Fetching store requests with status filter: {}", status);
        User admin = userService.getUserFromJwtToken(jwt);
        
        List<StoreRegistrationRequest> allRequests = registrationService.getAllRequests();
        log.info("Total requests in database: {}", allRequests.size());
        
        // If status parameter is provided, filter by status
        if (status != null && !status.trim().isEmpty()) {
            List<StoreRegistrationRequest> filteredRequests = allRequests.stream()
                .filter(req -> {
                    boolean matches = status.equalsIgnoreCase(req.getStatus());
                    log.debug("Request ID: {}, Status: {}, Matches filter '{}': {}", 
                        req.getId(), req.getStatus(), status, matches);
                    return matches;
                })
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .toList();
            
            log.info("Filtered requests with status '{}': {}", status, filteredRequests.size());
            return ResponseEntity.ok(filteredRequests);
        }
        
        log.info("Returning all {} requests", allRequests.size());
        return ResponseEntity.ok(allRequests);
    }

    /**
     * Get count of pending store registration requests
     * Matches frontend expectation: GET /api/admin/store-requests/pending/count
     */
    @GetMapping("/store-requests/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getPendingRequestsCount(
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        long count = registrationService.getAllPendingRequests().size();
        log.info("Pending store registration requests count: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * Approve a store registration request
     * Matches frontend expectation: POST /api/admin/store-requests/{id}/approve
     */
    @PostMapping("/store-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> approveStoreRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        registrationService.approveRequest(id, admin.getId());
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Store registration request approved successfully. Approval email sent to applicant.");
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a store registration request
     * Matches frontend expectation: POST /api/admin/store-requests/{id}/reject
     */
    @PostMapping("/store-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> rejectStoreRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        String reason = request.get("reason");
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        
        registrationService.rejectRequest(id, reason, admin.getId());
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Store registration request rejected successfully. Rejection email sent to applicant.");
        return ResponseEntity.ok(response);
    }

    /**
     * Update store subscription plan
     * Matches frontend expectation: PUT /api/admin/stores/{storeId}/subscription
     */
    @PutMapping("/stores/{storeId}/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateStoreSubscription(
            @PathVariable Long storeId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        String subscriptionPlan = request.get("subscriptionPlan");
        
        if (subscriptionPlan == null || subscriptionPlan.trim().isEmpty()) {
            throw new IllegalArgumentException("Subscription plan is required");
        }
        
        log.info("Admin {} updating store {} subscription to: {}", admin.getId(), storeId, subscriptionPlan);
        storeService.updateSubscriptionPlan(storeId, subscriptionPlan.toUpperCase());
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Store subscription plan updated successfully to " + subscriptionPlan);
        return ResponseEntity.ok(response);
    }

    /**
     * Debug endpoint to check all requests (REMOVE IN PRODUCTION)
     */
    @GetMapping("/store-requests/debug/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> debugGetAllRequests(@RequestHeader("Authorization") String jwt) throws Exception {
        User admin = userService.getUserFromJwtToken(jwt);
        List<StoreRegistrationRequest> all = registrationService.getAllRequests();
        
        log.info("=== DEBUG: Total requests in DB: {} ===", all.size());
        all.forEach(req -> {
            log.info("Request ID: {}, Store: {}, Status: '{}', Email: {}, Created: {}",
                req.getId(), req.getStoreName(), req.getStatus(), req.getEmail(), req.getCreatedAt());
        });
        
        return ResponseEntity.ok(Map.of(
            "total", all.size(),
            "requests", all
        ));
    }
}