package com.pharmacy.gateway.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
@Configuration
public class GatewayRoutes {
  @Bean
  CorsFilter corsFilter(@Value("${REACT_APP_ORIGIN:http://localhost:3000}") String origin) {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.addAllowedOrigin(origin); cfg.addAllowedMethod("*"); cfg.addAllowedHeader("*"); cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return new CorsFilter(src);
  }
}
