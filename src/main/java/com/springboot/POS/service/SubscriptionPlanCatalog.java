package com.springboot.POS.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single source of truth for subscription plan pricing/limits.
 * Frontend must fetch {@code GET /api/admin/plans} (or public
 * {@code GET /api/public/payments/plans}) instead of hardcoding prices.
 * Amounts are NPR per year.
 */
public final class SubscriptionPlanCatalog {

    private SubscriptionPlanCatalog() {}

    public record Plan(String name, double price, String currency, String billing,
                       int maxBranches, int maxUsers, String storage) {}

    private static final Map<String, Plan> PLANS = new LinkedHashMap<>();

    static {
        PLANS.put("BASIC", new Plan("Basic", 3500.0, "NPR", "yearly", 3, 10, "5GB"));
        PLANS.put("PROFESSIONAL", new Plan("Professional", 7000.0, "NPR", "yearly", 10, 50, "25GB"));
        PLANS.put("ENTERPRISE", new Plan("Enterprise", 10000.0, "NPR", "yearly", 25, 200, "100GB"));
    }

    public static Map<String, Plan> all() {
        return Map.copyOf(PLANS);
    }

    public static double priceOf(String plan) {
        if (plan == null) return 3500.0;
        Plan p = PLANS.get(plan.toUpperCase());
        return p != null ? p.price() : 3500.0;
    }

    public static Map<String, Object> asResponse() {
        Map<String, Object> out = new LinkedHashMap<>();
        PLANS.forEach((key, p) -> out.put(key, Map.of(
                "name", p.name(),
                "price", p.price(),
                "currency", p.currency(),
                "billing", p.billing(),
                "maxBranches", p.maxBranches(),
                "maxUsers", p.maxUsers(),
                "storage", p.storage())));
        return out;
    }
}
