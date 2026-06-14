package com.springboot.POS.controller;

import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.InventoryDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.InventoryService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;

    @PostMapping
    public ResponseEntity<InventoryDTO> create(
            @RequestBody InventoryDTO inventoryDTO,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        
        // For branch inventory, check branch access
        if (inventoryDTO.getBranchId() != null) {
            ownershipGuard.requireBranchAccess(user, inventoryDTO.getBranchId());
        }
        // For warehouse inventory, check store access
        else if (inventoryDTO.getStoreId() != null) {
            ownershipGuard.requireStoreAccess(user, inventoryDTO.getStoreId());
        } else {
            throw new Exception("Either branchId or storeId must be provided");
        }
        
        return ResponseEntity.ok(inventoryService.createInventory(inventoryDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryDTO> update(
            @RequestBody InventoryDTO inventoryDTO,
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(inventoryService.updateInventory(id, inventoryDTO));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<InventoryDTO> updateStock(
            @PathVariable Long id,
            @RequestBody InventoryDTO inventoryDTO,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(inventoryService.updateStock(id, inventoryDTO.getQuantity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        inventoryService.deleteInventory(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Inventory deleted");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<InventoryDTO>> getInventoryByBranch(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(inventoryService.getAllInventoryByBranchId(branchId));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<InventoryDTO>> getInventoryByStore(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        System.out.println("CONTROLLER: GET /api/inventories/store/" + storeId);
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        // Use warehouse inventory query which is confirmed working with native SQL
        List<InventoryDTO> warehouse = inventoryService.getWarehouseInventoryByStoreId(storeId);
        List<InventoryDTO> branches = inventoryService.getAllInventoryByStoreId(storeId)
                .stream()
                .filter(i -> i.getBranchId() != null)
                .collect(java.util.stream.Collectors.toList());
        List<InventoryDTO> result = new java.util.ArrayList<>();
        result.addAll(warehouse);
        result.addAll(branches);
        System.out.println("CONTROLLER: Returning " + result.size() + " items (" + warehouse.size() + " warehouse + " + branches.size() + " branch)");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<List<InventoryDTO>> getInventoryByProductAndBranchId(
            @PathVariable Long branchId,
            @PathVariable Long productId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(inventoryService.getInventoryByProductAndBranchId(productId, branchId));
    }

    @GetMapping("/branch/{branchId}/low-stock")
    public ResponseEntity<List<InventoryDTO>> getLowStock(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "10") int threshold,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        return ResponseEntity.ok(inventoryService.getLowStockItems(branchId, threshold));
    }

    @GetMapping("/store/{storeId}/low-stock")
    public ResponseEntity<List<InventoryDTO>> getLowStockByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "10") int threshold,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(inventoryService.getLowStockItemsByStore(storeId, threshold));
    }

    // Warehouse inventory endpoints
    @GetMapping("/warehouse/store/{storeId}")
    public ResponseEntity<List<InventoryDTO>> getWarehouseInventory(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(inventoryService.getWarehouseInventoryByStoreId(storeId));
    }

    @GetMapping("/warehouse/store/{storeId}/product/{productId}")
    public ResponseEntity<InventoryDTO> getWarehouseInventoryByProduct(
            @PathVariable Long storeId,
            @PathVariable Long productId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(inventoryService.getWarehouseInventoryByProductAndStore(productId, storeId));
    }
}
