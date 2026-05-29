package com.springboot.POS.mapper;

import com.springboot.POS.modal.RestockRequest;
import com.springboot.POS.payload.dto.RestockRequestDTO;

public class RestockRequestMapper {

    public static RestockRequestDTO toDTO(RestockRequest request) {
        if (request == null) return null;

        return RestockRequestDTO.builder()
                .id(request.getId())
                .branchId(request.getBranch() != null ? request.getBranch().getId() : null)
                .branchName(request.getBranch() != null ? request.getBranch().getName() : null)
                .productId(request.getProduct() != null ? request.getProduct().getId() : null)
                .productName(request.getProduct() != null ? request.getProduct().getName() : null)
                .productSku(request.getProduct() != null ? request.getProduct().getSku() : null)
                .requestedQuantity(request.getRequestedQuantity())
                .currentStock(request.getCurrentStock())
                .status(request.getStatus())
                .notes(request.getNotes())
                .rejectionReason(request.getRejectionReason())
                .requestedById(request.getRequestedBy() != null ? request.getRequestedBy().getId() : null)
                .requestedByName(request.getRequestedBy() != null ? request.getRequestedBy().getFullName() : null)
                .approvedById(request.getApprovedBy() != null ? request.getApprovedBy().getId() : null)
                .approvedByName(request.getApprovedBy() != null ? request.getApprovedBy().getFullName() : null)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
