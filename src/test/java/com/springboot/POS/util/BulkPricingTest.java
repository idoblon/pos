package com.springboot.POS.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkPricingTest {

    private static String tiers(Object... pairs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("{\"minQty\":").append(pairs[i])
              .append(",\"price\":").append(pairs[i + 1]).append("}");
        }
        return sb.append("]").toString();
    }

    @Test
    void basePriceBelowEveryTier() {
        assertEquals(new BigDecimal("100.00"), BulkPricing.effectiveUnitPrice(
                new BigDecimal("100"), tiers(10, 90, 50, 80), null, null, new BigDecimal("5")));
    }

    @Test
    void bestMatchingTierWins() {
        assertEquals(new BigDecimal("80.00"), BulkPricing.effectiveUnitPrice(
                new BigDecimal("100"), tiers(10, 90, 50, 80), null, null, new BigDecimal("60")));
    }

    @Test
    void legacySingleTierApplies() {
        assertEquals(new BigDecimal("90.00"), BulkPricing.effectiveUnitPrice(
                new BigDecimal("100"), null, 10, new BigDecimal("90"), new BigDecimal("12")));
    }

    @Test
    void legacyTierIgnoredBelowThreshold() {
        assertEquals(new BigDecimal("100.00"), BulkPricing.effectiveUnitPrice(
                new BigDecimal("100"), null, 10, new BigDecimal("90"), new BigDecimal("9")));
    }

    @Test
    void malformedTierJsonFallsBackToBase() {
        assertEquals(new BigDecimal("100.00"), BulkPricing.effectiveUnitPrice(
                new BigDecimal("100"), "not-json", null, null, new BigDecimal("99")));
    }

    @Test
    void stockUnitsRoundsFractionalSalesUp() {
        assertEquals(1, OrderTotals.stockUnits(new BigDecimal("0.5")));
        assertEquals(2, OrderTotals.stockUnits(new BigDecimal("2")));
        assertEquals(3, OrderTotals.stockUnits(new BigDecimal("2.1")));
    }
}
