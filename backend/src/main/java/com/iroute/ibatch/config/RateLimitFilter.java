package com.iroute.ibatch.config;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final int PROCESS_FILE_LIMIT = 5;
    private static final int REPROCESS_LIMIT = 20;

    private final ConcurrentHashMap<String, RequestWindow> windows = new ConcurrentHashMap<>();

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

        if (!window.allow(limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
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

    private static final class RequestWindow {
        private long startedAt = System.currentTimeMillis();
        private int requests;

        private synchronized boolean allow(int limit) {
            var now = System.currentTimeMillis();
            if (now - startedAt >= WINDOW_MILLIS) {
                startedAt = now;
                requests = 0;
            }
            requests++;
            return requests <= limit;
        }
    }
}
