package com.pharmacy.auth.service;

import com.pharmacy.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class JwtService {
    @Value("${jwt.access-token-ttl-seconds:900}")
    private long accessTokenTtlSeconds;

    @Value("${jwt.refresh-token-ttl-seconds:604800}")
    private long refreshTokenTtlSeconds;

    @Value("${JWT_EC_PRIVATE_KEY_B64:}")
    private String privateKeyB64;

    @Value("${JWT_EC_PUBLIC_KEY_B64:}")
    private String publicKeyB64;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .signWith(privateKey(), Jwts.SIG.ES256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenTtlSeconds)))
                .signWith(privateKey(), Jwts.SIG.ES256)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(publicKey()).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new SignatureException("Invalid token signature", e);
        }
    }

    private PrivateKey privateKey() {
        if (privateKeyB64 == null || privateKeyB64.isBlank()) {
            throw new IllegalStateException("JWT_EC_PRIVATE_KEY_B64 is not configured");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(privateKeyB64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid EC private key", e);
        }
    }

    private PublicKey publicKey() {
        if (publicKeyB64 == null || publicKeyB64.isBlank()) {
            throw new IllegalStateException("JWT_EC_PUBLIC_KEY_B64 is not configured");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(publicKeyB64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            return KeyFactory.getInstance("EC").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid EC public key", e);
        }
    }
}
