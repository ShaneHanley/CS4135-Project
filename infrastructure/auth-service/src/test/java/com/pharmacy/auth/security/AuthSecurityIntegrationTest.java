package com.pharmacy.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.auth.entity.User;
import com.pharmacy.auth.entity.UserRole;
import com.pharmacy.auth.repository.UserRepository;
import com.pharmacy.auth.service.JwtService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "nobody@example.com",
                                "password", "password12"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminPing_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/admin/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminPing_asPatient_returns403() throws Exception {
        User u = persistUser("patient-admin-test@example.com", UserRole.PATIENT);
        String token = jwtService.generateAccessToken(u);
        mockMvc.perform(get("/api/auth/admin/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void adminPing_asAdmin_returns200() throws Exception {
        User u = persistUser("admin-ping-test@example.com", UserRole.ADMIN);
        String token = jwtService.generateAccessToken(u);
        mockMvc.perform(get("/api/auth/admin/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void me_withValidToken_returns200() throws Exception {
        User u = persistUser("me-test@example.com", UserRole.DOCTOR);
        String token = jwtService.generateAccessToken(u);
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("me-test@example.com"))
                .andExpect(jsonPath("$.data.role").value("DOCTOR"));
    }

    private User persistUser(String email, UserRole role) {
        User u = new User();
        u.setFirstName("T");
        u.setLastName("U");
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("password12"));
        u.setRole(role);
        return userRepository.save(u);
    }
}
