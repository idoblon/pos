package com.springboot.POS.service.impl;

import com.springboot.POS.domain.StockMovementType;
import com.springboot.POS.mapper.StockMovementMapper;
import com.springboot.POS.modal.Inventory;
import com.springboot.POS.modal.StockMovement;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StockMovementDTO;
import com.springboot.POS.repository.InventoryRepository;
import com.springboot.POS.repository.StockMovementRepository;
import com.springboot.POS.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public void recordMovement(Long inventoryId, StockMovementType type, Integer quantityChanged,
                              String reason, String referenceType, Long referenceId, User performedBy) throws Exception {
        
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new Exception("Inventory not found"));

        Integer quantityBefore = inventory.getQuantity();
        Integer quantityAfter = quantityBefore;

        // Calculate quantity after based on movement type
        if (type == StockMovementType.SALE || type == StockMovementType.DAMAGE || type == StockMovementType.TRANSFER_OUT) {
            quantityAfter = quantityBefore - quantityChanged;
        } else {
            quantityAfter = quantityBefore + quantityChanged;
        }

        StockMovement movement = StockMovement.builder()
                .inventory(inventory)
                .product(inventory.getProduct())
                .branch(inventory.getBranch())
                .type(type)
                .quantityBefore(quantityBefore)
                .quantityChanged(quantityChanged)
                .quantityAfter(quantityAfter)
                .reason(reason)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .performedBy(performedBy)
                .build();

        stockMovementRepository.save(movement);
        System.out.println("📊 Stock movement recorded: " + type + " | Product: " + 
                inventory.getProduct().getName() + " | Qty: " + quantityChanged);
    }

    @Override
    public List<StockMovementDTO> getMovementsByBranch(Long branchId) {
        return stockMovementRepository.findByBranchId(branchId).stream()
                .map(StockMovementMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDTO> getMovementsByStore(Long storeId) {
        return stockMovementRepository.findByStoreId(storeId).stream()
                .map(StockMovementMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDTO> getMovementsByProduct(Long productId) {
        return stockMovementRepository.findByProductId(productId).stream()
                .map(StockMovementMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDTO> getMovementsByInventory(Long inventoryId) {
        return stockMovementRepository.findByInventoryId(inventoryId).stream()
                .map(StockMovementMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDTO> getMovementsByBranchAndType(Long branchId, StockMovementType type) {
        return stockMovementRepository.findByBranchIdAndType(branchId, type).stream()
                .map(StockMovementMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDTO> getMovementsByBranchAndDateRange(Long branchId, LocalDateTime startDate, LocalDateTime endDate) {
        return stockMovementRepository.findByBranchIdAndDateRange(branchId, startDate, endDate).stream()
                .map(StockMovementMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockMovementDTO> getMovementsByStoreAndDateRange(Long storeId, LocalDateTime startDate, LocalDateTime endDate) {
        return stockMovementRepository.findByStoreIdAndDateRange(storeId, startDate, endDate).stream()
                .map(StockMovementMapper::toDTO)
                .collect(Collectors.toList());
    }
}
