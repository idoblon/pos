package com.springboot.POS.service;

import com.springboot.POS.payload.dto.InventoryDTO;

import java.util.List;

public interface InventoryService {
    InventoryDTO createInventory(InventoryDTO inventoryDTO) throws Exception;
    InventoryDTO updateInventory(Long id, InventoryDTO inventoryDTO) throws Exception;
    InventoryDTO updateStock(Long id, Integer quantity) throws Exception;
    void deleteInventory(Long id) throws Exception;
    InventoryDTO getInventoryById(Long id) throws Exception;
    List<InventoryDTO> getInventoryByProductAndBranchId(Long productId, Long branchId);
    List<InventoryDTO> getAllInventoryByBranchId(Long branchId);
    List<InventoryDTO> getAllInventoryByStoreId(Long storeId);
    
    // Warehouse inventory methods
    List<InventoryDTO> getWarehouseInventoryByStoreId(Long storeId);
    InventoryDTO getWarehouseInventoryByProductAndStore(Long productId, Long storeId) throws Exception;
    
    void deductStock(Long productId, Long branchId, int quantity) throws Exception;
    void addStock(Long productId, Long branchId, int quantity) throws Exception;
    List<InventoryDTO> getLowStockItems(Long branchId, int threshold);
    List<InventoryDTO> getLowStockItemsByStore(Long storeId, int threshold);
}
