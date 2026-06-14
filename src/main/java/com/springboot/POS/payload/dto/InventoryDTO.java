package com.springboot.POS.payload.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDTO {

    private Long id;

    // NULL = Warehouse inventory, NOT NULL = Branch inventory
    private Long branchId;
    
    private String branchName;

    @NotNull(message = "Product is required")
    private Long productId;
    
    private String productName;
    private String productSku;
    private String productImage;
    private Long categoryId;
    private String categoryName;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    // Unit price for value calculation
    private Double unitPrice;

    // Store ID (for warehouse inventory when branchId is null)
    private Long storeId;

    private LocalDateTime lastUpdate;
    
    // Helper fields for UI
    private Boolean isLowStock;
    private Integer lowStockThreshold;
}
