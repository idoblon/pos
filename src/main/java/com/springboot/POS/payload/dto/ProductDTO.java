package com.springboot.POS.payload.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    private String description;

    @Positive(message = "MRP must be positive")
    private BigDecimal mrp;

    @NotNull(message = "Selling price is required")
    @Positive(message = "Selling price must be positive")
    private BigDecimal sellingPrice;

    private String brand;
    private String image;

    private CategoryDTO category;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Store is required")
    private Long storeId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Vertical store-type attributes (all optional) ──

    @JsonAlias("expiry")
    private LocalDate expiryDate;

    @JsonAlias("batch")
    private String batchNumber;

    @JsonAlias("requiresPrescription")
    private Boolean prescriptionRequired;

    @JsonAlias("isControlled")
    private Boolean controlledSubstance;

    private String dosage;

    private String unit;

    private BigDecimal weight;

    private BigDecimal weightStep;

    @JsonAlias("minOrderQty")
    private Integer moq;

    @JsonAlias("serialRequired")
    private Boolean requiresSerial;

    @JsonAlias("warranty")
    private Integer warrantyMonths;

    @JsonAlias("size")
    private String sizeVariant;

    @JsonAlias("color")
    private String colorVariant;

    /** Variant matrix [{sku,size,color,price,stock}]. */
    private List<java.util.Map<String, Object>> variants;

    private Integer bulkMinQty;

    private BigDecimal bulkPrice;

    /** Multi-tier wholesale pricing. */
    @Valid
    private List<BulkTierDTO> bulkTiers;

    private Integer preparationTime;

    private String kitchenStation;

    @JsonAlias("modifierOptions")
    private List<String> modifiers;

    private Boolean isVeg;

    private String careInstructions;

    private Integer guaranteeDays;

}
