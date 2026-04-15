package com.booking.therapist_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query == null ? path : path + "?" + query;

        log.info("START request method={} path={}", method, fullPath);

        try {
            filterChain.doFilter(request, response);
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("END request method={} path={} status={} durationMs={}",
                    method,
                    fullPath,
                    response.getStatus(),
                    durationMs);
        } catch (Exception ex) {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.warn("FAIL request method={} path={} status={} durationMs={} error={}",
                    method,
                    fullPath,
                    response.getStatus(),
                    durationMs,
                    ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
