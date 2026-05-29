package com.springboot.POS.service;

import com.springboot.POS.payload.dto.RestockAnalyticsDTO;

import java.time.LocalDateTime;

public interface RestockAnalyticsService {

    RestockAnalyticsDTO getStoreAnalytics(Long storeId);

    RestockAnalyticsDTO getBranchAnalytics(Long branchId);

    RestockAnalyticsDTO getStoreAnalyticsByDateRange(Long storeId, LocalDateTime startDate, LocalDateTime endDate);

    RestockAnalyticsDTO getBranchAnalyticsByDateRange(Long branchId, LocalDateTime startDate, LocalDateTime endDate);
}
