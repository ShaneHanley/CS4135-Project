package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionStatusService {

    private final JdbcTemplate jdbcTemplate;

    public Optional<Instant> findPrescriptionUpdatedAt(String prescriptionId) {
        if (prescriptionId == null || prescriptionId.isBlank()) {
            return Optional.empty();
        }

        UUID id;
        try {
            id = UUID.fromString(prescriptionId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid prescription id for staleness check: {}", prescriptionId);
            return Optional.empty();
        }

        try {
            return jdbcTemplate.query(
                "SELECT updated_at FROM pharmacy_svc.pharmacy_prescriptions WHERE prescription_id = ?",
                ps -> ps.setObject(1, id),
                rs -> rs.next() ? Optional.of(rs.getTimestamp("updated_at").toInstant()) : Optional.empty()
            );
        } catch (Exception e) {
            log.warn("Failed to read prescription updated_at for id {}", prescriptionId, e);
            return Optional.empty();
        }
    }
}
