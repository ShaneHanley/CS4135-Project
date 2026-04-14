package com.pharmacy.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pharmacy.auth.entity.User;
import com.pharmacy.auth.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        ReflectionTestUtils.setField(jwtService, "privateKeyB64",
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(jwtService, "publicKeyB64",
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        ReflectionTestUtils.setField(jwtService, "accessTokenTtlSeconds", 60L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenTtlSeconds", 120L);
    }

    @Test
    void generateAccessToken_containsClaims() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("u@example.com");
        u.setRole(UserRole.DOCTOR);
        String token = jwtService.generateAccessToken(u);
        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("u@example.com");
        assertThat(claims.get("userId", String.class)).isEqualTo(u.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("u@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("DOCTOR");
    }

    @Test
    void generateRefreshToken_containsSubjectAndUserId() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("u@example.com");
        u.setRole(UserRole.PATIENT);
        String token = jwtService.generateRefreshToken(u);
        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("u@example.com");
        assertThat(claims.get("userId", String.class)).isEqualTo(u.getId().toString());
    }

    @Test
    void parse_invalidToken_throws() {
        assertThatThrownBy(() -> jwtService.parse("not-a-jwt")).isInstanceOf(JwtException.class);
    }
}
