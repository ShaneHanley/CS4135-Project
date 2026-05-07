package com.pharmacy.prescription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.entity.OutboxEvent;
import com.pharmacy.prescription.entity.Prescription;
import com.pharmacy.prescription.entity.PrescriptionStatus;
import com.pharmacy.prescription.repository.OutboxEventRepository;
import com.pharmacy.prescription.repository.PrescriptionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrescriptionService {

    public static final String PRESCRIPTION_CREATED_QUEUE = "prescription_created";

    private final PrescriptionRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PrescriptionService(
            PrescriptionRepository repository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public List<PrescriptionDtos.PrescriptionView> byDoctor(String doctorId) {
        return repository.findByDoctorId(doctorId).stream().map(this::toView).toList();
    }

    @Transactional
    public PrescriptionDtos.PrescriptionView create(String doctorId, String idempotencyKey, PrescriptionDtos.CreatePrescriptionRequest r) {
        String key = normalizeIdempotencyKey(idempotencyKey);
        return repository
                .findByDoctorIdAndIdempotencyKey(doctorId, key)
                .map(this::toView)
                .orElseGet(() -> insertNewPrescription(doctorId, key, r));
    }

    private String normalizeIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("X-Idempotency-Key header is required");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 128) {
            throw new IllegalArgumentException("X-Idempotency-Key must be at most 128 characters");
        }
        return trimmed;
    }

    /**
     * Inserts prescription + outbox row in one transaction. On unique-key race for the same idempotency key,
     * loads the winner row and returns its view.
     */
    private PrescriptionDtos.PrescriptionView insertNewPrescription(
            String doctorId, String idempotencyKey, PrescriptionDtos.CreatePrescriptionRequest r) {
        Prescription p = new Prescription();
        p.setDoctorId(doctorId);
        p.setPatientId(r.patientId());
        p.setPatientEmail(r.patientEmail());
        p.setPatientName(r.patientName());
        p.setPharmacyId(r.pharmacyId());
        p.setMedicationName(r.medicationName());
        p.setDosage(r.dosage());
        p.setInstructions(r.instructions());
        p.setQuantity(r.quantity());
        p.setStatus(PrescriptionStatus.NEW);
        p.setRefillsAllowed(r.refillsAllowed());
        p.setIdempotencyKey(idempotencyKey);
        try {
            Prescription saved = repository.save(p);
            enqueuePrescriptionCreated(saved);
            return toView(saved);
        } catch (DataIntegrityViolationException ex) {
            return repository
                    .findByDoctorIdAndIdempotencyKey(doctorId, idempotencyKey)
                    .map(this::toView)
                    .orElseThrow(() -> ex);
        }
    }

    private void enqueuePrescriptionCreated(Prescription saved) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("prescriptionId", saved.getId().toString());
        payload.put("doctorId", saved.getDoctorId());
        payload.put("patientId", saved.getPatientId());
        payload.put("patientEmail", saved.getPatientEmail());
        payload.put("patientName", saved.getPatientName());
        payload.put("pharmacyId", saved.getPharmacyId());
        payload.put("medicationName", saved.getMedicationName());
        payload.put("dosage", saved.getDosage());
        payload.put("quantity", saved.getQuantity());
        payload.put("createdAt", saved.getCreatedAt().toString());
        OutboxEvent ev = new OutboxEvent();
        ev.setQueueName(PRESCRIPTION_CREATED_QUEUE);
        try {
            ev.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
        outboxEventRepository.save(ev);
    }

    public String status(String doctorId, String id) {
        var p = repository
                .findByIdAndDoctorId(UUID.fromString(id), doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));
        return p.getStatus().name();
    }

    private PrescriptionDtos.PrescriptionView toView(Prescription p) {
        return new PrescriptionDtos.PrescriptionView(
                p.getId().toString(),
                p.getDoctorId(),
                p.getPatientId(),
                p.getPatientEmail(),
                p.getPatientName(),
                p.getPharmacyId(),
                p.getMedicationName(),
                p.getDosage(),
                p.getInstructions(),
                p.getQuantity(),
                p.getStatus().name(),
                p.getRejectionReason(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getRefillsAllowed(),
                p.getRefillsUsed());
    }
}
