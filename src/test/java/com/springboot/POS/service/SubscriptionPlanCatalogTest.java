package com.springboot.POS.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionPlanCatalogTest {

    @Test
    void branchCapsMatchAdvertisedPlans() {
        assertEquals(3, SubscriptionPlanCatalog.maxBranchesOf("BASIC"));
        assertEquals(10, SubscriptionPlanCatalog.maxBranchesOf("PROFESSIONAL"));
        assertEquals(25, SubscriptionPlanCatalog.maxBranchesOf("ENTERPRISE"));
    }

    @Test
    void userCapsMatchAdvertisedPlans() {
        assertEquals(10, SubscriptionPlanCatalog.maxUsersOf("BASIC"));
        assertEquals(50, SubscriptionPlanCatalog.maxUsersOf("PROFESSIONAL"));
        assertEquals(200, SubscriptionPlanCatalog.maxUsersOf("ENTERPRISE"));
    }

    @Test
    void storeCapsAndUnlimitedEnterprise() {
        assertEquals(1, SubscriptionPlanCatalog.maxStoresOf("BASIC"));
        assertEquals(1, SubscriptionPlanCatalog.maxStoresOf("PROFESSIONAL"));
        assertEquals(SubscriptionPlanCatalog.UNLIMITED_STORES, SubscriptionPlanCatalog.maxStoresOf("ENTERPRISE"));
    }

    @Test
    void storageQuotasMatchAdvertisedPlans() {
        assertEquals(5, SubscriptionPlanCatalog.storageGBOf("BASIC"));
        assertEquals(25, SubscriptionPlanCatalog.storageGBOf("PROFESSIONAL"));
        assertEquals(100, SubscriptionPlanCatalog.storageGBOf("ENTERPRISE"));
    }

    @Test
    void unknownPlansFallBackToBasic() {
        assertEquals(3, SubscriptionPlanCatalog.maxBranchesOf("NOPE"));
        assertEquals(10, SubscriptionPlanCatalog.maxUsersOf(null));
        assertEquals(1, SubscriptionPlanCatalog.maxStoresOf(null));
        assertEquals(5, SubscriptionPlanCatalog.storageGBOf("NOPE"));
        assertEquals(75000.0, SubscriptionPlanCatalog.priceOf(null));
    }

    @Test
    void premiumFeaturesFollowPlanTiers() {
        assertFalse(SubscriptionPlanCatalog.hasFeature("BASIC", "warehouseTransfers"));
        assertFalse(SubscriptionPlanCatalog.hasFeature("BASIC", "advancedAnalytics"));
        assertTrue(SubscriptionPlanCatalog.hasFeature("PROFESSIONAL", "warehouseTransfers"));
        assertTrue(SubscriptionPlanCatalog.hasFeature("PROFESSIONAL", "advancedAnalytics"));
        assertTrue(SubscriptionPlanCatalog.hasFeature("ENTERPRISE", "auditLog"));
        assertTrue(SubscriptionPlanCatalog.hasFeature("ENTERPRISE", "apiAccess"));
        assertFalse(SubscriptionPlanCatalog.hasFeature("PROFESSIONAL", "whiteLabel"));
        assertTrue(SubscriptionPlanCatalog.hasFeature("ENTERPRISE", "whiteLabel"));
        // Unknown features stay closed; unknown plans behave as BASIC.
        assertFalse(SubscriptionPlanCatalog.hasFeature("ENTERPRISE", "teleportation"));
        assertFalse(SubscriptionPlanCatalog.hasFeature(null, "apiAccess"));
    }

    @Test
    void supportTiersMatchAdvertisedPlans() {
        assertEquals("Email", SubscriptionPlanCatalog.supportTierOf("BASIC"));
        assertEquals("Priority", SubscriptionPlanCatalog.supportTierOf("PROFESSIONAL"));
        assertEquals("24/7 Dedicated", SubscriptionPlanCatalog.supportTierOf("ENTERPRISE"));
    }
}
