package com.pharmacy.prescription.controller;
import com.pharmacy.prescription.common.ApiResponse;
import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/doctor")
public class PrescriptionController {
  private final PrescriptionService service;
  public PrescriptionController(PrescriptionService service){ this.service = service; }
  @GetMapping("/prescriptions") @Operation(summary="Get doctor prescriptions") @PreAuthorize("hasRole('DOCTOR')")
  public ApiResponse<List<PrescriptionDtos.PrescriptionView>> list(@RequestHeader("X-User-Id") String doctorId){ return ApiResponse.ok(service.byDoctor(doctorId), "Fetched"); }
  @PostMapping("/prescriptions") @Operation(summary="Create prescription") @PreAuthorize("hasRole('DOCTOR')")
  public ApiResponse<PrescriptionDtos.PrescriptionView> create(@RequestHeader("X-User-Id") String doctorId, @Valid @RequestBody PrescriptionDtos.CreatePrescriptionRequest r){ return ApiResponse.ok(service.create(doctorId, r), "Created"); }
  @GetMapping("/prescriptions/{id}/status") @Operation(summary="Get status") @PreAuthorize("hasRole('DOCTOR')")
  public ApiResponse<String> status(@RequestHeader("X-User-Id") String doctorId, @PathVariable String id){ return ApiResponse.ok(service.status(doctorId, id), "Status"); }
  @GetMapping("/pharmacies") @Operation(summary="Get pharmacies") @PreAuthorize("hasRole('DOCTOR')")
  public ApiResponse<?> pharmacies(){ return ApiResponse.ok(List.of(Map.of("id","default-pharmacy","name","Central Pharmacy")), "Fetched"); }
}
