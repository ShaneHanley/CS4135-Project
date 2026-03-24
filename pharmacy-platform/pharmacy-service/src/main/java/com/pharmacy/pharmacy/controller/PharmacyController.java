package com.pharmacy.pharmacy.controller;
import com.pharmacy.pharmacy.common.ApiResponse;
import com.pharmacy.pharmacy.service.PharmacyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {
  private final PharmacyService service;
  public PharmacyController(PharmacyService service) { this.service = service; }
  @GetMapping("/prescriptions") @Operation(summary="Pharmacy prescriptions") @PreAuthorize("hasAnyRole('PHARMACIST','TECHNICIAN','MANAGER')")
  public ApiResponse<?> list(){ return ApiResponse.ok(service.list(), "Fetched"); }
  @GetMapping("/prescriptions/{id}") @Operation(summary="Get pharmacy prescription") @PreAuthorize("hasAnyRole('PHARMACIST','TECHNICIAN','MANAGER')")
  public ApiResponse<?> one(@PathVariable String id){ return ApiResponse.ok(service.one(id), "Fetched"); }
  public record StatusUpdateRequest(@NotBlank String status, String rejectionReason) {}
  @PutMapping("/prescriptions/{id}/status") @Operation(summary="Update prescription status") @PreAuthorize("hasAnyRole('PHARMACIST','MANAGER')")
  public ApiResponse<?> update(@PathVariable String id, @RequestBody StatusUpdateRequest r){ return ApiResponse.ok(service.update(id, r.status(), r.rejectionReason()), "Updated"); }
  @GetMapping("/dashboard/stats") @Operation(summary="Dashboard stats") @PreAuthorize("hasRole('MANAGER')")
  public ApiResponse<?> stats(){ return ApiResponse.ok(service.stats(), "Fetched"); }
}
