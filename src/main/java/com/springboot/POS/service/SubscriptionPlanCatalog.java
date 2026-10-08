package com.springboot.POS.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single source of truth for subscription plan pricing/limits/features.
 * Frontend must fetch {@code GET /api/admin/plans} (or public
 * {@code GET /api/public/payments/plans}) instead of hardcoding prices.
 * Amounts are NPR per year.
 */
public final class SubscriptionPlanCatalog {

    private SubscriptionPlanCatalog() {}

    public record Plan(String name, double price, String currency, String billing,
                       int maxBranches, int maxUsers, String storage, int maxStores,
                       int storageGB, boolean advancedAnalytics, boolean warehouseTransfers,
                       boolean auditLog, boolean apiAccess, boolean whiteLabel,
                       String supportTier) {}

    /** -1 stores = unlimited (Enterprise). */
    public static final int UNLIMITED_STORES = -1;

    private static final Map<String, Plan> PLANS = new LinkedHashMap<>();

    static {
        PLANS.put("BASIC", new Plan("Basic", 75000.0, "NPR", "yearly", 3, 10, "5GB",
                1, 5,
                false, false, false, false, false,
                "Email"));
        PLANS.put("PROFESSIONAL", new Plan("Professional", 135000.0, "NPR", "yearly", 10, 50, "25GB",
                1, 25,
                true, true, false, true, false,
                "Priority"));
        PLANS.put("ENTERPRISE", new Plan("Enterprise", 210000.0, "NPR", "yearly", 25, 200, "100GB",
                UNLIMITED_STORES, 100,
                true, true, true, true, true,
                "24/7 Dedicated"));
    }

    public static Map<String, Plan> all() {
        return Map.copyOf(PLANS);
    }

    public static double priceOf(String plan) {
        if (plan == null) return 75000.0;
        Plan p = PLANS.get(plan.toUpperCase());
        return p != null ? p.price() : 75000.0;
    }

    /** Branch cap for a plan key; unknown or null plans fall back to BASIC. */
    public static int maxBranchesOf(String plan) {
        if (plan == null) return PLANS.get("BASIC").maxBranches();
        Plan p = PLANS.get(plan.toUpperCase());
        return p != null ? p.maxBranches() : PLANS.get("BASIC").maxBranches();
    }

    /** User cap for a plan key; unknown or null plans fall back to BASIC. */
    public static int maxUsersOf(String plan) {
        if (plan == null) return PLANS.get("BASIC").maxUsers();
        Plan p = PLANS.get(plan.toUpperCase());
        return p != null ? p.maxUsers() : PLANS.get("BASIC").maxUsers();
    }

    /**
     * Stores per owner for a plan key; {@link #UNLIMITED_STORES} means no cap.
     * Unknown or null plans fall back to BASIC (1 store).
     */
    public static int maxStoresOf(String plan) {
        if (plan == null) return PLANS.get("BASIC").maxStores();
        Plan p = PLANS.get(plan.toUpperCase());
        return p != null ? p.maxStores() : PLANS.get("BASIC").maxStores();
    }

    /** Storage quota in GB; unknown or null plans fall back to BASIC. */
    public static int storageGBOf(String plan) {
        if (plan == null) return PLANS.get("BASIC").storageGB();
        Plan p = PLANS.get(plan.toUpperCase());
        return p != null ? p.storageGB() : PLANS.get("BASIC").storageGB();
    }

    public static String supportTierOf(String plan) {
        if (plan == null) return PLANS.get("BASIC").supportTier();
        Plan p = PLANS.get(plan.toUpperCase());
        return p != null ? p.supportTier() : PLANS.get("BASIC").supportTier();
    }

    /**
     * Premium feature flags. Unknown features are closed (false) so newly
     * added flags default to the highest tier until explicitly assigned.
     */
    public static boolean hasFeature(String plan, String feature) {
        Plan p = plan == null ? null : PLANS.get(plan.toUpperCase());
        if (p == null) p = PLANS.get("BASIC");
        return switch (feature) {
            case "advancedAnalytics" -> p.advancedAnalytics();
            case "warehouseTransfers" -> p.warehouseTransfers();
            case "auditLog" -> p.auditLog();
            case "apiAccess" -> p.apiAccess();
            case "whiteLabel" -> p.whiteLabel();
            default -> false;
        };
    }

    public static Map<String, Object> asResponse() {
        Map<String, Object> out = new LinkedHashMap<>();
        PLANS.forEach((key, p) -> {
            Map<String, Object> plan = new LinkedHashMap<>();
            plan.put("name", p.name());
            plan.put("price", p.price());
            plan.put("currency", p.currency());
            plan.put("billing", p.billing());
            plan.put("maxBranches", p.maxBranches());
            plan.put("maxUsers", p.maxUsers());
            plan.put("storage", p.storage());
            plan.put("maxStores", p.maxStores());
            plan.put("storageGB", p.storageGB());
            plan.put("supportTier", p.supportTier());
            Map<String, Object> features = new LinkedHashMap<>();
            features.put("advancedAnalytics", p.advancedAnalytics());
            features.put("warehouseTransfers", p.warehouseTransfers());
            features.put("auditLog", p.auditLog());
            features.put("apiAccess", p.apiAccess());
            features.put("whiteLabel", p.whiteLabel());
            plan.put("features", features);
            out.put(key, plan);
        });
        return out;
    }
}
