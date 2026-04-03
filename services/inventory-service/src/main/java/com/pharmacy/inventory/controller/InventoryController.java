package com.pharmacy.inventory.controller;
import com.pharmacy.inventory.common.ApiResponse;
import com.pharmacy.inventory.messaging.PgmqService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/inventory")
public class InventoryController {
  private final PgmqService pgmqService;
  public InventoryController(PgmqService pgmqService) { this.pgmqService = pgmqService; }
  @GetMapping("/medications") @Operation(summary="List medications") @PreAuthorize("hasAnyRole('PHARMACIST','TECHNICIAN','MANAGER')")
  public ApiResponse<?> medications(){ return ApiResponse.ok(List.of(), "Fetched"); }
  @GetMapping("/lots") @Operation(summary="List lots") @PreAuthorize("hasAnyRole('PHARMACIST','TECHNICIAN','MANAGER')")
  public ApiResponse<?> lots(@RequestParam String pharmacyId){ return ApiResponse.ok(List.of(Map.of("pharmacyId", pharmacyId)), "Fetched"); }
  @PostMapping("/dispense") @Operation(summary="Dispense stock") @PreAuthorize("hasAnyRole('PHARMACIST','TECHNICIAN')")
  public ApiResponse<?> dispense(@RequestBody Map<String,Object> body){
    pgmqService.sendMessage("inventory_low_stock", Map.of("medicationId", body.getOrDefault("medicationId",""), "medicationName", body.getOrDefault("medicationName",""), "pharmacyId", body.getOrDefault("pharmacyId",""), "currentQuantity", 0, "reorderThreshold", 1, "lotNumber", body.getOrDefault("lotNumber","")));
    return ApiResponse.ok(body, "Dispensed");
  }
  @PostMapping("/receive") @Operation(summary="Receive stock") @PreAuthorize("hasRole('MANAGER')")
  public ApiResponse<?> receive(@RequestBody Map<String,Object> body){ return ApiResponse.ok(body, "Received"); }
  @GetMapping("/transactions") @Operation(summary="Transactions") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ApiResponse<?> transactions(){ return ApiResponse.ok(List.of(), "Fetched"); }
}
