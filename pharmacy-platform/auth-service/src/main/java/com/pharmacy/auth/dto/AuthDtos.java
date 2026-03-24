package com.pharmacy.auth.dto;

import com.pharmacy.auth.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Email String email,
            @Size(min = 8) String password,
            @NotNull UserRole role
    ) {
    }

    public record LoginRequest(@Email String email, @Size(min = 8) String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record UserView(
            String id,
            String firstName,
            String lastName,
            String email,
            String role,
            boolean active
    ) {
    }

    public record AuthResponse(String accessToken, String refreshToken, UserView user) {
    }
}
