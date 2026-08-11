package com.iroute.ibatch.config;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final long CLEANUP_INTERVAL_MILLIS = 300_000L;
    private static final int UPLOAD_FILE_LIMIT = 3;
    private static final int PROCESS_FILE_LIMIT = 5;
    private static final int REPROCESS_LIMIT = 20;
    private static final int LOGIN_LIMIT = 5;

    private final ConcurrentHashMap<String, RequestWindow> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupAt = new AtomicLong(System.currentTimeMillis());

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var limit = resolveLimit(request);

        if (limit == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        var key = request.getRemoteAddr() + ':' + request.getMethod() + ':' + rateLimitBucket(request);
        var window = windows.computeIfAbsent(key, ignored -> new RequestWindow());
        var now = System.currentTimeMillis();
        var decision = window.evaluate(limit, now);
        cleanupExpiredWindows(now);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remainingRequests()));

        if (!decision.allowed()) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.getWriter().write("{\"message\":\"Demasiadas solicitudes. Intente nuevamente en un minuto\",\"timestamp\":\""
                    + OffsetDateTime.now() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimit(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return 0;
        }
        if ("/files/upload".equals(request.getRequestURI())) {
            return UPLOAD_FILE_LIMIT;
        }
        if ("/auth/login".equals(request.getRequestURI())) {
            return LOGIN_LIMIT;
        }
        if ("/files/process".equals(request.getRequestURI())) {
            return PROCESS_FILE_LIMIT;
        }
        if (request.getRequestURI().matches("/transactions/\\d+")) {
            return REPROCESS_LIMIT;
        }
        return 0;
    }

    private String rateLimitBucket(HttpServletRequest request) {
        return request.getRequestURI().matches("/transactions/\\d+")
                ? "/transactions/{id}"
                : request.getRequestURI();
    }

    private void cleanupExpiredWindows(long now) {
        var previousCleanup = lastCleanupAt.get();
        if (now - previousCleanup < CLEANUP_INTERVAL_MILLIS
                || !lastCleanupAt.compareAndSet(previousCleanup, now)) {
            return;
        }

        windows.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private static final class RequestWindow {
        private long startedAt = System.currentTimeMillis();
        private int requests;

        private synchronized RateLimitDecision evaluate(int limit, long now) {
            if (now - startedAt >= WINDOW_MILLIS) {
                startedAt = now;
                requests = 0;
            }

            if (requests >= limit) {
                var remainingMillis = Math.max(1, WINDOW_MILLIS - (now - startedAt));
                var retryAfterSeconds = Math.max(1, (int) Math.ceil(remainingMillis / 1000.0));
                return new RateLimitDecision(false, 0, retryAfterSeconds);
            }

            requests++;
            return new RateLimitDecision(true, limit - requests, 0);
        }

        private synchronized boolean isExpired(long now) {
            return now - startedAt >= WINDOW_MILLIS;
        }
    }

    private record RateLimitDecision(boolean allowed, int remainingRequests, int retryAfterSeconds) {
    }
}
