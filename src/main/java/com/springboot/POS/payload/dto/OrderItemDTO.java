package com.springboot.POS.payload.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemDTO {

    private Long id;

    private Integer quantity;

    private BigDecimal price;

    private ProductDTO product;

    private Long productId;
    private Long orderId;

    private BigDecimal unitPrice;

}
