package com.springboot.POS.controller;

import com.springboot.POS.domain.StockMovementType;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StockMovementDTO;
import com.springboot.POS.service.StockMovementService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock-movements")
public class StockMovementController {

    private final StockMovementService stockMovementService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<StockMovementDTO>> getByBranch(
            @PathVariable Long branchId,
            @RequestParam(required = false) StockMovementType type,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);

        if (type != null) {
            return ResponseEntity.ok(stockMovementService.getMovementsByBranchAndType(branchId, type));
        }
        return ResponseEntity.ok(stockMovementService.getMovementsByBranch(branchId));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<StockMovementDTO>> getByStore(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(stockMovementService.getMovementsByStore(storeId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockMovementDTO>> getByProduct(
            @PathVariable Long productId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(stockMovementService.getMovementsByProduct(productId));
    }

    @GetMapping("/inventory/{inventoryId}")
    public ResponseEntity<List<StockMovementDTO>> getByInventory(
            @PathVariable Long inventoryId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(stockMovementService.getMovementsByInventory(inventoryId));
    }

    @GetMapping("/branch/{branchId}/date-range")
    public ResponseEntity<List<StockMovementDTO>> getByBranchAndDateRange(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(stockMovementService.getMovementsByBranchAndDateRange(branchId, startDate, endDate));
    }

    @GetMapping("/store/{storeId}/date-range")
    public ResponseEntity<List<StockMovementDTO>> getByStoreAndDateRange(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(stockMovementService.getMovementsByStoreAndDateRange(storeId, startDate, endDate));
    }
}
