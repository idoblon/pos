package com.springboot.POS.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryLimitsTest {

    @Test
    void clampFallsBackToDefaultForNonPositiveValues() {
        assertEquals(QueryLimits.DEFAULT_LIMIT, QueryLimits.clamp(0));
        assertEquals(QueryLimits.DEFAULT_LIMIT, QueryLimits.clamp(-5));
    }

    @Test
    void clampKeepsValidValues() {
        assertEquals(500, QueryLimits.clamp(500));
    }

    @Test
    void clampCapsAtMaxLimit() {
        assertEquals(QueryLimits.MAX_LIMIT, QueryLimits.clamp(999_999));
    }

    @Test
    void mostRecentSortsNewestFirstAndRespectsLimit() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        List<String> items = Arrays.asList("old", "new", "mid");
        List<String> result = QueryLimits.mostRecent(
                items,
                s -> switch (s) {
                    case "new" -> now.plusDays(2);
                    case "mid" -> now.plusDays(1);
                    default -> now;
                },
                2);
        assertEquals(Arrays.asList("new", "mid"), result);
    }

    @Test
    void nullCreatedAtSortsLast() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        List<String> result = QueryLimits.mostRecent(
                Arrays.asList("a", "b"),
                s -> s.equals("a") ? null : now,
                10);
        assertEquals(Arrays.asList("b", "a"), result);
    }

    @Test
    void nullListYieldsEmptyList() {
        assertEquals(List.of(), QueryLimits.mostRecent(null, s -> LocalDateTime.now(), 10));
    }
}
