package com.springboot.POS.service.impl;

import com.springboot.POS.domain.StockMovementType;
import com.springboot.POS.modal.Inventory;
import com.springboot.POS.modal.StockMovement;
import com.springboot.POS.payload.dto.InventoryAnalyticsDTO;
import com.springboot.POS.repository.InventoryRepository;
import com.springboot.POS.repository.StockMovementRepository;
import com.springboot.POS.service.InventoryAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryAnalyticsServiceImpl implements InventoryAnalyticsService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private static final Integer LOW_STOCK_THRESHOLD = 10;

    @Override
    public InventoryAnalyticsDTO getStoreAnalytics(Long storeId) {
        List<Inventory> inventories = inventoryRepository.findByStoreId(storeId);
        List<StockMovement> movements = stockMovementRepository.findByStoreId(storeId);
        
        return buildAnalytics(inventories, movements);
    }

    @Override
    public InventoryAnalyticsDTO getBranchAnalytics(Long branchId) {
        List<Inventory> inventories = inventoryRepository.findByBranchId(branchId);
        List<StockMovement> movements = stockMovementRepository.findByBranchId(branchId);
        
        return buildAnalytics(inventories, movements);
    }

    @Override
    public InventoryAnalyticsDTO getStoreAnalyticsByDateRange(Long storeId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Inventory> inventories = inventoryRepository.findByStoreId(storeId);
        List<StockMovement> movements = stockMovementRepository.findByStoreIdAndDateRange(storeId, startDate, endDate);
        
        return buildAnalytics(inventories, movements);
    }

    @Override
    public InventoryAnalyticsDTO getBranchAnalyticsByDateRange(Long branchId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Inventory> inventories = inventoryRepository.findByBranchId(branchId);
        List<StockMovement> movements = stockMovementRepository.findByBranchIdAndDateRange(branchId, startDate, endDate);
        
        return buildAnalytics(inventories, movements);
    }

    private InventoryAnalyticsDTO buildAnalytics(List<Inventory> inventories, List<StockMovement> movements) {
        // Summary metrics
        Integer totalProducts = inventories.size();
        Integer lowStockCount = (int) inventories.stream().filter(i -> i.getQuantity() <= LOW_STOCK_THRESHOLD).count();
        Integer outOfStockCount = (int) inventories.stream().filter(i -> i.getQuantity() == 0).count();
        Double averageStockLevel = inventories.stream().mapToInt(Inventory::getQuantity).average().orElse(0.0);

        // Top stocked products
        List<InventoryAnalyticsDTO.ProductStockInfo> topStocked = inventories.stream()
                .sorted(Comparator.comparingInt(Inventory::getQuantity).reversed())
                .limit(10)
                .map(i -> InventoryAnalyticsDTO.ProductStockInfo.builder()
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .productSku(i.getProduct().getSku())
                        .totalStock(i.getQuantity())
                        .build())
                .collect(Collectors.toList());

        // Low stocked products
        List<InventoryAnalyticsDTO.ProductStockInfo> lowStocked = inventories.stream()
                .filter(i -> i.getQuantity() <= LOW_STOCK_THRESHOLD && i.getQuantity() > 0)
                .sorted(Comparator.comparingInt(Inventory::getQuantity))
                .limit(10)
                .map(i -> InventoryAnalyticsDTO.ProductStockInfo.builder()
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .productSku(i.getProduct().getSku())
                        .totalStock(i.getQuantity())
                        .build())
                .collect(Collectors.toList());

        // Movements by type
        Map<String, Integer> movementsByType = movements.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getType().name(),
                        Collectors.summingInt(StockMovement::getQuantityChanged)
                ));

        Integer totalRestocks = movementsByType.getOrDefault(StockMovementType.RESTOCK.name(), 0);
        Integer totalSales = movementsByType.getOrDefault(StockMovementType.SALE.name(), 0);
        Integer totalAdjustments = movementsByType.getOrDefault(StockMovementType.MANUAL_ADJUSTMENT.name(), 0);

        // Branch summaries
        Map<Long, List<Inventory>> inventoriesByBranch = inventories.stream()
                .collect(Collectors.groupingBy(i -> i.getBranch().getId()));

        List<InventoryAnalyticsDTO.BranchStockSummary> branchSummaries = inventoriesByBranch.entrySet().stream()
                .map(entry -> {
                    List<Inventory> branchInventories = entry.getValue();
                    return InventoryAnalyticsDTO.BranchStockSummary.builder()
                            .branchId(entry.getKey())
                            .branchName(branchInventories.get(0).getBranch().getName())
                            .totalProducts(branchInventories.size())
                            .totalStock(branchInventories.stream().mapToInt(Inventory::getQuantity).sum())
                            .lowStockCount((int) branchInventories.stream().filter(i -> i.getQuantity() <= LOW_STOCK_THRESHOLD).count())
                            .outOfStockCount((int) branchInventories.stream().filter(i -> i.getQuantity() == 0).count())
                            .build();
                })
                .collect(Collectors.toList());

        // Stock trends (last 7 days)
        Map<String, List<StockMovement>> movementsByDate = movements.stream()
                .collect(Collectors.groupingBy(m -> 
                        m.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                ));

        List<InventoryAnalyticsDTO.StockTrendData> stockTrends = movementsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<StockMovement> dayMovements = entry.getValue();
                    return InventoryAnalyticsDTO.StockTrendData.builder()
                            .date(entry.getKey())
                            .totalStock(dayMovements.stream().mapToInt(StockMovement::getQuantityAfter).sum())
                            .restockCount((int) dayMovements.stream().filter(m -> m.getType() == StockMovementType.RESTOCK).count())
                            .saleCount((int) dayMovements.stream().filter(m -> m.getType() == StockMovementType.SALE).count())
                            .build();
                })
                .collect(Collectors.toList());

        return InventoryAnalyticsDTO.builder()
                .totalProducts(totalProducts)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .averageStockLevel(averageStockLevel)
                .topStockedProducts(topStocked)
                .lowStockedProducts(lowStocked)
                .movementsByType(movementsByType)
                .totalRestocks(totalRestocks)
                .totalSales(totalSales)
                .totalAdjustments(totalAdjustments)
                .branchSummaries(branchSummaries)
                .stockTrends(stockTrends)
                .build();
    }
}
