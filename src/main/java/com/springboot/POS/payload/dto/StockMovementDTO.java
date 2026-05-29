package com.springboot.POS.payload.dto;

import com.springboot.POS.domain.StockMovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDTO {

    private Long id;
    private Long inventoryId;
    private Long productId;
    private String productName;
    private String productSku;
    private Long branchId;
    private String branchName;
    private StockMovementType type;
    private Integer quantityBefore;
    private Integer quantityChanged;
    private Integer quantityAfter;
    private String reason;
    private String referenceType;
    private Long referenceId;
    private Long performedById;
    private String performedByName;
    private LocalDateTime createdAt;
}
