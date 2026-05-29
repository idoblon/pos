package com.springboot.POS.mapper;

import com.springboot.POS.modal.Inventory;
import com.springboot.POS.payload.dto.InventoryDTO;

public class InventoryMapper {

    private static final Integer DEFAULT_LOW_STOCK_THRESHOLD = 10;

    public static InventoryDTO toDTO(Inventory inventory) {
        if (inventory == null) return null;

        InventoryDTO dto = InventoryDTO.builder()
                .id(inventory.getId())
                .branchId(inventory.getBranch() != null ? inventory.getBranch().getId() : null)
                .branchName(inventory.getBranch() != null ? inventory.getBranch().getName() : null)
                .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                .productSku(inventory.getProduct() != null ? inventory.getProduct().getSku() : null)
                .productImage(inventory.getProduct() != null ? inventory.getProduct().getImage() : null)
                .quantity(inventory.getQuantity())
                .lastUpdate(inventory.getLastUpdate())
                .lowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                .build();

        // Calculate if low stock
        dto.setIsLowStock(inventory.getQuantity() <= DEFAULT_LOW_STOCK_THRESHOLD);

        return dto;
    }
}
