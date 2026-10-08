package com.springboot.POS.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure money math for orders: tax, discount and final total.
 * Extracted from OrderServiceImpl so it can be unit tested without Spring.
 */
public final class OrderTotals {

    private OrderTotals() {}

    /** VAT for the subtotal: {@code subtotal * taxRate}, rounded to 2 decimals (HALF_UP). */
    public static BigDecimal tax(BigDecimal subtotal, BigDecimal taxRate) {
        return subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Resolves the discount to subtract, in currency units.
     *
     * @param discountType "percentage" → discount is a percent of the subtotal;
     *                     anything else (or null) → discount is a flat amount
     * @throws IllegalArgumentException for negative discounts, percentages &gt; 100,
     *                                  or flat discounts above the subtotal
     */
    public static BigDecimal discountAmount(BigDecimal subtotal, BigDecimal discount, String discountType) {
        if (discount == null) return BigDecimal.ZERO;
        if (discount.signum() < 0) {
            throw new IllegalArgumentException("Discount must be a non-negative number");
        }
        if ("percentage".equalsIgnoreCase(discountType)) {
            if (discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100");
            }
            return subtotal.multiply(discount)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed the order subtotal");
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /** {@code subtotal + tax - discount}, clamped so the total can never go negative. */
    public static BigDecimal total(BigDecimal subtotal, BigDecimal taxAmount, BigDecimal discountAmount) {
        BigDecimal total = subtotal.add(taxAmount).subtract(discountAmount);
        return total.signum() < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : total;
    }

    /**
     * Integer stock units to move for a billed quantity. Inventory tracks whole
     * units, so fractional sales (e.g. 0.5 kg) deduct the ceiling — never
     * overselling stock.
     *
     * @throws IllegalArgumentException for null, zero, negative or huge quantities
     */
    public static int stockUnits(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Order item quantity must be greater than zero");
        }
        try {
            return quantity.setScale(0, RoundingMode.CEILING).intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Order item quantity is too large", e);
        }
    }
}
