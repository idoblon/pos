package com.springboot.POS.service.impl;

import com.springboot.POS.domain.RestockStatus;
import com.springboot.POS.modal.RestockRequest;
import com.springboot.POS.payload.dto.RestockAnalyticsDTO;
import com.springboot.POS.repository.RestockRequestRepository;
import com.springboot.POS.service.RestockAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestockAnalyticsServiceImpl implements RestockAnalyticsService {

    private final RestockRequestRepository restockRequestRepository;

    @Override
    public RestockAnalyticsDTO getStoreAnalytics(Long storeId) {
        List<RestockRequest> requests = restockRequestRepository.findByStoreId(storeId);
        return buildAnalytics(requests);
    }

    @Override
    public RestockAnalyticsDTO getBranchAnalytics(Long branchId) {
        List<RestockRequest> requests = restockRequestRepository.findByBranchId(branchId);
        return buildAnalytics(requests);
    }

    @Override
    public RestockAnalyticsDTO getStoreAnalyticsByDateRange(Long storeId, LocalDateTime startDate, LocalDateTime endDate) {
        List<RestockRequest> allRequests = restockRequestRepository.findByStoreId(storeId);
        List<RestockRequest> requests = allRequests.stream()
                .filter(r -> r.getCreatedAt().isAfter(startDate) && r.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());
        return buildAnalytics(requests);
    }

    @Override
    public RestockAnalyticsDTO getBranchAnalyticsByDateRange(Long branchId, LocalDateTime startDate, LocalDateTime endDate) {
        List<RestockRequest> allRequests = restockRequestRepository.findByBranchId(branchId);
        List<RestockRequest> requests = allRequests.stream()
                .filter(r -> r.getCreatedAt().isAfter(startDate) && r.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());
        return buildAnalytics(requests);
    }

    private RestockAnalyticsDTO buildAnalytics(List<RestockRequest> requests) {
        // Summary metrics
        Integer totalRequests = requests.size();
        Integer pendingRequests = (int) requests.stream().filter(r -> r.getStatus() == RestockStatus.PENDING).count();
        Integer approvedRequests = (int) requests.stream().filter(r -> r.getStatus() == RestockStatus.APPROVED).count();
        Integer rejectedRequests = (int) requests.stream().filter(r -> r.getStatus() == RestockStatus.REJECTED).count();
        Integer fulfilledRequests = (int) requests.stream().filter(r -> r.getStatus() == RestockStatus.FULFILLED).count();

        // Performance metrics
        Double averageFulfillmentTime = requests.stream()
                .filter(r -> r.getStatus() == RestockStatus.FULFILLED && r.getUpdatedAt() != null)
                .mapToDouble(r -> Duration.between(r.getCreatedAt(), r.getUpdatedAt()).toHours())
                .average()
                .orElse(0.0);

        Double approvalRate = totalRequests > 0 ? (approvedRequests + fulfilledRequests) * 100.0 / totalRequests : 0.0;
        Double rejectionRate = totalRequests > 0 ? rejectedRequests * 100.0 / totalRequests : 0.0;

        // Most requested products
        Map<Long, List<RestockRequest>> requestsByProduct = requests.stream()
                .collect(Collectors.groupingBy(r -> r.getProduct().getId()));

        List<RestockAnalyticsDTO.ProductRequestInfo> mostRequested = requestsByProduct.entrySet().stream()
                .map(entry -> {
                    List<RestockRequest> productRequests = entry.getValue();
                    return RestockAnalyticsDTO.ProductRequestInfo.builder()
                            .productId(entry.getKey())
                            .productName(productRequests.get(0).getProduct().getName())
                            .productSku(productRequests.get(0).getProduct().getSku())
                            .requestCount(productRequests.size())
                            .totalQuantityRequested(productRequests.stream().mapToInt(RestockRequest::getRequestedQuantity).sum())
                            .approvedCount((int) productRequests.stream().filter(r -> r.getStatus() == RestockStatus.APPROVED || r.getStatus() == RestockStatus.FULFILLED).count())
                            .rejectedCount((int) productRequests.stream().filter(r -> r.getStatus() == RestockStatus.REJECTED).count())
                            .build();
                })
                .sorted(Comparator.comparingInt(RestockAnalyticsDTO.ProductRequestInfo::getRequestCount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // Most rejected products
        List<RestockAnalyticsDTO.ProductRequestInfo> mostRejected = requestsByProduct.entrySet().stream()
                .map(entry -> {
                    List<RestockRequest> productRequests = entry.getValue();
                    int rejectedCount = (int) productRequests.stream().filter(r -> r.getStatus() == RestockStatus.REJECTED).count();
                    return RestockAnalyticsDTO.ProductRequestInfo.builder()
                            .productId(entry.getKey())
                            .productName(productRequests.get(0).getProduct().getName())
                            .productSku(productRequests.get(0).getProduct().getSku())
                            .requestCount(productRequests.size())
                            .rejectedCount(rejectedCount)
                            .build();
                })
                .filter(p -> p.getRejectedCount() > 0)
                .sorted(Comparator.comparingInt(RestockAnalyticsDTO.ProductRequestInfo::getRejectedCount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // Branch summaries
        Map<Long, List<RestockRequest>> requestsByBranch = requests.stream()
                .collect(Collectors.groupingBy(r -> r.getBranch().getId()));

        List<RestockAnalyticsDTO.BranchRequestSummary> branchSummaries = requestsByBranch.entrySet().stream()
                .map(entry -> {
                    List<RestockRequest> branchRequests = entry.getValue();
                    Double branchAvgTime = branchRequests.stream()
                            .filter(r -> r.getStatus() == RestockStatus.FULFILLED && r.getUpdatedAt() != null)
                            .mapToDouble(r -> Duration.between(r.getCreatedAt(), r.getUpdatedAt()).toHours())
                            .average()
                            .orElse(0.0);

                    return RestockAnalyticsDTO.BranchRequestSummary.builder()
                            .branchId(entry.getKey())
                            .branchName(branchRequests.get(0).getBranch().getName())
                            .totalRequests(branchRequests.size())
                            .pendingRequests((int) branchRequests.stream().filter(r -> r.getStatus() == RestockStatus.PENDING).count())
                            .fulfilledRequests((int) branchRequests.stream().filter(r -> r.getStatus() == RestockStatus.FULFILLED).count())
                            .averageFulfillmentTimeHours(branchAvgTime)
                            .build();
                })
                .collect(Collectors.toList());

        // Request trends
        Map<String, List<RestockRequest>> requestsByDate = requests.stream()
                .collect(Collectors.groupingBy(r -> 
                        r.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                ));

        List<RestockAnalyticsDTO.RequestTrendData> requestTrends = requestsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<RestockRequest> dayRequests = entry.getValue();
                    return RestockAnalyticsDTO.RequestTrendData.builder()
                            .date(entry.getKey())
                            .requestCount(dayRequests.size())
                            .approvedCount((int) dayRequests.stream().filter(r -> r.getStatus() == RestockStatus.APPROVED).count())
                            .rejectedCount((int) dayRequests.stream().filter(r -> r.getStatus() == RestockStatus.REJECTED).count())
                            .fulfilledCount((int) dayRequests.stream().filter(r -> r.getStatus() == RestockStatus.FULFILLED).count())
                            .build();
                })
                .collect(Collectors.toList());

        // Requests by status
        Map<String, Integer> requestsByStatus = requests.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getStatus().name(),
                        Collectors.summingInt(r -> 1)
                ));

        return RestockAnalyticsDTO.builder()
                .totalRequests(totalRequests)
                .pendingRequests(pendingRequests)
                .approvedRequests(approvedRequests)
                .rejectedRequests(rejectedRequests)
                .fulfilledRequests(fulfilledRequests)
                .averageFulfillmentTimeHours(averageFulfillmentTime)
                .approvalRate(approvalRate)
                .rejectionRate(rejectionRate)
                .mostRequestedProducts(mostRequested)
                .mostRejectedProducts(mostRejected)
                .branchSummaries(branchSummaries)
                .requestTrends(requestTrends)
                .requestsByStatus(requestsByStatus)
                .build();
    }
}
