package com.pharmacy.auth.config;

import com.pharmacy.auth.entity.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inserts fixed-UUID demo users when {@code app.demo-users.enabled=true}, using bcrypt hashes
 * derived from environment variables (never from Flyway SQL).
 */
@Component
@ConditionalOnProperty(name = "app.demo-users.enabled", havingValue = "true")
public class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${DEMO_USER_PASSWORD:}")
    private String demoUserPassword;

    @Value("${DEMO_DOCTOR_PASSWORD:}")
    private String demoDoctorPassword;

    @Value("${DEMO_PHARMACIST_PASSWORD:}")
    private String demoPharmacistPassword;

    @Value("${DEMO_MANAGER_PASSWORD:}")
    private String demoManagerPassword;

    public DemoUserSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "doctor@demo.com",
                "Demo",
                "Doctor",
                UserRole.DOCTOR,
                resolvePassword(demoDoctorPassword));
        seed(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "pharmacist@demo.com",
                "Demo",
                "Pharmacist",
                UserRole.PHARMACIST,
                resolvePassword(demoPharmacistPassword));
        seed(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "manager@demo.com",
                "Demo",
                "Manager",
                UserRole.MANAGER,
                resolvePassword(demoManagerPassword));
    }

    private String resolvePassword(String specific) {
        if (specific != null && !specific.isBlank()) {
            return specific;
        }
        if (demoUserPassword != null && !demoUserPassword.isBlank()) {
            return demoUserPassword;
        }
        return null;
    }

    private void seed(UUID id, String email, String firstName, String lastName, UserRole role, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            log.warn("Demo user seed skipped for {}: set DEMO_USER_PASSWORD or DEMO_*_PASSWORD", email);
            return;
        }
        String hash = passwordEncoder.encode(rawPassword);
        Instant now = Instant.now();
        int rows =
                jdbcTemplate.update(
                        """
                        INSERT INTO auth_svc.users (id, first_name, last_name, email, password_hash, role, created_at, updated_at, active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, true)
                        ON CONFLICT (email) DO NOTHING
                        """,
                        id,
                        firstName,
                        lastName,
                        email.toLowerCase(),
                        hash,
                        role.name(),
                        now,
                        now);
        if (rows > 0) {
            log.info("Seeded demo user {}", email);
        }
    }
}
