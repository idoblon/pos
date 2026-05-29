package com.springboot.POS.controller;

import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.InventoryAnalyticsDTO;
import com.springboot.POS.service.InventoryAnalyticsService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics/inventory")
public class InventoryAnalyticsController {

    private final InventoryAnalyticsService inventoryAnalyticsService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;

    @GetMapping("/store/{storeId}")
    public ResponseEntity<InventoryAnalyticsDTO> getStoreAnalytics(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(inventoryAnalyticsService.getStoreAnalytics(storeId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<InventoryAnalyticsDTO> getBranchAnalytics(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(inventoryAnalyticsService.getBranchAnalytics(branchId));
    }

    @GetMapping("/store/{storeId}/date-range")
    public ResponseEntity<InventoryAnalyticsDTO> getStoreAnalyticsByDateRange(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(inventoryAnalyticsService.getStoreAnalyticsByDateRange(storeId, startDate, endDate));
    }

    @GetMapping("/branch/{branchId}/date-range")
    public ResponseEntity<InventoryAnalyticsDTO> getBranchAnalyticsByDateRange(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(inventoryAnalyticsService.getBranchAnalyticsByDateRange(branchId, startDate, endDate));
    }
}
