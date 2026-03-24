package com.pharmacy.patient.controller;
import com.pharmacy.patient.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/patients")
public class PatientController {
  private final Map<String,Object> profile = new LinkedHashMap<>(Map.of("deliveryPreference","PICKUP"));
  @GetMapping("/me") @Operation(summary="Get patient profile") @PreAuthorize("hasRole('PATIENT')")
  public ApiResponse<?> me(){ return ApiResponse.ok(profile, "Fetched"); }
  @PutMapping("/me") @Operation(summary="Update patient profile") @PreAuthorize("hasRole('PATIENT')")
  public ApiResponse<?> update(@RequestBody Map<String,Object> body){ profile.putAll(body); return ApiResponse.ok(profile, "Updated"); }
  @GetMapping("/me/prescriptions") @Operation(summary="Get patient prescriptions") @PreAuthorize("hasRole('PATIENT')")
  public ApiResponse<?> prescriptions(){ return ApiResponse.ok(List.of(), "Fetched"); }
  @PostMapping("/prescriptions/{id}/refill") @Operation(summary="Request refill") @PreAuthorize("hasRole('PATIENT')")
  public ApiResponse<?> refill(@PathVariable String id){ return ApiResponse.ok(Map.of("prescriptionId", id, "status", "PENDING"), "Requested"); }
  @GetMapping("/{id}") @Operation(summary="Get patient by id") @PreAuthorize("hasAnyRole('PHARMACIST','DOCTOR','ADMIN')")
  public ApiResponse<?> byId(@PathVariable String id){ return ApiResponse.ok(Map.of("id", id), "Fetched"); }
}
