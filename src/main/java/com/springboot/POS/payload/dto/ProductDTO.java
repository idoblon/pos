package com.springboot.POS.payload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String desciption;

    @Positive(message = "MRP must be positive")
    private Double mrp;

    @NotNull(message = "Selling price is required")
    @Positive(message = "Selling price must be positive")
    private Double sellingPrice;

    private String brand;
    private String image;

    private CategoryDTO category;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Store is required")
    private Long storeId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
