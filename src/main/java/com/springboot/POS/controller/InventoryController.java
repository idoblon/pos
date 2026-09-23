package com.springboot.POS.controller;

import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.InventoryDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.repository.InventoryRepository;
import com.springboot.POS.service.InventoryService;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserService userService;
    private final OwnershipGuard ownershipGuard;
    private final InventoryRepository inventoryRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
    public ResponseEntity<InventoryDTO> update(
            @RequestBody InventoryDTO inventoryDTO,
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        requireInventoryAccess(user, id);
        return ResponseEntity.ok(inventoryService.updateInventory(id, inventoryDTO));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
    public ResponseEntity<InventoryDTO> updateStock(
            @PathVariable Long id,
            @RequestBody InventoryDTO inventoryDTO,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        requireInventoryAccess(user, id);
        return ResponseEntity.ok(inventoryService.updateStock(id, inventoryDTO.getQuantity()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
    public ResponseEntity<ApiResponse> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        requireInventoryAccess(user, id);
        inventoryService.deleteInventory(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Inventory deleted");
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Atomic warehouse → branch transfer. Guards both the warehouse row's
     * store and the destination branch before delegating to the transactional
     * service method.
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER')")
    public ResponseEntity<InventoryDTO> transfer(
            @RequestBody TransferRequest request,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        requireInventoryAccess(user, request.getWarehouseInventoryId());
        ownershipGuard.requireBranchAccess(user, request.getToBranchId());
        return ResponseEntity.ok(inventoryService.transferStock(
                request.getWarehouseInventoryId(), request.getToBranchId(),
                request.getQuantity(), user));
    }

    private void requireInventoryAccess(User user, Long inventoryId) throws Exception {
        com.springboot.POS.modal.Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new Exception("inventory not found...."));
        if (inventory.getBranch() != null) {
            ownershipGuard.requireBranchAccess(user, inventory.getBranch().getId());
        } else if (inventory.getStore() != null) {
            ownershipGuard.requireStoreAccess(user, inventory.getStore().getId());
        } else {
            throw new Exception("Inventory has no store or branch scope");
        }
    }

    public static class TransferRequest {
        private Long warehouseInventoryId;
        private Long toBranchId;
        private int quantity;
        public Long getWarehouseInventoryId() { return warehouseInventoryId; }
        public void setWarehouseInventoryId(Long v) { this.warehouseInventoryId = v; }
        public Long getToBranchId() { return toBranchId; }
        public void setToBranchId(Long v) { this.toBranchId = v; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int v) { this.quantity = v; }
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
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<List<InventoryDTO>> getInventoryByStore(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        log.debug("GET /api/inventories/store/{}", storeId);
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
        log.debug("Returning {} items ({} warehouse + {} branch)", result.size(), warehouse.size(), branches.size());
        return ResponseEntity.ok(result);
    }

    /**
     * Server-joined POS catalog for the cashier terminal: one call returns
     * branch inventory rows enriched with product details, with server-side
     * search and pagination. Replaces the terminal's double fetch + client
     * join (and its duplicate-row workaround).
     */
    @GetMapping("/branch/{branchId}/pos-catalog")
    public ResponseEntity<java.util.Map<String, Object>> getPosCatalog(
            @PathVariable Long branchId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);
        List<com.springboot.POS.modal.Inventory> rows =
                inventoryRepository.findByBranchId(branchId);
        String query = q != null ? q.trim().toLowerCase() : "";
        List<java.util.Map<String, Object>> items = rows.stream()
                .filter(i -> i.getProduct() != null && !Boolean.TRUE.equals(i.getProduct().getDeleted()))
                .map(i -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", i.getProduct().getId());
                    m.put("name", i.getProduct().getName());
                    m.put("sku", i.getProduct().getSku());
                    m.put("sellingPrice", i.getProduct().getSellingPrice());
                    m.put("mrp", i.getProduct().getMrp());
                    m.put("brand", i.getProduct().getBrand());
                    m.put("image", i.getProduct().getImage());
                    m.put("categoryName", i.getProduct().getCategory() != null
                            ? i.getProduct().getCategory().getName() : null);
                    m.put("description", i.getProduct().getDescription());
                    m.put("stock", i.getQuantity());
                    m.put("unitPrice", i.getUnitPrice());
                    return m;
                })
                .filter(m -> query.isEmpty()
                        || String.valueOf(m.get("name")).toLowerCase().contains(query)
                        || String.valueOf(m.get("sku")).toLowerCase().contains(query)
                        || (m.get("categoryName") != null
                                && String.valueOf(m.get("categoryName")).toLowerCase().contains(query)))
                .sorted(java.util.Comparator.comparing(m -> String.valueOf(m.get("name"))))
                .collect(java.util.stream.Collectors.toList());
        // Merge accidental duplicate rows per product (server-side fix for
        // the workaround the terminal used to carry client-side).
        java.util.Map<Object, java.util.Map<String, Object>> merged = new java.util.LinkedHashMap<>();
        for (java.util.Map<String, Object> m : items) {
            java.util.Map<String, Object> existing = merged.get(m.get("id"));
            if (existing == null) {
                merged.put(m.get("id"), m);
            } else {
                existing.put("stock", Math.max(
                        ((Number) existing.getOrDefault("stock", 0)).intValue(),
                        ((Number) m.getOrDefault("stock", 0)).intValue()));
            }
        }
        List<java.util.Map<String, Object>> all = new java.util.ArrayList<>(merged.values());
        int safeSize = Math.min(Math.max(size, 1), 200);
        int from = Math.min(page * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("content", all.subList(from, to));
        body.put("page", page);
        body.put("size", safeSize);
        body.put("totalElements", all.size());
        return ResponseEntity.ok(body);
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
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<List<InventoryDTO>> getWarehouseInventory(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(inventoryService.getWarehouseInventoryByStoreId(storeId));
    }

    @GetMapping("/warehouse/store/{storeId}/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','STORE_MANAGER','BRANCH_MANAGER')")
    public ResponseEntity<InventoryDTO> getWarehouseInventoryByProduct(
            @PathVariable Long storeId,
            @PathVariable Long productId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);
        return ResponseEntity.ok(inventoryService.getWarehouseInventoryByProductAndStore(productId, storeId));
    }
}
