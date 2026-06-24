package com.splitit.infrastructure.web.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Outermost filter: tags every request with a short traceId (in MDC, so it appears
 * on all log lines for the request) and logs a single completion line with method,
 * URI, status and duration. Clears the MDC at the end to avoid leaking into pooled
 * threads. Runs before Spring Security so {@code userId} (set by the JWT filter) is
 * still present when the completion line is written.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        MDC.put(TRACE_ID, UUID.randomUUID().toString().substring(0, 8));
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.debug("--> {} {}", method, uri);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("<-- {} {} {} ({} ms)", method, uri, response.getStatus(), durationMs);
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Health probe is polled frequently; keep it out of the request log.
        return request.getRequestURI().startsWith("/api/health");
    }
}
