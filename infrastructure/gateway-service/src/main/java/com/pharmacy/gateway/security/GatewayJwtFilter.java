package com.pharmacy.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayJwtFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
    );

    private static final Map<String, List<String>> ROLE_PREFIX_PERMISSIONS = Map.of(
        "DOCTOR", List.of("/api/doctor"),
        "PHARMACIST", List.of("/api/pharmacy"),
        "MANAGER", List.of("/api/pharmacy"),
        "ADMIN", List.of("/api/auth/admin")
    );

    private static final Set<String> RESTRICTED_PREFIXES = Set.of(
        "/api/doctor",
        "/api/pharmacy",
        "/api/auth/admin"
    );

    private final Environment environment;

    public GatewayJwtFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
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
            Claims claims = Jwts.parser().verifyWith(publicKey()).build().parseSignedClaims(token).getPayload();
            String userId = claims.get("userId", String.class);
            String role = claims.get("role", String.class);
            if (userId == null || role == null) {
                writeUnauthorized(response, "Token missing required claims");
                return;
            }

            String path = request.getRequestURI();
            if (isRestrictedPath(path) && !isRoleAllowed(role, path)) {
                writeForbidden(response, "Insufficient role permissions");
                return;
            }

            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put("X-User-Id", userId);
            extraHeaders.put("X-User-Role", role);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            var wrapped = new HeaderMapRequestWrapper(request, extraHeaders);
            try {
                filterChain.doFilter(wrapped, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        } catch (JwtException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("expired")) {
                writeUnauthorized(response, "Token has expired");
            } else {
                writeUnauthorized(response, "Invalid token");
            }
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\",\"details\":null},\"timestamp\":\"" + java.time.Instant.now() + "\"}");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"" + message + "\",\"details\":null},\"timestamp\":\"" + java.time.Instant.now() + "\"}");
    }

    private PublicKey publicKey() {
        String publicKeyB64 = environment.getProperty("JWT_EC_PUBLIC_KEY_B64");
        if (publicKeyB64 == null || publicKeyB64.isBlank()) {
            throw new JwtException("JWT_EC_PUBLIC_KEY_B64 is not configured");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(publicKeyB64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            return KeyFactory.getInstance("EC").generatePublic(spec);
        } catch (Exception e) {
            throw new JwtException("Invalid EC public key", e);
        }
    }

    private static boolean isRestrictedPath(String path) {
        for (String prefix : RESTRICTED_PREFIXES) {
            if (matchesPrefix(path, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRoleAllowed(String role, String path) {
        List<String> allowedPrefixes = ROLE_PREFIX_PERMISSIONS.get(role);
        if (allowedPrefixes == null || allowedPrefixes.isEmpty()) {
            return false;
        }
        for (String prefix : allowedPrefixes) {
            if (matchesPrefix(path, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPrefix(String path, String prefix) {
        if (path == null) {
            return false;
        }
        if (path.equals(prefix)) {
            return true;
        }
        return path.startsWith(prefix + "/");
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
