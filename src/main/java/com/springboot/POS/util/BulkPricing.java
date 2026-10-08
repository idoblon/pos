package com.springboot.POS.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Bulk-tier unit pricing shared by order creation and held orders.
 * Mirrors the terminal's {@code getEffectivePrice}: the best (lowest)
 * tier whose {@code minQty} is met wins, otherwise the base price.
 * Extracted so it can be unit tested without Spring.
 */
public final class BulkPricing {

    private BulkPricing() {}

    /**
     * @param basePrice  product selling price (never null in practice)
     * @param tiersJson  LONGTEXT JSON array of {minQty, price} maps, may be null/blank
     * @param legacyMin  legacy single-tier min qty, may be null
     * @param legacyPrice legacy single-tier price, may be null
     * @param quantity   total units for this product on the order
     * @return effective unit price, scale 2 HALF_UP
     */
    public static BigDecimal effectiveUnitPrice(BigDecimal basePrice, String tiersJson,
                                                Integer legacyMin, BigDecimal legacyPrice,
                                                BigDecimal quantity) {
        BigDecimal base = basePrice == null ? BigDecimal.ZERO
                : basePrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal qty = quantity == null ? BigDecimal.ONE : quantity;
        BigDecimal best = base;

        if (legacyMin != null && legacyMin > 1 && legacyPrice != null
                && legacyPrice.signum() > 0
                && qty.compareTo(BigDecimal.valueOf(legacyMin)) >= 0) {
            best = legacyPrice.setScale(2, RoundingMode.HALF_UP);
        }

        for (Map<String, Object> tier : JsonLists.mapList(tiersJson)) {
            BigDecimal min = toDecimal(tier.get("minQty"));
            BigDecimal price = toDecimal(tier.get("price"));
            if (min == null || price == null) continue;
            if (min.compareTo(BigDecimal.ONE) > 0 && price.signum() > 0
                    && qty.compareTo(min) >= 0
                    && price.compareTo(best) < 0) {
                best = price.setScale(2, RoundingMode.HALF_UP);
            }
        }
        return best;
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
