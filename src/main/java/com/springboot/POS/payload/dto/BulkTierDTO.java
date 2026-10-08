package com.springboot.POS.payload.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** One wholesale tier: orders of {@code minQty}+ units bill at {@code price} each. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTierDTO {

    @Min(value = 2, message = "Bulk tier quantity must be at least 2")
    private Integer minQty;

    @Positive(message = "Bulk tier price must be positive")
    private BigDecimal price;
}
