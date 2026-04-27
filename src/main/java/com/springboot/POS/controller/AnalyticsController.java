package com.springboot.POS.controller;

import com.springboot.POS.modal.User;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
public class AnalyticsController {
    
    private final UserService userService;

    @GetMapping("/store/{storeId}")
    public ResponseEntity<Map<String, Object>> getStoreAnalytics(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        
        // Mock analytics data - replace with actual service implementation
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalOrders", 150);
        analytics.put("totalSales", 45000.0);
        analytics.put("totalCustomers", 85);
        analytics.put("averageOrderValue", 300.0);
        analytics.put("period", "all-time");
        
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/store/{storeId}/range")
    public ResponseEntity<Map<String, Object>> getStoreAnalyticsByDateRange(
            @PathVariable Long storeId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        
        // Mock analytics data for date range
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalOrders", 45);
        analytics.put("totalSales", 12000.0);
        analytics.put("totalCustomers", 32);
        analytics.put("averageOrderValue", 266.67);
        analytics.put("startDate", startDate);
        analytics.put("endDate", endDate);
        
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<Map<String, Object>> getBranchAnalytics(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        
        // Mock branch analytics data
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalOrders", 75);
        analytics.put("totalSales", 22500.0);
        analytics.put("totalCustomers", 45);
        analytics.put("averageOrderValue", 300.0);
        analytics.put("branchId", branchId);
        
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/store/{storeId}/sales-chart")
    public ResponseEntity<Map<String, Object>[]> getSalesChart(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "monthly") String period,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        
        // Mock sales chart data
        Map<String, Object>[] chartData = new Map[6];
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        double[] sales = {5000, 7500, 6200, 8100, 9300, 7800};
        
        for (int i = 0; i < 6; i++) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("period", months[i]);
            dataPoint.put("sales", sales[i]);
            dataPoint.put("orders", (int)(sales[i] / 300)); // Mock order count
            chartData[i] = dataPoint;
        }
        
        return ResponseEntity.ok(chartData);
    }

    @GetMapping("/store/{storeId}/top-products")
    public ResponseEntity<Map<String, Object>[]> getTopProducts(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        
        // Mock top products data
        Map<String, Object>[] topProducts = new Map[Math.min(limit, 5)];
        String[] productNames = {"Notebook A5", "Pen Blue", "Marker Set", "Stapler", "Paper Clips"};
        int[] quantities = {45, 38, 32, 28, 25};
        double[] revenues = {2250, 760, 960, 840, 125};
        
        for (int i = 0; i < topProducts.length; i++) {
            Map<String, Object> product = new HashMap<>();
            product.put("productName", productNames[i]);
            product.put("quantitySold", quantities[i]);
            product.put("revenue", revenues[i]);
            product.put("rank", i + 1);
            topProducts[i] = product;
        }
        
        return ResponseEntity.ok(topProducts);
    }

    @GetMapping("/store/{storeId}/payment-summary")
    public ResponseEntity<Map<String, Object>[]> getPaymentSummary(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        
        User user = userService.getUserFromJwtToken(jwt);
        
        // Mock payment summary data
        Map<String, Object>[] paymentSummary = new Map[3];
        
        Map<String, Object> cash = new HashMap<>();
        cash.put("paymentType", "CASH");
        cash.put("amount", 18000.0);
        cash.put("percentage", 60.0);
        cash.put("count", 90);
        
        Map<String, Object> card = new HashMap<>();
        card.put("paymentType", "CARD");
        card.put("amount", 9000.0);
        card.put("percentage", 30.0);
        card.put("count", 45);
        
        Map<String, Object> digital = new HashMap<>();
        digital.put("paymentType", "DIGITAL");
        digital.put("amount", 3000.0);
        digital.put("percentage", 10.0);
        digital.put("count", 15);
        
        paymentSummary[0] = cash;
        paymentSummary[1] = card;
        paymentSummary[2] = digital;
        
        return ResponseEntity.ok(paymentSummary);
    }
}