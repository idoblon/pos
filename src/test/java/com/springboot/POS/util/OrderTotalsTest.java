package com.springboot.POS.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTotalsTest {

    private static final BigDecimal RATE = new BigDecimal("0.13");

    @Test
    void taxIsThirteenPercentRoundedToTwoDecimals() {
        assertEquals(new BigDecimal("13.00"), OrderTotals.tax(new BigDecimal("100.00"), RATE));
    }

    @Test
    void taxRoundsHalfUp() {
        // 99.99 * 0.13 = 12.9987 → 13.00
        assertEquals(new BigDecimal("13.00"), OrderTotals.tax(new BigDecimal("99.99"), RATE));
        // 10.01 * 0.13 = 1.3013 → 1.30
        assertEquals(new BigDecimal("1.30"), OrderTotals.tax(new BigDecimal("10.01"), RATE));
    }

    @Test
    void nullDiscountIsZero() {
        assertEquals(0, OrderTotals.discountAmount(new BigDecimal("50.00"), null, null)
                .compareTo(BigDecimal.ZERO));
    }

    @Test
    void percentageDiscountComputesShareOfSubtotal() {
        BigDecimal subtotal = new BigDecimal("200.00");
        BigDecimal discount = OrderTotals.discountAmount(subtotal, new BigDecimal("10"), "percentage");
        assertEquals(new BigDecimal("20.00"), discount);
    }

    @Test
    void flatDiscountStaysAsIs() {
        BigDecimal subtotal = new BigDecimal("200.00");
        BigDecimal discount = OrderTotals.discountAmount(subtotal, new BigDecimal("5.5"), "flat");
        assertEquals(new BigDecimal("5.50"), discount);
    }

    @Test
    void negativeDiscountRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> OrderTotals.discountAmount(new BigDecimal("100"), new BigDecimal("-1"), null));
    }

    @Test
    void percentageOver100Rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> OrderTotals.discountAmount(new BigDecimal("100"), new BigDecimal("101"), "percentage"));
    }

    @Test
    void flatDiscountAboveSubtotalRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> OrderTotals.discountAmount(new BigDecimal("100"), new BigDecimal("100.01"), null));
    }

    @Test
    void totalIsSubtotalPlusTaxMinusDiscount() {
        BigDecimal total = OrderTotals.total(
                new BigDecimal("100.00"), new BigDecimal("13.00"), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("103.00"), total);
    }

    @Test
    void totalNeverGoesNegative() {
        BigDecimal total = OrderTotals.total(
                new BigDecimal("10.00"), new BigDecimal("1.30"), new BigDecimal("100.00"));
        assertEquals(0, total.compareTo(BigDecimal.ZERO));
    }
}
