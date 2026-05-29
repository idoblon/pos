package com.springboot.POS.service;

import com.springboot.POS.domain.StockMovementType;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StockMovementDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementService {

    void recordMovement(Long inventoryId, StockMovementType type, Integer quantityChanged, 
                       String reason, String referenceType, Long referenceId, User performedBy) throws Exception;

    List<StockMovementDTO> getMovementsByBranch(Long branchId);

    List<StockMovementDTO> getMovementsByStore(Long storeId);

    List<StockMovementDTO> getMovementsByProduct(Long productId);

    List<StockMovementDTO> getMovementsByInventory(Long inventoryId);

    List<StockMovementDTO> getMovementsByBranchAndType(Long branchId, StockMovementType type);

    List<StockMovementDTO> getMovementsByBranchAndDateRange(Long branchId, LocalDateTime startDate, LocalDateTime endDate);

    List<StockMovementDTO> getMovementsByStoreAndDateRange(Long storeId, LocalDateTime startDate, LocalDateTime endDate);
}
