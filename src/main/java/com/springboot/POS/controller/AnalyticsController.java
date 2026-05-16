package com.springboot.POS.controller;

import com.springboot.POS.domain.PaymentType;
import com.springboot.POS.modal.Order;
import com.springboot.POS.modal.OrderItem;
import com.springboot.POS.modal.User;
import com.springboot.POS.repository.OrderRepository;
import com.springboot.POS.service.UserService;
import com.springboot.POS.service.impl.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final OwnershipGuard ownershipGuard;

    @GetMapping("/store/{storeId}")
    public ResponseEntity<Map<String, Object>> getStoreAnalytics(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);

        List<Order> orders = orderRepository.findByStoreId(storeId);
        return ResponseEntity.ok(buildSummary(orders, storeId, null, null));
    }

    @GetMapping("/store/{storeId}/range")
    public ResponseEntity<Map<String, Object>> getStoreAnalyticsByDateRange(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();
        List<Order> orders = orderRepository.findByStoreIdAndCreatedAtBetween(storeId, from, to);
        return ResponseEntity.ok(buildSummary(orders, storeId, startDate.toString(), endDate.toString()));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<Map<String, Object>> getBranchAnalytics(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireBranchAccess(user, branchId);

        List<Order> orders = orderRepository.findByBranchId(branchId);
        return ResponseEntity.ok(buildSummary(orders, null, null, null));
    }

    @GetMapping("/store/{storeId}/sales-chart")
    public ResponseEntity<List<Map<String, Object>>> getSalesChart(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);

        // Last 6 months
        List<Map<String, Object>> chart = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime from = LocalDate.now().minusMonths(i).withDayOfMonth(1).atStartOfDay();
            LocalDateTime to = from.plusMonths(1);
            List<Order> orders = orderRepository.findByStoreIdAndCreatedAtBetween(storeId, from, to);
            double sales = orders.stream().mapToDouble(Order::getTotalAmount).sum();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("period", from.getMonth().name());
            point.put("sales", sales);
            point.put("orders", orders.size());
            chart.add(point);
        }
        return ResponseEntity.ok(chart);
    }

    @GetMapping("/store/{storeId}/top-products")
    public ResponseEntity<List<Map<String, Object>>> getTopProducts(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);

        List<Order> orders = orderRepository.findByStoreId(storeId);

        Map<Long, double[]> productStats = new HashMap<>(); // productId -> [qty, revenue]
        Map<Long, String> productNames = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                Long pid = item.getProduct().getId();
                productNames.put(pid, item.getProduct().getName());
                productStats.computeIfAbsent(pid, k -> new double[]{0, 0});
                productStats.get(pid)[0] += item.getQuantity();
                productStats.get(pid)[1] += item.getPrice();
            }
        }

        List<Map<String, Object>> result = productStats.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .limit(limit)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", e.getKey());
                    m.put("productName", productNames.get(e.getKey()));
                    m.put("quantitySold", e.getValue()[0]);
                    m.put("revenue", e.getValue()[1]);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/store/{storeId}/payment-summary")
    public ResponseEntity<List<Map<String, Object>>> getPaymentSummary(
            @PathVariable Long storeId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);
        ownershipGuard.requireStoreAccess(user, storeId);

        List<Order> orders = orderRepository.findByStoreId(storeId);
        double totalSales = orders.stream().mapToDouble(Order::getTotalAmount).sum();

        Map<PaymentType, List<Order>> grouped = orders.stream()
                .filter(o -> o.getPaymentType() != null)
                .collect(Collectors.groupingBy(Order::getPaymentType));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<PaymentType, List<Order>> entry : grouped.entrySet()) {
            double amount = entry.getValue().stream().mapToDouble(Order::getTotalAmount).sum();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("paymentType", entry.getKey());
            m.put("amount", amount);
            m.put("count", entry.getValue().size());
            m.put("percentage", totalSales > 0 ? Math.round((amount / totalSales) * 10000.0) / 100.0 : 0);
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> buildSummary(List<Order> orders, Long storeId,
                                              String startDate, String endDate) {
        double totalSales = orders.stream().mapToDouble(Order::getTotalAmount).sum();
        Set<Long> uniqueCustomers = orders.stream()
                .filter(o -> o.getCustomer() != null)
                .map(o -> o.getCustomer().getId())
                .collect(Collectors.toSet());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalOrders", orders.size());
        summary.put("totalSales", totalSales);
        summary.put("totalCustomers", uniqueCustomers.size());
        summary.put("averageOrderValue", orders.isEmpty() ? 0 : Math.round((totalSales / orders.size()) * 100.0) / 100.0);
        if (storeId != null) summary.put("storeId", storeId);
        if (startDate != null) summary.put("startDate", startDate);
        if (endDate != null) summary.put("endDate", endDate);
        return summary;
    }
}
