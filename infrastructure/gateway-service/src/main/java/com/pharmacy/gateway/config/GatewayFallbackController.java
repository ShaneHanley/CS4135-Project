package com.pharmacy.gateway.config;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class GatewayFallbackController {

    @GetMapping("/doctor")
    public ResponseEntity<Map<String, String>> doctorFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "DOCTOR_SERVICE_UNAVAILABLE"));
    }

    @GetMapping("/pharmacy")
    public ResponseEntity<Map<String, String>> pharmacyFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "PHARMACY_SERVICE_UNAVAILABLE"));
    }
}
