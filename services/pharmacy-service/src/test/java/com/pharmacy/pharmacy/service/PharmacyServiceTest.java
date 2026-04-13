package com.pharmacy.pharmacy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.messaging.PgmqService;
import com.pharmacy.pharmacy.entity.PharmacyPrescription;
import com.pharmacy.pharmacy.entity.PharmacyPrescriptionStatus;
import com.pharmacy.pharmacy.repository.PharmacyPrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock PharmacyPrescriptionRepository repository;
    @Mock PgmqService pgmqService;

    PharmacyService service;

    @BeforeEach
    void setUp() {
        service = new PharmacyService(repository, pgmqService, new ObjectMapper());
    }

    // --- pollPrescriptionCreated ---

    @Test
    void poll_skips_duplicate_prescription() {
        UUID prescriptionId = UUID.randomUUID();
        when(pgmqService.readMessages(eq("prescription_created"), anyInt(), anyInt()))
            .thenReturn(List.of(row(prescriptionId, 1L)));
        when(repository.findByPrescriptionId(prescriptionId))
            .thenReturn(Optional.of(new PharmacyPrescription()));

        service.pollPrescriptionCreated();

        verify(repository, never()).save(any());
        verify(pgmqService).deleteMessage("prescription_created", 1L);
    }

    @Test
    void poll_saves_new_prescription_with_NEW_status() {
        UUID prescriptionId = UUID.randomUUID();
        when(pgmqService.readMessages(eq("prescription_created"), anyInt(), anyInt()))
            .thenReturn(List.of(row(prescriptionId, 42L)));
        when(repository.findByPrescriptionId(prescriptionId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.pollPrescriptionCreated();

        ArgumentCaptor<PharmacyPrescription> captor = ArgumentCaptor.forClass(PharmacyPrescription.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PharmacyPrescriptionStatus.NEW);
        assertThat(captor.getValue().getMedicationName()).isEqualTo("Aspirin");
        verify(pgmqService).deleteMessage("prescription_created", 42L);
    }

    // --- update ---

    @Test
    void update_rejected_without_reason_throws() {
        UUID id = UUID.randomUUID();
        when(repository.findByPrescriptionId(id))
            .thenReturn(Optional.of(prescription(id, PharmacyPrescriptionStatus.PROCESSING)));

        assertThatThrownBy(() -> service.update(id.toString(), "REJECTED", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rejectionReason");
    }

    @Test
    void update_processing_publishes_correct_notification() {
        UUID id = UUID.randomUUID();
        when(repository.findByPrescriptionId(id))
            .thenReturn(Optional.of(prescription(id, PharmacyPrescriptionStatus.NEW)));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(id.toString(), "PROCESSING", null);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(pgmqService).sendMessage(eq("notifications"), captor.capture());
        Map<?, ?> payload = captor.getValue();
        assertThat(payload.get("channel")).isEqualTo("email");
        assertThat(payload.get("type")).isEqualTo("processing");
        assertThat(payload.get("recipient")).isEqualTo("patient@test.com");
        assertThat((String) payload.get("subject")).contains("being prepared");
    }

    @Test
    void update_ready_for_pickup_publishes_collect_type() {
        UUID id = UUID.randomUUID();
        when(repository.findByPrescriptionId(id))
            .thenReturn(Optional.of(prescription(id, PharmacyPrescriptionStatus.PROCESSING)));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(id.toString(), "READY_FOR_PICKUP", null);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(pgmqService).sendMessage(eq("notifications"), captor.capture());
        assertThat(captor.getValue().get("type")).isEqualTo("collect");
    }

    @Test
    void update_dispensed_publishes_dispensed_type() {
        UUID id = UUID.randomUUID();
        when(repository.findByPrescriptionId(id))
            .thenReturn(Optional.of(prescription(id, PharmacyPrescriptionStatus.READY_FOR_PICKUP)));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(id.toString(), "DISPENSED", null);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(pgmqService).sendMessage(eq("notifications"), captor.capture());
        assertThat(captor.getValue().get("type")).isEqualTo("dispensed");
    }

    @Test
    void update_rejected_publishes_body_containing_reason() {
        UUID id = UUID.randomUUID();
        when(repository.findByPrescriptionId(id))
            .thenReturn(Optional.of(prescription(id, PharmacyPrescriptionStatus.PROCESSING)));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(id.toString(), "REJECTED", "Out of stock");

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(pgmqService).sendMessage(eq("notifications"), captor.capture());
        Map<?, ?> payload = captor.getValue();
        assertThat(payload.get("type")).isEqualTo("rejected");
        assertThat((String) payload.get("body")).contains("Out of stock");
    }

    // --- one ---

    @Test
    void one_throws_when_prescription_not_found() {
        UUID id = UUID.randomUUID();
        when(repository.findByPrescriptionId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.one(id.toString()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // --- stats ---

    @Test
    void stats_returns_counts_grouped_by_status() {
        when(repository.findAll()).thenReturn(List.of(
            prescription(UUID.randomUUID(), PharmacyPrescriptionStatus.NEW),
            prescription(UUID.randomUUID(), PharmacyPrescriptionStatus.NEW),
            prescription(UUID.randomUUID(), PharmacyPrescriptionStatus.PROCESSING)
        ));

        Map<String, Long> stats = service.stats();

        assertThat(stats.get("NEW")).isEqualTo(2L);
        assertThat(stats.get("PROCESSING")).isEqualTo(1L);
    }

    // --- helpers ---

    private Map<String, Object> row(UUID prescriptionId, long msgId) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("prescriptionId", prescriptionId.toString());
        msg.put("doctorId", "doc-1");
        msg.put("patientId", "pat-1");
        msg.put("patientEmail", "patient@test.com");
        msg.put("patientName", "Test Patient");
        msg.put("pharmacyId", "pharm-1");
        msg.put("medicationName", "Aspirin");
        msg.put("dosage", "100mg");
        msg.put("quantity", "30");
        Map<String, Object> row = new HashMap<>();
        row.put("message", msg);
        row.put("msg_id", msgId);
        return row;
    }

    private PharmacyPrescription prescription(UUID prescriptionId, PharmacyPrescriptionStatus status) {
        PharmacyPrescription p = new PharmacyPrescription();
        p.setPrescriptionId(prescriptionId);
        p.setDoctorId("doc-1");
        p.setPatientId("pat-1");
        p.setPatientEmail("patient@test.com");
        p.setPatientName("Test Patient");
        p.setPharmacyId("pharm-1");
        p.setMedicationName("Aspirin");
        p.setDosage("100mg");
        p.setQuantity(30);
        p.setStatus(status);
        return p;
    }
}
