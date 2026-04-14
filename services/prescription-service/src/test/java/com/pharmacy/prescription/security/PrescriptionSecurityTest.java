package com.pharmacy.prescription.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.prescription.config.GatewayHeaderAuthFilter;
import com.pharmacy.prescription.config.SecurityConfig;
import com.pharmacy.prescription.controller.DoctorController;
import com.pharmacy.prescription.controller.PrescriptionController;
import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.service.DoctorService;
import com.pharmacy.prescription.service.PharmacyService;
import com.pharmacy.prescription.service.PrescriptionService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {PrescriptionController.class, DoctorController.class})
@Import({SecurityConfig.class, GatewayHeaderAuthFilter.class})
class PrescriptionSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PrescriptionService prescriptionService;

    @MockBean
    PharmacyService pharmacyService;

    @MockBean
    DoctorService doctorService;

    // --- Unauthenticated (no headers) → 403 (stateless session policy has no redirect entry point) ---

    @Test
    void prescriptions_noHeaders_returns403() throws Exception {
        mockMvc.perform(get("/api/doctor/prescriptions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pharmacies_noHeaders_returns403() throws Exception {
        mockMvc.perform(get("/api/doctor/pharmacies"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorProfile_noHeaders_returns403() throws Exception {
        mockMvc.perform(get("/api/doctor/profile"))
                .andExpect(status().isForbidden());
    }

    // --- Wrong role (PHARMACIST) → 403 ---

    @Test
    void prescriptions_pharmacistRole_returns403() throws Exception {
        mockMvc.perform(get("/api/doctor/prescriptions")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "PHARMACIST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorProfile_pharmacistRole_returns403() throws Exception {
        mockMvc.perform(get("/api/doctor/profile")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "PHARMACIST"))
                .andExpect(status().isForbidden());
    }

    // --- Correct role (DOCTOR) → 200 ---

    @Test
    void prescriptions_doctorRole_returns200() throws Exception {
        String doctorId = UUID.randomUUID().toString();
        when(prescriptionService.byDoctor(doctorId)).thenReturn(List.of());

        mockMvc.perform(get("/api/doctor/prescriptions")
                        .header("X-User-Id", doctorId)
                        .header("X-User-Role", "DOCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void pharmacies_doctorRole_returns200() throws Exception {
        when(pharmacyService.listActive()).thenReturn(List.of());

        mockMvc.perform(get("/api/doctor/pharmacies")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "DOCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void doctorProfile_doctorRole_returns200() throws Exception {
        String doctorId = UUID.randomUUID().toString();
        PrescriptionDtos.DoctorView view = new PrescriptionDtos.DoctorView(
                doctorId, "Alice", "Smith", "alice@demo.com", "LIC-001", "061-999", true, null, null);
        when(doctorService.getProfile(doctorId)).thenReturn(view);

        mockMvc.perform(get("/api/doctor/profile")
                        .header("X-User-Id", doctorId)
                        .header("X-User-Role", "DOCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.licenseNumber").value("LIC-001"));
    }

    // --- Validation: missing required fields → 400 ---

    @Test
    void createPrescription_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/doctor/prescriptions")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "DOCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDoctorProfile_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/doctor/profile")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "DOCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDoctorProfile_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/doctor/profile")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "DOCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Alice",
                                "lastName", "Smith",
                                "email", "not-an-email",
                                "licenseNumber", "LIC-001"))))
                .andExpect(status().isBadRequest());
    }
}
