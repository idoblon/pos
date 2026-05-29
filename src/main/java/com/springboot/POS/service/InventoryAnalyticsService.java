package com.springboot.POS.service;

import com.springboot.POS.payload.dto.InventoryAnalyticsDTO;

import java.time.LocalDateTime;

public interface InventoryAnalyticsService {

    InventoryAnalyticsDTO getStoreAnalytics(Long storeId);

    InventoryAnalyticsDTO getBranchAnalytics(Long branchId);

    InventoryAnalyticsDTO getStoreAnalyticsByDateRange(Long storeId, LocalDateTime startDate, LocalDateTime endDate);

    InventoryAnalyticsDTO getBranchAnalyticsByDateRange(Long branchId, LocalDateTime startDate, LocalDateTime endDate);
}
