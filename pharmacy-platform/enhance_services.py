from pathlib import Path

ROOT = Path(r"c:\Users\josep\cs4135-copy\pharmacy-platform")


def w(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


services = {
    "prescription-service": ("prescription", "doctor"),
    "pharmacy-service": ("pharmacy", "pharmacy"),
    "patient-service": ("patient", "patients"),
    "inventory-service": ("inventory", "inventory"),
    "billing-service": ("billing", "billing"),
    "analytics-service": ("analytics", "analytics"),
}

for sname, (pkg, route) in services.items():
    b = f"com/pharmacy/{pkg}"
    w(
        ROOT / sname / f"src/main/java/{b}/common/ApiResponse.java",
        f"""
package com.pharmacy.{pkg}.common;
import java.time.Instant;
public record ApiResponse<T>(boolean success, T data, String message, Instant timestamp) {{
  public static <T> ApiResponse<T> ok(T data, String message) {{ return new ApiResponse<>(true, data, message, Instant.now()); }}
}}
""",
    )
    w(
        ROOT / sname / f"src/main/java/{b}/common/ErrorResponse.java",
        f"""
package com.pharmacy.{pkg}.common;
import java.time.Instant;
public record ErrorResponse(boolean success, ErrorBody error, Instant timestamp) {{
  public static ErrorResponse of(String code, String message, Object details) {{ return new ErrorResponse(false, new ErrorBody(code, message, details), Instant.now()); }}
  public record ErrorBody(String code, String message, Object details) {{}}
}}
""",
    )
    w(
        ROOT / sname / f"src/main/java/{b}/exception/GlobalExceptionHandler.java",
        f"""
package com.pharmacy.{pkg}.exception;
import com.pharmacy.{pkg}.common.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {{
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handle(IllegalArgumentException ex) {{
    return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", ex.getMessage(), null));
  }}
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAny(Exception ex) {{
    return ResponseEntity.internalServerError().body(ErrorResponse.of("INTERNAL_ERROR", ex.getMessage(), null));
  }}
}}
""",
    )
    w(
        ROOT / sname / f"src/main/java/{b}/config/SecurityConfig.java",
        f"""
package com.pharmacy.{pkg}.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {{
  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {{
    http.csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth.requestMatchers("/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html","/actuator/health").permitAll().anyRequest().authenticated())
      .oauth2ResourceServer(oauth -> oauth.jwt());
    return http.build();
  }}
}}
""",
    )
    w(
        ROOT / sname / f"src/main/java/{b}/messaging/PgmqService.java",
        f"""
package com.pharmacy.{pkg}.messaging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
@Service
public class PgmqService {{
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  public PgmqService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {{ this.jdbcTemplate = jdbcTemplate; this.objectMapper = objectMapper; }}
  public long sendMessage(String queue, Object payload) {{
    try {{
      String json = objectMapper.writeValueAsString(payload);
      Long id = jdbcTemplate.queryForObject("SELECT pgmq.send(?::text, ?::jsonb)", Long.class, queue, json);
      return id == null ? -1 : id;
    }} catch (JsonProcessingException e) {{
      throw new IllegalArgumentException("Invalid payload", e);
    }}
  }}
  public List<Map<String,Object>> readMessages(String queue, int vt, int limit) {{
    return jdbcTemplate.queryForList("SELECT * FROM pgmq.read(?::text, ?, ?)", queue, vt, limit);
  }}
  public boolean deleteMessage(String queue, long id) {{
    Boolean deleted = jdbcTemplate.queryForObject("SELECT pgmq.delete(?::text, ?)", Boolean.class, queue, id);
    return Boolean.TRUE.equals(deleted);
  }}
}}
""",
    )

# Prescription endpoints and publishing
w(
    ROOT / "prescription-service/src/main/java/com/pharmacy/prescription/controller/PrescriptionController.java",
    """
package com.pharmacy.prescription.controller;
import com.pharmacy.prescription.common.ApiResponse;
import com.pharmacy.prescription.messaging.PgmqService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
@RestController
@RequestMapping("/api/doctor")
public class PrescriptionController {
  private final PgmqService pgmqService;
  private final Map<String, Map<String, Object>> store = new LinkedHashMap<>();
  public PrescriptionController(PgmqService pgmqService) { this.pgmqService = pgmqService; }
  public record CreatePrescriptionRequest(@NotBlank String patientId,@NotBlank String patientEmail,@NotBlank String patientName,@NotBlank String pharmacyId,@NotBlank String medicationName,@NotBlank String dosage,@NotBlank String instructions,@Min(1) int quantity,@Min(0) int refillsAllowed) {}
  @GetMapping("/prescriptions") @Operation(summary="Get doctor prescriptions") @PreAuthorize("hasRole('DOCTOR')")
  public ApiResponse<?> list(@RequestHeader("X-User-Id") String doctorId){ return ApiResponse.ok(store.values().stream().filter(v -> doctorId.equals(v.get("doctorId"))).toList(), "Fetched"); }
  @PostMapping("/prescriptions") @Operation(summary="Create prescription") @PreAuthorize("hasRole('DOCTOR')")
  public ApiResponse<?> create(@RequestHeader("X-User-Id") String doctorId, @RequestBody CreatePrescriptionRequest r){
    String id = UUID.randomUUID().toString();
    Map<String,Object> p = new LinkedHashMap<>();
    p.put("prescriptionId", id); p.put("doctorId", doctorId); p.put("patientId", r.patientId()); p.put("patientEmail", r.patientEmail()); p.put("patientName", r.patientName()); p.put("pharmacyId", r.pharmacyId()); p.put("medicationName", r.medicationName()); p.put("dosage", r.dosage()); p.put("instructions", r.instructions()); p.put("quantity", r.quantity()); p.put("status", "NEW"); p.put("createdAt", Instant.now().toString()); p.put("refillsAllowed", r.refillsAllowed()); p.put("refillsUsed", 0);
    store.put(id, p);
    pgmqService.sendMessage("prescription_created", p);
    return ApiResponse.ok(p, "Created");
  }
  @GetMapping("/prescriptions/{id}/status") @Operation(summary="Get status") @PreAuthorize("hasRole('DOCTOR')")
  public ApiResponse<?> status(@PathVariable String id){ Map<String,Object> p = store.get(id); return ApiResponse.ok(p == null ? "NOT_FOUND" : p.get("status"), "Status"); }
  @GetMapping("/pharmacies") @Operation(summary="Get pharmacies") @PreAuthorize("hasRole('DOCTOR')")
  public ApiResponse<?> pharmacies(){ return ApiResponse.ok(List.of(Map.of("id", "default-pharmacy", "name", "Central Pharmacy")), "Fetched"); }
}
""",
)

w(
    ROOT / "pharmacy-service/src/main/java/com/pharmacy/pharmacy/controller/PharmacyController.java",
    """
package com.pharmacy.pharmacy.controller;
import com.pharmacy.pharmacy.common.ApiResponse;
import com.pharmacy.pharmacy.messaging.PgmqService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {
  private final PgmqService pgmqService;
  private final Map<String, Map<String,Object>> store = new LinkedHashMap<>();
  public PharmacyController(PgmqService pgmqService) { this.pgmqService = pgmqService; }
  @Scheduled(fixedDelay = 5000)
  public void pollPrescriptionCreated() {
    List<Map<String,Object>> rows = pgmqService.readMessages("prescription_created", 30, 20);
    for (Map<String,Object> row : rows) {
      Object msgObj = row.get("message");
      if (msgObj instanceof Map<?,?> msg) {
        Map<String,Object> payload = new LinkedHashMap<>();
        msg.forEach((k,v) -> payload.put(String.valueOf(k), v));
        store.put(String.valueOf(payload.get("prescriptionId")), payload);
      }
      Object idObj = row.get("msg_id");
      if (idObj != null) pgmqService.deleteMessage("prescription_created", Long.parseLong(String.valueOf(idObj)));
    }
  }
  @GetMapping("/prescriptions") @Operation(summary="Pharmacy prescriptions") @PreAuthorize("hasAnyRole('PHARMACIST','TECHNICIAN','MANAGER')")
  public ApiResponse<?> list(){ return ApiResponse.ok(store.values(), "Fetched"); }
  @GetMapping("/prescriptions/{id}") @Operation(summary="Get pharmacy prescription") @PreAuthorize("hasAnyRole('PHARMACIST','TECHNICIAN','MANAGER')")
  public ApiResponse<?> one(@PathVariable String id){ return ApiResponse.ok(store.get(id), "Fetched"); }
  public record StatusUpdateRequest(String status, String rejectionReason) {}
  @PutMapping("/prescriptions/{id}/status") @Operation(summary="Update prescription status") @PreAuthorize("hasAnyRole('PHARMACIST','MANAGER')")
  public ApiResponse<?> update(@PathVariable String id, @RequestBody StatusUpdateRequest r){
    Map<String,Object> p = Optional.ofNullable(store.get(id)).orElseThrow(() -> new IllegalArgumentException("Prescription not found"));
    p.put("status", r.status()); p.put("rejectionReason", r.rejectionReason());
    Map<String,Object> payload = new LinkedHashMap<>();
    payload.put("prescriptionId", p.get("prescriptionId"));
    payload.put("patientId", p.get("patientId"));
    payload.put("patientEmail", p.get("patientEmail"));
    payload.put("patientName", p.get("patientName"));
    payload.put("newStatus", r.status());
    payload.put("rejectionReason", r.rejectionReason());
    payload.put("pharmacyName", "Central Pharmacy");
    pgmqService.sendMessage("notification_prescription_status", payload);
    return ApiResponse.ok(p, "Updated");
  }
  @GetMapping("/dashboard/stats") @Operation(summary="Dashboard stats") @PreAuthorize("hasRole('MANAGER')")
  public ApiResponse<?> stats(){
    Map<String,Long> counts = new HashMap<>();
    for (Map<String,Object> p : store.values()) { counts.merge(String.valueOf(p.getOrDefault("status", "NEW")), 1L, Long::sum); }
    return ApiResponse.ok(counts, "Fetched");
  }
}
""",
)

