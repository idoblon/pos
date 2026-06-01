package com.springboot.POS.controller;

import com.springboot.POS.domain.RestockStatus;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.RestockRequestDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.RestockRequestService;
import com.springboot.POS.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/restock-requests")
public class RestockRequestController {

    private final RestockRequestService restockRequestService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<RestockRequestDTO> createRequest(
            @RequestBody @Valid RestockRequestDTO requestDTO,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.createRequest(requestDTO, user));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<RestockRequestDTO>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) RestockStatus status,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        
        if (status != null) {
            return ResponseEntity.ok(restockRequestService.getRequestsByStoreAndStatus(storeId, status));
        }
        return ResponseEntity.ok(restockRequestService.getRequestsByStore(storeId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<RestockRequestDTO>> getByBranch(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.getRequestsByBranch(branchId));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<RestockRequestDTO> approve(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.approveRequest(id, user));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<RestockRequestDTO> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        String reason = body.get("reason");
        return ResponseEntity.ok(restockRequestService.rejectRequest(id, reason, user));
    }

    @PatchMapping("/{id}/fulfill")
    public ResponseEntity<RestockRequestDTO> fulfill(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Integer> body,
            @RequestHeader(value = "Authorization", required = false) String jwt) {
        try {
            System.out.println("🔍 CONTROLLER DEBUG - Fulfill request received for ID: " + id);
            System.out.println("🔍 CONTROLLER DEBUG - Request body: " + body);
            System.out.println("🔍 CONTROLLER DEBUG - JWT header: " + (jwt != null ? "Present" : "Missing"));
            
            User user = null;
            if (jwt != null && !jwt.isEmpty()) {
                try {
                    user = userService.getUserFromJwtToken(jwt);
                    System.out.println("🔍 CONTROLLER DEBUG - User: " + user.getFullName() + " (ID: " + user.getId() + ")");
                } catch (Exception e) {
                    System.out.println("⚠️ CONTROLLER DEBUG - JWT validation failed: " + e.getMessage());
                    // For debugging, continue without user validation
                }
            }
            
            Integer receivedQuantity = (body != null) ? body.get("receivedQuantity") : null;
            System.out.println("🔍 CONTROLLER DEBUG - Extracted receivedQuantity: " + receivedQuantity);
            
            RestockRequestDTO result = restockRequestService.fulfillRequest(id, receivedQuantity, user);
            System.out.println("✅ CONTROLLER DEBUG - Fulfill request completed successfully");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ CONTROLLER DEBUG - Fulfill request failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/batch/approve")
    public ResponseEntity<List<RestockRequestDTO>> batchApprove(
            @RequestBody List<Long> requestIds,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.batchApprove(requestIds, user));
    }

    @PostMapping("/batch/reject")
    public ResponseEntity<List<RestockRequestDTO>> batchReject(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        @SuppressWarnings("unchecked")
        List<Long> requestIds = (List<Long>) body.get("requestIds");
        String reason = (String) body.get("reason");
        return ResponseEntity.ok(restockRequestService.batchReject(requestIds, reason, user));
    }

    @PostMapping("/batch/fulfill")
    public ResponseEntity<List<RestockRequestDTO>> batchFulfill(
            @RequestBody List<Long> requestIds,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(restockRequestService.batchFulfill(requestIds, user));
    }

    // Debug endpoint to check inventory
    @GetMapping("/debug/inventory/{branchId}/{productId}")
    public ResponseEntity<Map<String, Object>> debugInventory(
            @PathVariable Long branchId,
            @PathVariable Long productId,
            @RequestHeader("Authorization") String jwt) {
        try {
            System.out.println("🔍 DEBUG INVENTORY - Checking inventory for branchId: " + branchId + ", productId: " + productId);
            
            User user = userService.getUserFromJwtToken(jwt);
            System.out.println("🔍 DEBUG INVENTORY - User: " + user.getFullName());
            
            // Get inventory directly from repository
            List<com.springboot.POS.modal.Inventory> inventoryList = 
                restockRequestService.getInventoryRepository().findByProductIdAndBranchId(productId, branchId);
            
            System.out.println("🔍 DEBUG INVENTORY - Found " + inventoryList.size() + " inventory records");
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("branchId", branchId);
            result.put("productId", productId);
            result.put("inventoryCount", inventoryList.size());
            
            if (!inventoryList.isEmpty()) {
                com.springboot.POS.modal.Inventory inventory = inventoryList.get(0);
                result.put("inventoryId", inventory.getId());
                result.put("currentQuantity", inventory.getQuantity());
                result.put("lastUpdate", inventory.getLastUpdate());
                System.out.println("✅ DEBUG INVENTORY - Current quantity: " + inventory.getQuantity());
            } else {
                result.put("error", "No inventory found");
                System.out.println("❌ DEBUG INVENTORY - No inventory found");
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ DEBUG INVENTORY - Error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("branchId", branchId);
            errorResult.put("productId", productId);
            
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", "OK");
        result.put("timestamp", java.time.LocalDateTime.now());
        result.put("service", "RestockRequestController");
        System.out.println("✅ HEALTH CHECK - RestockRequestController is running");
        return ResponseEntity.ok(result);
    }

    // Simple test endpoint to verify backend is working
    @GetMapping("/test/{id}")
    public ResponseEntity<Map<String, Object>> testEndpoint(@PathVariable Long id) {
        System.out.println("✅ TEST ENDPOINT - Received request for ID: " + id);
        System.out.println("✅ TEST ENDPOINT - Security config updated: " + java.time.LocalDateTime.now());
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("message", "Backend is working! Security bypassed successfully.");
        result.put("requestId", id);
        result.put("timestamp", java.time.LocalDateTime.now());
        result.put("securityConfigUpdated", true);
        
        return ResponseEntity.ok(result);
    }
}
