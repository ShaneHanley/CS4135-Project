package com.pharmacy.billing.controller;
import com.pharmacy.billing.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/billing")
public class BillingController {
  @PostMapping("/invoices") @Operation(summary="Create invoice") @PreAuthorize("hasAnyRole('PHARMACIST','MANAGER')")
  public ApiResponse<?> create(@RequestBody Map<String,Object> body){ return ApiResponse.ok(body, "Created"); }
  @GetMapping("/invoices") @Operation(summary="Get invoices") @PreAuthorize("hasAnyRole('PATIENT','MANAGER')")
  public ApiResponse<?> invoices(@RequestParam(required = false) String patientId){ return ApiResponse.ok(List.of(Map.of("patientId", patientId)), "Fetched"); }
  @PostMapping("/invoices/{id}/pay") @Operation(summary="Pay invoice") @PreAuthorize("hasRole('PHARMACIST')")
  public ApiResponse<?> pay(@PathVariable String id, @RequestBody Map<String,Object> body){ return ApiResponse.ok(Map.of("invoiceId", id, "payment", body), "Paid"); }
  @GetMapping("/summary") @Operation(summary="Billing summary") @PreAuthorize("hasRole('MANAGER')")
  public ApiResponse<?> summary(@RequestParam String pharmacyId, @RequestParam String from, @RequestParam String to){ return ApiResponse.ok(Map.of("pharmacyId", pharmacyId, "from", from, "to", to), "Fetched"); }
}
