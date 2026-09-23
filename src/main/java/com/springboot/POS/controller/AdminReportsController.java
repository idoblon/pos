package com.springboot.POS.controller;

import com.springboot.POS.modal.Order;
import com.springboot.POS.modal.Refund;
import com.springboot.POS.modal.Store;
import com.springboot.POS.repository.OrderRepository;
import com.springboot.POS.repository.RefundRepository;
import com.springboot.POS.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Server-aggregated admin reports. Replaces the admin UI's full-dump
 * client-side aggregation (getAllOrders/getAllRefund in memory) with a
 * single overview call plus CSV export. Showcase-safe: date params optional,
 * results capped by repository queries.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportsController {

    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final StoreRepository storeRepository;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long storeId) {
        LocalDateTime start = (from != null ? from : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime end = (to != null ? to : LocalDate.now()).atTime(23, 59, 59);

        List<Order> orders = storeId != null
                ? orderRepository.findByStoreIdAndCreatedAtBetween(storeId, start, end)
                : orderRepository.findAll().stream()
                        .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(end))
                        .limit(5000).collect(Collectors.toList());

        List<Refund> refunds = (storeId != null
                ? refundRepository.findByStoreId(storeId)
                : refundRepository.findAll()).stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(start) && !r.getCreatedAt().isAfter(end))
                .limit(5000).collect(Collectors.toList());

        BigDecimal sales = orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundTotal = refunds.stream()
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long orderCount = orders.size();
        BigDecimal aov = orderCount == 0 ? BigDecimal.ZERO
                : sales.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);

        Map<String, Long> paymentMix = orders.stream()
                .collect(Collectors.groupingBy(o -> String.valueOf(o.getPaymentType()), Collectors.counting()));
        Map<String, Long> planMix = new LinkedHashMap<>();
        Map<String, Long> statusMix = new LinkedHashMap<>();
        List<Store> stores = storeId != null
                ? storeRepository.findById(storeId).map(List::of).orElse(List.of())
                : storeRepository.findAll();
        long expiring = 0, expired = 0, activeStores = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Store s : stores) {
            if (Boolean.TRUE.equals(s.getDeleted())) continue;
            if (s.getStatus() != null) statusMix.merge(String.valueOf(s.getStatus()), 1L, Long::sum);
            planMix.merge(String.valueOf(s.getSubscriptionPlan()), 1L, Long::sum);
            if (s.getSubscriptionExpiry() != null) {
                if (s.getSubscriptionExpiry().isBefore(now)) expired++;
                else if (s.getSubscriptionExpiry().isBefore(now.plusDays(60))) expiring++;
            }
            if ("ACTIVE".equals(String.valueOf(s.getStatus()))) activeStores++;
        }

        // Daily trend for the range (capped at 90 points).
        Map<String, BigDecimal> dailyTrend = new TreeMap<>();
        for (Order o : orders) {
            String day = o.getCreatedAt().toLocalDate().toString();
            dailyTrend.merge(day, o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO, BigDecimal::add);
            if (dailyTrend.size() > 90) break;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", start.toLocalDate().toString());
        body.put("to", end.toLocalDate().toString());
        body.put("storeId", storeId);
        body.put("totalSales", sales);
        body.put("totalRefunds", refundTotal);
        body.put("netSales", sales.subtract(refundTotal));
        body.put("orderCount", orderCount);
        body.put("refundCount", refunds.size());
        body.put("averageOrderValue", aov);
        body.put("refundRate", orderCount == 0 ? 0 : (double) refunds.size() / orderCount);
        body.put("paymentMix", paymentMix);
        body.put("planMix", planMix);
        body.put("storeStatusMix", statusMix);
        body.put("activeStores", activeStores);
        body.put("totalStores", stores.size());
        body.put("expiring60d", expiring);
        body.put("expired", expired);
        body.put("dailyTrend", dailyTrend);
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long storeId) {
        Map<String, Object> data = overview(from, to, storeId).getBody();
        StringBuilder csv = new StringBuilder("metric,value\n");
        if (data != null) {
            for (Map.Entry<String, Object> e : data.entrySet()) {
                if (e.getValue() instanceof Map || e.getValue() instanceof List) continue;
                csv.append(e.getKey()).append(",").append(csvEscape(String.valueOf(e.getValue()))).append("\n");
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
