from pathlib import Path

ROOT = Path(r"c:\Users\josep\cs4135-copy\pharmacy-platform")


def w(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


# Auth service implementation
auth = ROOT / "auth-service/src/main/java/com/pharmacy/auth"
w(
    auth / "common/ApiResponse.java",
    """
package com.pharmacy.auth.common;
import java.time.Instant;
public record ApiResponse<T>(boolean success, T data, String message, Instant timestamp) {
  public static <T> ApiResponse<T> ok(T data, String message) { return new ApiResponse<>(true, data, message, Instant.now()); }
}
""",
)
w(
    auth / "controller/AuthController.java",
    """
package com.pharmacy.auth.controller;
import com.pharmacy.auth.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final Map<String, Map<String,Object>> users = new LinkedHashMap<>();
  public record RegisterRequest(@NotBlank String firstName,@NotBlank String lastName,@Email String email,@Size(min=8) String password,@NotBlank String role) {}
  public record LoginRequest(@Email String email,@Size(min=8) String password) {}
  public record RefreshRequest(@NotBlank String refreshToken) {}
  @PostMapping("/register") @Operation(summary="Register")
  public ApiResponse<?> register(@RequestBody RegisterRequest r){
    Map<String,Object> u = new LinkedHashMap<>();
    u.put("id", UUID.randomUUID().toString()); u.put("firstName", r.firstName()); u.put("lastName", r.lastName()); u.put("email", r.email()); u.put("role", r.role()); u.put("active", true);
    users.put(r.email(), u);
    return ApiResponse.ok(u, "Registered");
  }
  @PostMapping("/login") @Operation(summary="Login")
  public ApiResponse<?> login(@RequestBody LoginRequest r){
    Map<String,Object> u = users.getOrDefault(r.email(), Map.of("id", UUID.randomUUID().toString(), "email", r.email(), "role", "PATIENT"));
    return ApiResponse.ok(Map.of("accessToken", "access-token", "refreshToken", "refresh-token", "user", u), "Logged in");
  }
  @PostMapping("/refresh") @Operation(summary="Refresh")
  public ApiResponse<?> refresh(@RequestBody RefreshRequest req){
    return ApiResponse.ok(Map.of("accessToken","new-access-token","refreshToken","new-refresh-token","user",Map.of("role","PATIENT")), "Refreshed");
  }
  @PostMapping("/logout") @Operation(summary="Logout")
  public ApiResponse<?> logout(){ return ApiResponse.ok("ok", "Logged out"); }
  @GetMapping("/me") @Operation(summary="Me")
  public ApiResponse<?> me(@RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous") String userId){
    return ApiResponse.ok(Map.of("id", userId, "role", "PATIENT"), "Current user");
  }
}
""",
)

# Gateway config (routes + CORS)
gateway = ROOT / "gateway-service/src/main/java/com/pharmacy/gateway"
w(
    gateway / "config/GatewayRoutes.java",
    """
package com.pharmacy.gateway.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
@Configuration
public class GatewayRoutes {
  @Bean
  CorsFilter corsFilter(@Value("${REACT_APP_ORIGIN:http://localhost:3000}") String origin) {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.addAllowedOrigin(origin); cfg.addAllowedMethod("*"); cfg.addAllowedHeader("*"); cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return new CorsFilter(src);
  }
}
""",
)
w(
    ROOT / "gateway-service/src/main/resources/application.yml",
    """
server:
  port: 8080
spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      mvc:
        routes:
          - id: auth
            uri: ${AUTH_SERVICE_URL:http://auth-service:8081}
            predicates: [ Path=/api/auth/** ]
          - id: doctor
            uri: ${PRESCRIPTION_SERVICE_URL:http://prescription-service:8082}
            predicates: [ Path=/api/doctor/** ]
          - id: pharmacy
            uri: ${PHARMACY_SERVICE_URL:http://pharmacy-service:8083}
            predicates: [ Path=/api/pharmacy/** ]
          - id: patients
            uri: ${PATIENT_SERVICE_URL:http://patient-service:8084}
            predicates: [ Path=/api/patients/** ]
          - id: inventory
            uri: ${INVENTORY_SERVICE_URL:http://inventory-service:8085}
            predicates: [ Path=/api/inventory/** ]
          - id: billing
            uri: ${BILLING_SERVICE_URL:http://billing-service:8086}
            predicates: [ Path=/api/billing/** ]
          - id: analytics
            uri: ${ANALYTICS_SERVICE_URL:http://analytics-service:8087}
            predicates: [ Path=/api/analytics/** ]
""",
)

# Remaining service endpoints
remaining = {
    "patient": """
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
""",
    "inventory": """
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
""",
    "billing": """
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
""",
    "analytics": """
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
""",
}

for pkg, content in remaining.items():
    cls = pkg.capitalize() + "Controller.java"
    w(ROOT / f"{pkg}-service/src/main/java/com/pharmacy/{pkg}/controller/{cls}", content)

print("Added auth, gateway, and remaining controllers")
