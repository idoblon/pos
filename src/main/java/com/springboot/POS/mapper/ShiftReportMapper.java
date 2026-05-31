package com.springboot.POS.mapper;

import com.springboot.POS.modal.Order;
import com.springboot.POS.modal.Product;
import com.springboot.POS.modal.Refund;
import com.springboot.POS.modal.ShiftReport;
import com.springboot.POS.modal.Branch;
import com.springboot.POS.payload.dto.BranchDTO;
import com.springboot.POS.payload.dto.OrderDTO;
import com.springboot.POS.payload.dto.ProductDTO;
import com.springboot.POS.payload.dto.RefundDTO;
import com.springboot.POS.payload.dto.ShiftReportDTO;
import com.springboot.POS.payload.dto.UserDTO;

import java.util.List;
import java.util.stream.Collectors;

public class ShiftReportMapper {
    public static ShiftReportDTO toDTO(ShiftReport entity){
        if (entity == null) return null;
        return ShiftReportDTO.builder()
                .id(entity.getId())
                .shiftStart(entity.getShiftStart())
                .shiftEnd(entity.getShiftEnd())
                .totalSales(entity.getTotalSales())
                .totalRefunds(entity.getTotalRefunds())
                .netSale(entity.getNetSale())
                .totalOrders(entity.getTotalOrders())
                .cashier(entity.getCashier() != null ? UserMapper.toDTO(entity.getCashier()) : null)
                .cashierId(entity.getCashier() != null ? entity.getCashier().getId() : null)
                .branch(entity.getBranch() != null ? BranchMapper.toDTO(entity.getBranch()) : null)
                .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)
                .paymentSummaries(entity.getPaymentSummaries())
                .topSellingProducts(mapProducts(entity.getTopSellingProducts()))
                .recentOrders(mapOrders(entity.getRecentOrders()))
                .refunds(mapRefunds(entity.getRefunds()))
                .openingFloat(entity.getOpeningFloat())
                .declaredCash(entity.getDeclaredCash())
                .expectedCash(entity.getExpectedCash())
                .cashDiscrepancy(entity.getCashDiscrepancy())
                .reconciliationStatus(entity.getReconciliationStatus())
                .build();
    }

    private static List<RefundDTO> mapRefunds(List<Refund> refunds) {
        if (refunds == null || refunds.isEmpty()) {
            return null;
        }
        return refunds.stream().map(RefundMapper::toDTO).collect(Collectors.toList());
    }

    private static List<ProductDTO> mapProducts(List<Product> topSellingProducts) {
        if (topSellingProducts == null || topSellingProducts.isEmpty()) {
            return null;
        }
        return topSellingProducts.stream().map(ProductMapper::toDTO).collect(Collectors.toList());
    }

    private static List<OrderDTO> mapOrders(List<Order> recentOrders) {
        if (recentOrders == null || recentOrders.isEmpty()){
            return null;
        }
        return recentOrders.stream().map(OrderMapper::toDTO).collect(Collectors.toList());
    }

}
