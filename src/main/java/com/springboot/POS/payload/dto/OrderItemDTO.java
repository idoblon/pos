package com.springboot.POS.payload.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderItemDTO {

    private Long id;

    /** Exact billed amount in the product's selling unit (supports 0.5 kg). */
    private BigDecimal quantity;

    private BigDecimal price;

    private ProductDTO product;

    private Long productId;
    private Long orderId;

    private BigDecimal unitPrice;

    /** Chosen modifiers, e.g. ["Extra cheese", "No onion"]. */
    private List<String> modifiers;

    /** Per-item kitchen instruction. */
    private String kitchenNote;

    /** Per-item pharmacy dosage label. */
    private String dosage;

    /** Captured serials/IMEIs, one per unit. */
    private List<String> serials;

}
