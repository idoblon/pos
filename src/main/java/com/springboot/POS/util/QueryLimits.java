package com.springboot.POS.util;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bounds unbounded list endpoints so a large tenant/history can never
 * serialize millions of rows into one response.
 */
public final class QueryLimits {

    public static final int DEFAULT_LIMIT = 1000;
    public static final int MAX_LIMIT = 5000;

    private QueryLimits() {}

    public static int clamp(int limit) {
        if (limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }

    /** Returns the most recent {@code limit} items, newest first. */
    public static <T> List<T> mostRecent(List<T> items,
                                         Function<T, LocalDateTime> createdAt,
                                         int limit) {
        int l = clamp(limit);
        if (items == null) return List.of();
        return items.stream()
                .sorted(Comparator.comparing(createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(l)
                .collect(Collectors.toList());
    }
}
