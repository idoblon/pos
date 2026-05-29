package com.springboot.POS.service.impl;

import com.springboot.POS.domain.RestockStatus;
import com.springboot.POS.modal.*;
import com.springboot.POS.repository.*;
import com.springboot.POS.service.AutoRestockService;
import com.springboot.POS.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoRestockServiceImpl implements AutoRestockService {

    private final InventoryRepository inventoryRepository;
    private final RestockRequestRepository restockRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final Integer LOW_STOCK_THRESHOLD = 10;
    private static final Integer AUTO_REQUEST_QUANTITY = 50;

    @Override
    @Transactional
    public void checkAndCreateAutoRequests(Long branchId) throws Exception {
        List<Inventory> lowStockItems = inventoryRepository.findLowStockByBranch(branchId, LOW_STOCK_THRESHOLD);

        for (Inventory inventory : lowStockItems) {
            // Check if there's already a pending request for this product
            boolean hasPendingRequest = restockRequestRepository.findByBranchId(branchId).stream()
                    .anyMatch(req -> req.getProduct().getId().equals(inventory.getProduct().getId()) 
                            && req.getStatus() == RestockStatus.PENDING);

            if (!hasPendingRequest) {
                createAutoRequest(inventory);
            }
        }
    }

    @Override
    @Transactional
    public void checkAndCreateAutoRequestsForStore(Long storeId) throws Exception {
        List<Inventory> lowStockItems = inventoryRepository.findLowStockByStore(storeId, LOW_STOCK_THRESHOLD);

        for (Inventory inventory : lowStockItems) {
            // Check if there's already a pending request
            boolean hasPendingRequest = restockRequestRepository.findByBranchId(inventory.getBranch().getId()).stream()
                    .anyMatch(req -> req.getProduct().getId().equals(inventory.getProduct().getId()) 
                            && req.getStatus() == RestockStatus.PENDING);

            if (!hasPendingRequest) {
                createAutoRequest(inventory);
            }
        }
    }

    @Override
    @Transactional
    public void checkInventoryAfterSale(Long inventoryId) throws Exception {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new Exception("Inventory not found"));

        if (inventory.getQuantity() <= LOW_STOCK_THRESHOLD) {
            // Check if there's already a pending request
            boolean hasPendingRequest = restockRequestRepository.findByBranchId(inventory.getBranch().getId()).stream()
                    .anyMatch(req -> req.getProduct().getId().equals(inventory.getProduct().getId()) 
                            && req.getStatus() == RestockStatus.PENDING);

            if (!hasPendingRequest) {
                createAutoRequest(inventory);
            }
        }
    }

    private void createAutoRequest(Inventory inventory) {
        // Find a branch manager or use system user
        List<User> branchManagers = userRepository.findByBranch_IdAndDeletedFalse(inventory.getBranch().getId())
                .stream()
                .filter(u -> u.getRole().name().contains("BRANCH_MANAGER"))
                .toList();

        User requestedBy = branchManagers.isEmpty() ? null : branchManagers.get(0);

        RestockRequest autoRequest = RestockRequest.builder()
                .branch(inventory.getBranch())
                .product(inventory.getProduct())
                .requestedBy(requestedBy)
                .requestedQuantity(AUTO_REQUEST_QUANTITY)
                .currentStock(inventory.getQuantity())
                .notes("Auto-generated: Stock level below threshold (" + LOW_STOCK_THRESHOLD + ")")
                .build();

        restockRequestRepository.save(autoRequest);

        // Send email to store admins
        List<User> storeAdmins = userRepository.findByStore_IdAndDeletedFalse(inventory.getBranch().getStore().getId())
                .stream()
                .filter(u -> u.getRole().name().contains("STORE_ADMIN") || u.getRole().name().contains("STORE_MANAGER"))
                .toList();

        for (User admin : storeAdmins) {
            emailService.sendRestockRequestEmail(
                    admin.getEmail(),
                    admin.getFullName(),
                    inventory.getBranch().getName(),
                    inventory.getProduct().getName(),
                    AUTO_REQUEST_QUANTITY,
                    inventory.getQuantity()
            );
        }

        System.out.println("🤖 Auto-created restock request for " + inventory.getProduct().getName() 
                + " at " + inventory.getBranch().getName());
    }
}
