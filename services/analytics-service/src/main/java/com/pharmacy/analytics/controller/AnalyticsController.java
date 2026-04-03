package com.pharmacy.analytics.controller;
import com.pharmacy.analytics.common.ApiResponse;
import com.pharmacy.analytics.messaging.PgmqService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/analytics")
public class AnalyticsController {
  private final PgmqService pgmqService;
  private final List<Map<String,Object>> lowStockEvents = new ArrayList<>();
  public AnalyticsController(PgmqService pgmqService) { this.pgmqService = pgmqService; }
  @Scheduled(fixedDelay = 5000)
  public void pollLowStock() {
    List<Map<String,Object>> rows = pgmqService.readMessages("inventory_low_stock", 30, 20);
    for (Map<String,Object> r : rows) {
      lowStockEvents.add(r);
      Object idObj = r.get("msg_id");
      if (idObj != null) pgmqService.deleteMessage("inventory_low_stock", Long.parseLong(String.valueOf(idObj)));
    }
  }
  @GetMapping("/prescriptions/volume") @Operation(summary="Volume") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ApiResponse<?> volume(@RequestParam String pharmacyId, @RequestParam String period){ return ApiResponse.ok(Map.of("pharmacyId", pharmacyId, "period", period, "count", 0), "Fetched"); }
  @GetMapping("/revenue") @Operation(summary="Revenue") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ApiResponse<?> revenue(@RequestParam String pharmacyId, @RequestParam String from, @RequestParam String to){ return ApiResponse.ok(Map.of("pharmacyId", pharmacyId, "revenue", 0), "Fetched"); }
  @GetMapping("/controlled-substances") @Operation(summary="Controlled substances") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ApiResponse<?> controlled(@RequestParam String pharmacyId, @RequestParam String from, @RequestParam String to){ return ApiResponse.ok(List.of(), "Fetched"); }
  @GetMapping("/kpi") @Operation(summary="KPI") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
  public ApiResponse<?> kpi(@RequestParam String pharmacyId){ return ApiResponse.ok(Map.of("totalPrescriptions", 0, "avgProcessingTime", 0, "revenueThisPeriod", 0, "rejectionRate", 0), "Fetched"); }
}
