package com.springboot.POS.controller;

import com.springboot.POS.modal.SubscriptionChangeRequest;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.repository.SubscriptionChangeRequestRepository;
import com.springboot.POS.service.AdminAuditService;
import com.springboot.POS.service.SubscriptionChangeRequestService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SubscriptionChangeRequestController {
    
    private final SubscriptionChangeRequestService changeRequestService;
    private final UserService userService;
    private final SubscriptionChangeRequestRepository changeRequestRepository;
    private final AdminAuditService auditService;
    private final com.springboot.POS.service.impl.OwnershipGuard ownershipGuard;

    /**
     * Admin marks an upgrade/downgrade request's payment as received
     * (offline/bank transfer confirmation).
     */
    @PostMapping("/admin/subscription-upgrade-requests/{id}/mark-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> markChangeRequestPaid(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader("Authorization") String jwt) throws Exception {
        SubscriptionChangeRequest request = changeRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription change request not found"));
        String reference = body != null ? body.get("reference") : null;
        if (reference != null && !reference.isBlank()) {
            request.setPaymentReference(reference.trim());
        }
        request.setStatus("PAID");
        request.setPaidAt(LocalDateTime.now());
        SubscriptionChangeRequest saved = changeRequestRepository.save(request);
        auditService.record(userService.getUserFromJwtToken(jwt).getId(),
                "SUBSCRIPTION_MARK_PAID", "subscriptionChange", id, "Reference: " + reference);
        log.info("Admin marked subscription change request {} as paid", id);
        return ResponseEntity.ok(saved);
    }
    
    @PostMapping("/subscription-upgrade-requests")
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<SubscriptionChangeRequest> createChangeRequest(
            @RequestBody SubscriptionChangeRequest request,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        log.info("Store admin {} creating subscription change request for store {}", 
            user.getId(), request.getStoreId());
        
        SubscriptionChangeRequest created = changeRequestService.createRequest(request);
        return ResponseEntity.ok(created);
    }
    
    @GetMapping("/subscription-upgrade-requests/store/{storeId}")
    @PreAuthorize("hasAnyRole('STORE_ADMIN', 'STORE_MANAGER')")
    public ResponseEntity<List<SubscriptionChangeRequest>> getStoreRequests(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        log.info("Store admin {} fetching change requests for store {}", user.getId(), storeId);
        
        List<SubscriptionChangeRequest> requests = changeRequestService.getRequestsByStoreId(storeId);
        return ResponseEntity.ok(requests);
    }
    
    @GetMapping("/admin/subscription-change-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionChangeRequest>> getAllChangeRequests(
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        log.info("POS Admin {} fetching subscription change requests, status filter: {}", 
            admin.getId(), status);
        
        List<SubscriptionChangeRequest> requests = (status != null && !status.trim().isEmpty()) 
            ? changeRequestService.getRequestsByStatus(status)
            : changeRequestService.getAllRequests();
        
        return ResponseEntity.ok(requests);
    }

    /**
     * Backward-compat alias: older admin UI builds call
     * {@code /api/admin/subscription-upgrade-requests}. Canonical path is
     * {@code /api/admin/subscription-change-requests}.
     * @deprecated use {@code GET /api/admin/subscription-change-requests}
     */
    @Deprecated
    @GetMapping("/admin/subscription-upgrade-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionChangeRequest>> getAllChangeRequestsAlias(
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("Authorization") String jwt) throws Exception {
        return getAllChangeRequests(status, jwt);
    }

    /**
     * Backward-compat alias for approve.
     * @deprecated use {@code POST /api/admin/subscription-change-requests/{id}/approve}
     */
    @Deprecated
    @PostMapping("/admin/subscription-upgrade-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> approveChangeRequestAlias(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        return approveChangeRequest(id, jwt);
    }
    
    @PostMapping("/admin/subscription-change-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> approveChangeRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        log.info("POS Admin {} approving subscription change request {}", admin.getId(), id);
        
        changeRequestService.approveRequest(id, admin.getId());
        auditService.record(admin.getId(), "SUBSCRIPTION_APPROVE", "subscriptionChange", id, null);
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Subscription change request approved and store plan updated successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/admin/subscription-change-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> rejectChangeRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        String reason = request.get("reason");
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        
        log.info("POS Admin {} rejecting subscription change request {}: {}", 
            admin.getId(), id, reason);
        
        changeRequestService.rejectRequest(id, reason, admin.getId());
        auditService.record(admin.getId(), "SUBSCRIPTION_REJECT", "subscriptionChange", id,
                "Reason: " + reason);
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Subscription change request rejected");
        return ResponseEntity.ok(response);
    }
}
