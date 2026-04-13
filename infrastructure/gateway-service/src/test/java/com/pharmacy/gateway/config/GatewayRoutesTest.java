package com.pharmacy.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

class GatewayRoutesTest {

    @Test
    void corsFilter_allowsConfiguredOrigin() throws Exception {
        String origin = "http://localhost:3000";
        GatewayRoutes routes = new GatewayRoutes();
        CorsFilter filter = routes.corsFilter(origin);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients/me");
        request.addHeader("Origin", origin);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    }
}
