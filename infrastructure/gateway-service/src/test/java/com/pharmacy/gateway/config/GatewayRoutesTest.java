package com.pharmacy.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class GatewayRoutesTest {

    @Test
    void corsConfigurationSource_allowsConfiguredOrigin() {
        String origin = "http://localhost:3000";
        SecurityConfig config = new SecurityConfig();
        CorsConfigurationSource source = config.corsConfigurationSource(origin);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients/me");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);

        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).contains(origin);
        assertThat(corsConfig.getAllowCredentials()).isTrue();
    }
}
