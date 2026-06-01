package com.springboot.POS.payload.dto;

import com.springboot.POS.domain.RestockStatus;
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
public class RestockRequestDTO {

    private Long id;

    @NotNull(message = "Branch is required")
    private Long branchId;
    
    private String branchName;

    @NotNull(message = "Product is required")
    private Long productId;
    
    private String productName;
    private String productSku;

    @NotNull(message = "Requested quantity is required")
    @Positive(message = "Requested quantity must be positive")
    private Integer requestedQuantity;

    private Integer receivedQuantity; // Actual quantity received during fulfillment

    private Integer currentStock;
    private RestockStatus status;
    private String notes;
    private String rejectionReason;

    private Long requestedById;
    private String requestedByName;
    
    private Long approvedById;
    private String approvedByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
