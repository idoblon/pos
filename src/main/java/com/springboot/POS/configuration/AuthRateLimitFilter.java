package com.springboot.POS.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple fixed-window rate limiter for authentication endpoints
 * (login, forgot-password, reset-password, refresh) keyed by client IP.
 * Blocks credential stuffing / brute force attempts per IP.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 60;

    private static final class AttemptWindow {
        final AtomicInteger count = new AtomicInteger();
        final Instant resetAt = Instant.now().plusSeconds(WINDOW_SECONDS);

        boolean expired() {
            return Instant.now().isAfter(resetAt);
        }
    }

    /** clientIp -> current window */
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isRateLimitedPath(request.getRequestURI())) {
            String clientIp = resolveClientIp(request);
            if (clientIp == null) {
                response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Too many requests");
                return;
            }
            pruneIfOverflow();
            AttemptWindow window = attempts.compute(clientIp,
                    (ip, existing) -> (existing == null || existing.expired()) ? new AttemptWindow() : existing);
            if (window.count.incrementAndGet() > MAX_ATTEMPTS) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Too many attempts, please try again later\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String uri) {
        return uri != null && (
                uri.equals("/auth/login")
                        || uri.equals("/auth/forgot-password")
                        || uri.equals("/auth/reset-password")
                        || uri.equals("/auth/refresh"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First entry is the original client when behind a proxy.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Avoid unbounded map growth under IP rotation (e.g. distributed attackers). */
    private void pruneIfOverflow() {
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(e -> e.getValue().expired());
        }
    }
}
