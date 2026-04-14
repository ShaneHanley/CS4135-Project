package com.pharmacy.prescription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmacy.messaging.PgmqService;
import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.entity.Prescription;
import com.pharmacy.prescription.entity.PrescriptionStatus;
import com.pharmacy.prescription.repository.PrescriptionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    PrescriptionRepository repository;

    @Mock
    PgmqService pgmqService;

    @InjectMocks
    PrescriptionService service;

    @Test
    void create_savesAndPublishesEvent() {
        String doctorId = UUID.randomUUID().toString();
        PrescriptionDtos.CreatePrescriptionRequest req = new PrescriptionDtos.CreatePrescriptionRequest(
                "patient-1", "patient@demo.com", "Demo Patient",
                "pharmacy-1", "Amoxicillin", "500mg", "Take with food", 21, 1);

        Prescription saved = prescriptionWith(UUID.randomUUID(), doctorId, PrescriptionStatus.NEW);
        when(repository.save(any(Prescription.class))).thenReturn(saved);

        PrescriptionDtos.PrescriptionView view = service.create(doctorId, req);

        assertThat(view.status()).isEqualTo("NEW");
        assertThat(view.doctorId()).isEqualTo(doctorId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pgmqService).sendMessage(eq("prescription_created"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsKey("prescriptionId");
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
        } catch (Exception ignored) {}
        return p;
    }
}
