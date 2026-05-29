package com.springboot.POS.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAnalyticsDTO {

    // Summary metrics
    private Integer totalProducts;
    private Integer totalStockValue;
    private Integer lowStockCount;
    private Integer outOfStockCount;
    private Double averageStockLevel;

    // Top products
    private List<ProductStockInfo> topStockedProducts;
    private List<ProductStockInfo> lowStockedProducts;
    private List<ProductStockInfo> mostRequestedProducts;

    // Movement statistics
    private Map<String, Integer> movementsByType;
    private Integer totalRestocks;
    private Integer totalSales;
    private Integer totalAdjustments;

    // Branch comparison
    private List<BranchStockSummary> branchSummaries;

    // Time-based trends
    private List<StockTrendData> stockTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStockInfo {
        private Long productId;
        private String productName;
        private String productSku;
        private Integer totalStock;
        private Integer branchCount;
        private Integer requestCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchStockSummary {
        private Long branchId;
        private String branchName;
        private Integer totalProducts;
        private Integer totalStock;
        private Integer lowStockCount;
        private Integer outOfStockCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockTrendData {
        private String date;
        private Integer totalStock;
        private Integer restockCount;
        private Integer saleCount;
    }
}
