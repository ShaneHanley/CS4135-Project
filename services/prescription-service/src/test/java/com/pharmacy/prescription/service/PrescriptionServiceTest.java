package com.pharmacy.prescription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.entity.OutboxEvent;
import com.pharmacy.prescription.entity.Prescription;
import com.pharmacy.prescription.entity.PrescriptionStatus;
import com.pharmacy.prescription.repository.OutboxEventRepository;
import com.pharmacy.prescription.repository.PrescriptionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    PrescriptionRepository repository;

    @Mock
    OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PrescriptionService service;

    @BeforeEach
    void setUp() {
        service = new PrescriptionService(repository, outboxEventRepository, objectMapper);
    }

    @Test
    void create_savesPrescriptionAndOutboxEvent() throws Exception {
        String doctorId = UUID.randomUUID().toString();
        String idem = "idem-" + UUID.randomUUID();
        PrescriptionDtos.CreatePrescriptionRequest req =
                new PrescriptionDtos.CreatePrescriptionRequest(
                        "patient-1",
                        "patient@demo.com",
                        "Demo Patient",
                        "pharmacy-1",
                        "Amoxicillin",
                        "500mg",
                        "Take with food",
                        21,
                        1);

        when(repository.findByDoctorIdAndIdempotencyKey(doctorId, idem)).thenReturn(Optional.empty());
        Prescription saved = prescriptionWith(UUID.randomUUID(), doctorId, PrescriptionStatus.NEW);
        saved.setIdempotencyKey(idem);
        when(repository.save(any(Prescription.class))).thenReturn(saved);

        PrescriptionDtos.PrescriptionView view = service.create(doctorId, idem, req);

        assertThat(view.status()).isEqualTo("NEW");
        assertThat(view.doctorId()).isEqualTo(doctorId);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent out = outboxCaptor.getValue();
        assertThat(out.getQueueName()).isEqualTo(PrescriptionService.PRESCRIPTION_CREATED_QUEUE);
        assertThat(objectMapper.readTree(out.getPayload()).get("prescriptionId").asText())
                .isEqualTo(saved.getId().toString());
    }

    @Test
    void create_sameIdempotencyKey_returnsExistingWithoutSavingAgain() {
        String doctorId = UUID.randomUUID().toString();
        String idem = "shared-key";
        PrescriptionDtos.CreatePrescriptionRequest req =
                new PrescriptionDtos.CreatePrescriptionRequest(
                        "patient-1",
                        "patient@demo.com",
                        "Demo Patient",
                        "pharmacy-1",
                        "Amoxicillin",
                        "500mg",
                        "Take with food",
                        21,
                        1);
        Prescription existing = prescriptionWith(UUID.randomUUID(), doctorId, PrescriptionStatus.NEW);
        existing.setIdempotencyKey(idem);
        when(repository.findByDoctorIdAndIdempotencyKey(doctorId, idem)).thenReturn(Optional.of(existing));

        PrescriptionDtos.PrescriptionView view = service.create(doctorId, idem, req);

        assertThat(view.prescriptionId()).isEqualTo(existing.getId().toString());
        verify(repository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void byDoctor_returnsOnlyMatchingPrescriptions() {
        String doctorId = UUID.randomUUID().toString();
        Prescription p = prescriptionWith(UUID.randomUUID(), doctorId, PrescriptionStatus.NEW);
        when(repository.findByDoctorId(doctorId)).thenReturn(List.of(p));

        List<PrescriptionDtos.PrescriptionView> results = service.byDoctor(doctorId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).doctorId()).isEqualTo(doctorId);
    }

    @Test
    void status_prescriptionFound_returnsStatusName() {
        String doctorId = UUID.randomUUID().toString();
        UUID id = UUID.randomUUID();
        Prescription p = prescriptionWith(id, doctorId, PrescriptionStatus.NEW);
        when(repository.findByIdAndDoctorId(id, doctorId)).thenReturn(Optional.of(p));

        String status = service.status(doctorId, id.toString());

        assertThat(status).isEqualTo("NEW");
    }

    @Test
    void status_prescriptionNotFound_throwsIllegalArgument() {
        String doctorId = UUID.randomUUID().toString();
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndDoctorId(id, doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.status(doctorId, id.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prescription not found");
    }

    @Test
    void create_blankIdempotencyKey_throws() {
        assertThatThrownBy(
                        () ->
                                service.create(
                                        UUID.randomUUID().toString(),
                                        "   ",
                                        new PrescriptionDtos.CreatePrescriptionRequest(
                                                "p", "e@e.com", "N", "ph", "m", "d", "i", 1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X-Idempotency-Key");
    }

    private Prescription prescriptionWith(UUID id, String doctorId, PrescriptionStatus status) {
        Prescription p = new Prescription();
        p.setDoctorId(doctorId);
        p.setPatientId("patient-1");
        p.setPatientEmail("patient@demo.com");
        p.setPatientName("Demo Patient");
        p.setPharmacyId("pharmacy-1");
        p.setMedicationName("Amoxicillin");
        p.setDosage("500mg");
        p.setInstructions("Take with food");
        p.setQuantity(21);
        p.setRefillsAllowed(1);
        p.setStatus(status);
        p.onCreate();
        try {
            var f = Prescription.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {
        }
        return p;
    }
}
