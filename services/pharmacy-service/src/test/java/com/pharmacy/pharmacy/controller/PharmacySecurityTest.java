package com.pharmacy.pharmacy.controller;

import com.pharmacy.pharmacy.config.GatewayHeaderAuthFilter;
import com.pharmacy.pharmacy.config.SecurityConfig;
import com.pharmacy.pharmacy.entity.PharmacyPrescription;
import com.pharmacy.pharmacy.entity.PharmacyPrescriptionStatus;
import com.pharmacy.pharmacy.service.PharmacyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PharmacyController.class)
@Import({SecurityConfig.class, GatewayHeaderAuthFilter.class})
class PharmacySecurityTest {

    @Autowired MockMvc mockMvc;
    @MockBean PharmacyService pharmacyService;

    // --- GET /api/pharmacy/prescriptions ---

    @Test
    void listPrescriptions_noHeaders_returns403() throws Exception {
        mockMvc.perform(get("/api/pharmacy/prescriptions"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listPrescriptions_asPatient_returns403() throws Exception {
        mockMvc.perform(get("/api/pharmacy/prescriptions")
            .header("X-User-Id", "user-1")
            .header("X-User-Role", "PATIENT"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listPrescriptions_asTechnician_returns200() throws Exception {
        when(pharmacyService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/pharmacy/prescriptions")
            .header("X-User-Id", "user-1")
            .header("X-User-Role", "TECHNICIAN"))
            .andExpect(status().isOk());
    }

    @Test
    void listPrescriptions_asPharmacist_returns200() throws Exception {
        when(pharmacyService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/pharmacy/prescriptions")
            .header("X-User-Id", "user-1")
            .header("X-User-Role", "PHARMACIST"))
            .andExpect(status().isOk());
    }

    @Test
    void listPrescriptions_asManager_returns200() throws Exception {
        when(pharmacyService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/pharmacy/prescriptions")
            .header("X-User-Id", "user-1")
            .header("X-User-Role", "MANAGER"))
            .andExpect(status().isOk());
    }

    // --- PUT /api/pharmacy/prescriptions/{id}/status ---

    @Test
    void updateStatus_asTechnician_returns403() throws Exception {
        mockMvc.perform(put("/api/pharmacy/prescriptions/some-id/status")
            .header("X-User-Id", "user-1")
            .header("X-User-Role", "TECHNICIAN")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"PROCESSING\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_asPharmacist_returns200() throws Exception {
        PharmacyPrescription p = new PharmacyPrescription();
        p.setPrescriptionId(UUID.randomUUID());
        p.setStatus(PharmacyPrescriptionStatus.PROCESSING);
        when(pharmacyService.update(any(), any(), any())).thenReturn(p);

        mockMvc.perform(put("/api/pharmacy/prescriptions/some-id/status")
            .header("X-User-Id", "user-1")
            .header("X-User-Role", "PHARMACIST")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"PROCESSING\"}"))
            .andExpect(status().isOk());
    }

    // --- GET /api/pharmacy/dashboard/stats ---

    @Test
    void dashboardStats_asPharmacist_returns403() throws Exception {
        mockMvc.perform(get("/api/pharmacy/dashboard/stats")
            .header("X-User-Id", "user-1")
            .header("X-User-Role", "PHARMACIST"))
            .andExpect(status().isForbidden());
    }

    @Test
    void dashboardStats_asManager_returns200() throws Exception {
        when(pharmacyService.stats()).thenReturn(Map.of("NEW", 3L, "PROCESSING", 1L));
        mockMvc.perform(get("/api/pharmacy/dashboard/stats")
            .header("X-User-Id", "user-1")
            .header("X-User-Role", "MANAGER"))
            .andExpect(status().isOk());
    }
}
