package com.springboot.POS.mapper;

import com.springboot.POS.modal.StockMovement;
import com.springboot.POS.payload.dto.StockMovementDTO;

public class StockMovementMapper {

    public static StockMovementDTO toDTO(StockMovement movement) {
        if (movement == null) return null;

        return StockMovementDTO.builder()
                .id(movement.getId())
                .inventoryId(movement.getInventory() != null ? movement.getInventory().getId() : null)
                .productId(movement.getProduct() != null ? movement.getProduct().getId() : null)
                .productName(movement.getProduct() != null ? movement.getProduct().getName() : null)
                .productSku(movement.getProduct() != null ? movement.getProduct().getSku() : null)
                .branchId(movement.getBranch() != null ? movement.getBranch().getId() : null)
                .branchName(movement.getBranch() != null ? movement.getBranch().getName() : null)
                .type(movement.getType())
                .quantityBefore(movement.getQuantityBefore())
                .quantityChanged(movement.getQuantityChanged())
                .quantityAfter(movement.getQuantityAfter())
                .reason(movement.getReason())
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .performedById(movement.getPerformedBy() != null ? movement.getPerformedBy().getId() : null)
                .performedByName(movement.getPerformedBy() != null ? movement.getPerformedBy().getFullName() : null)
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
