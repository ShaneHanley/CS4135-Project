package com.pharmacy.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.auth.common.ErrorResponse;
import com.pharmacy.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final List<RequestMatcher> PUBLIC_PATHS = List.of(
            new AntPathRequestMatcher("/api/auth/register"),
            new AntPathRequestMatcher("/api/auth/login"),
            new AntPathRequestMatcher("/api/auth/refresh"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/actuator/health"));

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (PUBLIC_PATHS.stream().anyMatch(m -> m.matches(request))) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.parse(token);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            Collection<? extends GrantedAuthority> authorities =
                    role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("expired")) {
                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED", "Token has expired");
            } else {
                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN", "Invalid token");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeJsonError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(code, message, null));
    }
}
