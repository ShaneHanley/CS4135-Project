package com.pharmacy.pharmacy.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;

  public SecurityConfig(GatewayHeaderAuthFilter gatewayHeaderAuthFilter) {
    this.gatewayHeaderAuthFilter = gatewayHeaderAuthFilter;
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth.requestMatchers("/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html","/actuator/health").permitAll().anyRequest().authenticated())
      .addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
