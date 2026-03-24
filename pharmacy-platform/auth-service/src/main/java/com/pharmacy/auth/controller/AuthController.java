package com.pharmacy.auth.controller;

import com.pharmacy.auth.common.ApiResponse;
import com.pharmacy.auth.dto.AuthDtos;
import com.pharmacy.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register")
    public ApiResponse<AuthDtos.UserView> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Registered");
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "Logged in");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh")
    public ApiResponse<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request), "Refreshed");
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public ApiResponse<String> logout() {
        return ApiResponse.ok("ok", "Logged out");
    }

    @GetMapping("/me")
    @Operation(summary = "Me")
    public ApiResponse<AuthDtos.UserView> me(Authentication authentication) {
        return ApiResponse.ok(authService.me(authentication.getName()), "Current user");
    }
}
