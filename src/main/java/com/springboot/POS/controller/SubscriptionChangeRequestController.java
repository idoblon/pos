package com.springboot.POS.controller;

import com.springboot.POS.modal.SubscriptionChangeRequest;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.SubscriptionChangeRequestService;
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
@RequestMapping("/api")
public class SubscriptionChangeRequestController {
    
    private final SubscriptionChangeRequestService changeRequestService;
    private final UserService userService;
    
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
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<List<SubscriptionChangeRequest>> getStoreRequests(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
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
    
    @PostMapping("/admin/subscription-change-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> approveChangeRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User admin = userService.getUserFromJwtToken(jwt);
        log.info("POS Admin {} approving subscription change request {}", admin.getId(), id);
        
        changeRequestService.approveRequest(id, admin.getId());
        
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
        
        ApiResponse response = new ApiResponse();
        response.setMessage("Subscription change request rejected");
        return ResponseEntity.ok(response);
    }
}
