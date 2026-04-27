package com.springboot.POS.mapper;

import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.Inventory;
import com.springboot.POS.modal.Product;
import com.springboot.POS.payload.dto.InventoryDTO;

public class InventoryMapper {
    public static InventoryDTO toDTO(Inventory inventory){
        return InventoryDTO.builder()
                .id(inventory.getId())
                .branchId(inventory.getBranch().getId())
                .productId(inventory.getProduct().getId())
                .product(ProductMapper.toDTO(inventory.getProduct()))
                .quantity(inventory.getQuantity())
                .build();
    }
    public static Inventory toEntity(InventoryDTO inventoryDTO,
                                     Branch branch,
                                     Product product){
        return Inventory.builder()
                .branch(branch)
                .product(product)
                .quantity(inventoryDTO.getQuantity())
                .build();
    }
}
