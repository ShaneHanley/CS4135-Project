package com.pharmacy.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutingIntegrationTest {

    private static final KeyPair KEY_PAIR = generateKeyPair();
    private static final String PUBLIC_KEY_B64 = Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded());

    private static final HttpServer authBackend = startAuthBackend();
    private static final HttpServer prescriptionBackend = startPrescriptionBackend();
    private static final HttpServer patientBackend = startPatientBackend();
    private static final HttpServer pharmacyBackend = startPharmacyBackend();

    private static final AtomicReference<String> lastUserId = new AtomicReference<>();
    private static final AtomicReference<String> lastUserRole = new AtomicReference<>();

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> "http://localhost:" + authBackend.getAddress().getPort());
        registry.add("PRESCRIPTION_SERVICE_URL", () -> "http://localhost:" + prescriptionBackend.getAddress().getPort());
        registry.add("PATIENT_SERVICE_URL", () -> "http://localhost:" + patientBackend.getAddress().getPort());
        registry.add("PHARMACY_SERVICE_URL", () -> "http://localhost:" + pharmacyBackend.getAddress().getPort());
        registry.add("JWT_EC_PUBLIC_KEY_B64", () -> PUBLIC_KEY_B64);
    }

    @AfterAll
    static void tearDown() {
        authBackend.stop(0);
        prescriptionBackend.stop(0);
        patientBackend.stop(0);
        pharmacyBackend.stop(0);
    }

    @Test
    void publicAuthRoute_isForwardedWithoutToken() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/login", "{}", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("auth-backend");
    }

    @Test
    void protectedDoctorRoute_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/doctor/prescriptions", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("UNAUTHORIZED");
    }

    @Test
    void protectedDoctorRoute_withValidToken_isForwardedWithUserHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + buildToken("integration-user-1", "DOCTOR"));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/doctor/prescriptions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("prescription-backend");
        assertThat(lastUserId.get()).isEqualTo("integration-user-1");
        assertThat(lastUserRole.get()).isEqualTo("DOCTOR");
    }

    @Test
    void protectedPatientRoute_withValidToken_isForwarded() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + buildToken("patient-user-1", "PATIENT"));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/patients/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("patient-backend");
    }

    @Test
    void protectedPharmacyRoute_withValidToken_isForwarded() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + buildToken("pharmacy-user-1", "PHARMACY"));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/pharmacy/prescriptions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("pharmacy-backend");
    }

    @Test
    void notificationsRoute_withValidToken_returns404_whenNotConfigured() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + buildToken("notify-user-1", "ADMIN"));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/notifications/ping",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void endToEnd_gatewayFlow_loginThenDoctorThenPharmacyRoute() {
        ResponseEntity<String> login = restTemplate.postForEntity("/api/auth/login", "{}", String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders doctorHeaders = new HttpHeaders();
        doctorHeaders.set("Authorization", "Bearer " + buildToken("doctor-flow-1", "DOCTOR"));
        ResponseEntity<String> doctorResponse = restTemplate.exchange(
                "/api/doctor/prescriptions",
                HttpMethod.GET,
                new HttpEntity<>(doctorHeaders),
                String.class);
        assertThat(doctorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lastUserId.get()).isEqualTo("doctor-flow-1");
        assertThat(lastUserRole.get()).isEqualTo("DOCTOR");

        HttpHeaders pharmacyHeaders = new HttpHeaders();
        pharmacyHeaders.set("Authorization", "Bearer " + buildToken("pharmacy-flow-1", "PHARMACY"));
        ResponseEntity<String> pharmacyResponse = restTemplate.exchange(
                "/api/pharmacy/prescriptions",
                HttpMethod.GET,
                new HttpEntity<>(pharmacyHeaders),
                String.class);
        assertThat(pharmacyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static HttpServer startAuthBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/auth/login", exchange -> {
                byte[] body = "{\"service\":\"auth-backend\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            return server;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to start auth backend mock", ex);
        }
    }

    private static HttpServer startPrescriptionBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/doctor/prescriptions", exchange -> {
                lastUserId.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
                lastUserRole.set(exchange.getRequestHeaders().getFirst("X-User-Role"));
                byte[] body = "{\"service\":\"prescription-backend\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            return server;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to start prescription backend mock", ex);
        }
    }

    private static HttpServer startPatientBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/patients/me", exchange -> {
                byte[] body = "{\"service\":\"patient-backend\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            return server;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to start patient backend mock", ex);
        }
    }

    private static HttpServer startPharmacyBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/pharmacy/prescriptions", exchange -> {
                byte[] body = "{\"service\":\"pharmacy-backend\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            return server;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to start pharmacy backend mock", ex);
        }
    }

    private String buildToken(String userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("integration@example.com")
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(120)))
                .signWith(privateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private static PrivateKey privateKey() {
        return KEY_PAIR.getPrivate();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(256);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate test EC key pair", e);
        }
    }
}
