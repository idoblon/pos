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
public class RestockAnalyticsDTO {

    // Summary metrics
    private Integer totalRequests;
    private Integer pendingRequests;
    private Integer approvedRequests;
    private Integer rejectedRequests;
    private Integer fulfilledRequests;

    // Performance metrics
    private Double averageFulfillmentTimeHours;
    private Double approvalRate;
    private Double rejectionRate;

    // Top products
    private List<ProductRequestInfo> mostRequestedProducts;
    private List<ProductRequestInfo> mostRejectedProducts;

    // Branch performance
    private List<BranchRequestSummary> branchSummaries;

    // Time-based trends
    private List<RequestTrendData> requestTrends;
    private Map<String, Integer> requestsByStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRequestInfo {
        private Long productId;
        private String productName;
        private String productSku;
        private Integer requestCount;
        private Integer totalQuantityRequested;
        private Integer approvedCount;
        private Integer rejectedCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchRequestSummary {
        private Long branchId;
        private String branchName;
        private Integer totalRequests;
        private Integer pendingRequests;
        private Integer fulfilledRequests;
        private Double averageFulfillmentTimeHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestTrendData {
        private String date;
        private Integer requestCount;
        private Integer approvedCount;
        private Integer rejectedCount;
        private Integer fulfilledCount;
    }
}
