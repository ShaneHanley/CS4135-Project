package com.pharmacy.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmacy.auth.dto.AuthDtos;
import com.pharmacy.auth.entity.User;
import com.pharmacy.auth.entity.UserRole;
import com.pharmacy.auth.exception.DuplicateEmailException;
import com.pharmacy.auth.exception.InvalidCredentialsException;
import com.pharmacy.auth.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthService authService;

    private AuthDtos.RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthDtos.RegisterRequest("A", "B", "a@b.com", "password12", UserRole.PATIENT);
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> authService.register(registerRequest)).isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_success_savesUser() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password12")).thenReturn("encoded");
        User saved = new User();
        saved.setId(UUID.randomUUID());
        saved.setFirstName("A");
        saved.setLastName("B");
        saved.setEmail("a@b.com");
        saved.setRole(UserRole.PATIENT);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        AuthDtos.UserView view = authService.register(registerRequest);
        assertThat(view.email()).isEqualTo("a@b.com");
        assertThat(view.role()).isEqualTo("PATIENT");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_invalidPassword_throws() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("a@b.com");
        u.setPasswordHash("hash");
        u.setActive(true);
        u.setRole(UserRole.PATIENT);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrongpassword12", "hash")).thenReturn(false);
        AuthDtos.LoginRequest login = new AuthDtos.LoginRequest("a@b.com", "wrongpassword12");
        assertThatThrownBy(() -> authService.login(login)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_success_returnsTokensAndRole() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("a@b.com");
        u.setPasswordHash("hash");
        u.setActive(true);
        u.setRole(UserRole.PATIENT);
        u.setFirstName("A");
        u.setLastName("B");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("password12", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(u)).thenReturn("access");
        when(jwtService.generateRefreshToken(u)).thenReturn("refresh");
        AuthDtos.AuthResponse resp = authService.login(new AuthDtos.LoginRequest("a@b.com", "password12"));
        assertThat(resp.accessToken()).isEqualTo("access");
        assertThat(resp.refreshToken()).isEqualTo("refresh");
        assertThat(resp.user().role()).isEqualTo("PATIENT");
    }
}
