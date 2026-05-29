package com.springboot.POS.controller;

import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.RestockAnalyticsDTO;
import com.springboot.POS.service.RestockAnalyticsService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics/restock")
public class RestockAnalyticsController {

    private final RestockAnalyticsService restockAnalyticsService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;

    @GetMapping("/store/{storeId}")
    public ResponseEntity<RestockAnalyticsDTO> getStoreAnalytics(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(restockAnalyticsService.getStoreAnalytics(storeId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<RestockAnalyticsDTO> getBranchAnalytics(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(restockAnalyticsService.getBranchAnalytics(branchId));
    }

    @GetMapping("/store/{storeId}/date-range")
    public ResponseEntity<RestockAnalyticsDTO> getStoreAnalyticsByDateRange(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(restockAnalyticsService.getStoreAnalyticsByDateRange(storeId, startDate, endDate));
    }

    @GetMapping("/branch/{branchId}/date-range")
    public ResponseEntity<RestockAnalyticsDTO> getBranchAnalyticsByDateRange(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(restockAnalyticsService.getBranchAnalyticsByDateRange(branchId, startDate, endDate));
    }
}
