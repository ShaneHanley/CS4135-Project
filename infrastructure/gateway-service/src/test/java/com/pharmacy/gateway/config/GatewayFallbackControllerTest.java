package com.pharmacy.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GatewayFallbackControllerTest {

    private final GatewayFallbackController controller = new GatewayFallbackController();

    @Test
    void doctorFallback_returnsServiceUnavailableResponse() {
        ResponseEntity<Map<String, String>> response = controller.doctorFallback();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "DOCTOR_SERVICE_UNAVAILABLE");
    }

    @Test
    void pharmacyFallback_returnsServiceUnavailableResponse() {
        ResponseEntity<Map<String, String>> response = controller.pharmacyFallback();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "PHARMACY_SERVICE_UNAVAILABLE");
    }
}
