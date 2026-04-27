package com.springboot.POS.payload.dto;

import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryDTO {


    private Long id;

    private Branch branch;
    private Long branchId;

    private ProductDTO product;
    private Long productId;

    private Integer quantity;

    private LocalDateTime lastUpdate;
}
