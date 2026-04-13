package com.pharmacy.prescription.controller;

import com.pharmacy.prescription.common.ApiResponse;
import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor/profile")
public class DoctorController {

    private final DoctorService service;

    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create doctor profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<PrescriptionDtos.DoctorView> create(
            @RequestHeader("X-User-Id") String doctorId,
            @Valid @RequestBody PrescriptionDtos.CreateDoctorProfileRequest request) {
        return ApiResponse.ok(service.createProfile(doctorId, request), "Profile created");
    }

    @GetMapping
    @Operation(summary = "Get doctor profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<PrescriptionDtos.DoctorView> get(
            @RequestHeader("X-User-Id") String doctorId) {
        return ApiResponse.ok(service.getProfile(doctorId), "Fetched");
    }

    @PutMapping
    @Operation(summary = "Update doctor profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ApiResponse<PrescriptionDtos.DoctorView> update(
            @RequestHeader("X-User-Id") String doctorId,
            @Valid @RequestBody PrescriptionDtos.UpdateDoctorProfileRequest request) {
        return ApiResponse.ok(service.updateProfile(doctorId, request), "Profile updated");
    }
}
