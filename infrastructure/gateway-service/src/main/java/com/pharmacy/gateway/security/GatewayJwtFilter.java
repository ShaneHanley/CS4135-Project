package com.pharmacy.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayJwtFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
    );

    @Value("${JWT_SECRET:change-me-change-me-change-me-change-me}")
    private String jwtSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
            String userId = claims.get("userId", String.class);
            String role = claims.get("role", String.class);
            if (userId == null || role == null) {
                writeUnauthorized(response, "Token missing required claims");
                return;
            }

            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put("X-User-Id", userId);
            extraHeaders.put("X-User-Role", role);
            var wrapped = new HeaderMapRequestWrapper(request, extraHeaders);
            filterChain.doFilter(wrapped, response);
        } catch (Exception ex) {
            writeUnauthorized(response, "Invalid token");
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\",\"details\":null},\"timestamp\":\"" + java.time.Instant.now() + "\"}");
    }

    private static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> extraHeaders;

        HeaderMapRequestWrapper(HttpServletRequest request, Map<String, String> extraHeaders) {
            super(request);
            this.extraHeaders = extraHeaders;
        }

        @Override
        public String getHeader(String name) {
            String headerValue = extraHeaders.get(name);
            if (headerValue != null) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String headerValue = extraHeaders.get(name);
            if (headerValue != null) {
                return Collections.enumeration(Collections.singletonList(headerValue));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            var names = new java.util.LinkedHashSet<String>();
            var original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                names.add(original.nextElement());
            }
            names.addAll(extraHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}
