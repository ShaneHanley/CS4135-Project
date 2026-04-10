package com.pharmacy.auth.service;

import com.pharmacy.auth.dto.AuthDtos;
import com.pharmacy.auth.entity.User;
import com.pharmacy.auth.exception.DuplicateEmailException;
import com.pharmacy.auth.exception.InvalidCredentialsException;
import com.pharmacy.auth.exception.InvalidRefreshTokenException;
import com.pharmacy.auth.exception.UserNotFoundException;
import com.pharmacy.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthDtos.UserView register(AuthDtos.RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(normalizedEmail).ifPresent(u -> {
            throw new DuplicateEmailException("Email already registered");
        });
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        User saved = userRepository.save(user);
        return toUserView(saved);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return new AuthDtos.AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                toUserView(user)
        );
    }

    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }
        String email = claims.getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
        return new AuthDtos.AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                toUserView(user)
        );
    }

    public AuthDtos.UserView me(String email) {
        User user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return toUserView(user);
    }

    private AuthDtos.UserView toUserView(User user) {
        return new AuthDtos.UserView(
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive()
        );
    }
}
