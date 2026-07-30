package de.training.taskapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ergänzt defensive Header und eine Korrelations-ID für die Fehlersuche.
 * HSTS wird erst sinnvoll, sobald die Anwendung ausschließlich über HTTPS erreichbar ist.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = sanitizeRequestId(request.getHeader(REQUEST_ID_HEADER));

        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Cache-Control", "no-store");

        filterChain.doFilter(request, response);
    }

    private static String sanitizeRequestId(String candidate) {
        if (candidate != null && candidate.matches("[A-Za-z0-9._-]{1,64}")) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
