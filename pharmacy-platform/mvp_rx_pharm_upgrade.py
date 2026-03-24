from pathlib import Path

ROOT = Path(r"c:\Users\josep\cs4135-copy\pharmacy-platform")


def w(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


# Prescription service
ps = ROOT / "prescription-service/src/main/java/com/pharmacy/prescription"
w(
    ps / "entity/PrescriptionStatus.java",
    """
package com.pharmacy.prescription.entity;
public enum PrescriptionStatus { NEW, PROCESSING, READY_FOR_PICKUP, REJECTED }
""",
)
w(
    ps / "entity/Prescription.java",
    """
package com.pharmacy.prescription.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "prescriptions", schema = "prescription_svc")
public class Prescription {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false) private String doctorId;
  @Column(nullable = false) private String patientId;
  @Column(nullable = false) private String patientEmail;
  @Column(nullable = false) private String patientName;
  @Column(nullable = false) private String pharmacyId;
  @Column(nullable = false) private String medicationName;
  @Column(nullable = false) private String dosage;
  @Column(nullable = false) private String instructions;
  @Column(nullable = false) private int quantity;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private PrescriptionStatus status = PrescriptionStatus.NEW;
  private String rejectionReason;
  @Column(nullable = false) private int refillsAllowed;
  @Column(nullable = false) private int refillsUsed = 0;
  @Column(nullable = false) private Instant createdAt;
  @Column(nullable = false) private Instant updatedAt;
  @PrePersist public void onCreate(){ Instant now = Instant.now(); createdAt = now; updatedAt = now; if (status == null) status = PrescriptionStatus.NEW; }
  @PreUpdate public void onUpdate(){ updatedAt = Instant.now(); }
  public UUID getId(){ return id; } public String getDoctorId(){ return doctorId; } public void setDoctorId(String v){ doctorId=v; }
  public String getPatientId(){ return patientId; } public void setPatientId(String v){ patientId=v; }
  public String getPatientEmail(){ return patientEmail; } public void setPatientEmail(String v){ patientEmail=v; }
  public String getPatientName(){ return patientName; } public void setPatientName(String v){ patientName=v; }
  public String getPharmacyId(){ return pharmacyId; } public void setPharmacyId(String v){ pharmacyId=v; }
  public String getMedicationName(){ return medicationName; } public void setMedicationName(String v){ medicationName=v; }
  public String getDosage(){ return dosage; } public void setDosage(String v){ dosage=v; }
  public String getInstructions(){ return instructions; } public void setInstructions(String v){ instructions=v; }
  public int getQuantity(){ return quantity; } public void setQuantity(int v){ quantity=v; }
  public PrescriptionStatus getStatus(){ return status; } public void setStatus(PrescriptionStatus v){ status=v; }
  public String getRejectionReason(){ return rejectionReason; } public void setRejectionReason(String v){ rejectionReason=v; }
  public int getRefillsAllowed(){ return refillsAllowed; } public void setRefillsAllowed(int v){ refillsAllowed=v; }
  public int getRefillsUsed(){ return refillsUsed; } public void setRefillsUsed(int v){ refillsUsed=v; }
  public Instant getCreatedAt(){ return createdAt; } public Instant getUpdatedAt(){ return updatedAt; }
}
""",
)
w(
    ps / "repository/PrescriptionRepository.java",
    """
package com.pharmacy.prescription.repository;
import com.pharmacy.prescription.entity.Prescription;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
  List<Prescription> findByDoctorId(String doctorId);
  Optional<Prescription> findByIdAndDoctorId(UUID id, String doctorId);
}
""",
)
w(
    ps / "dto/PrescriptionDtos.java",
    """
package com.pharmacy.prescription.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
public class PrescriptionDtos {
  public record CreatePrescriptionRequest(@NotBlank String patientId,@NotBlank String patientEmail,@NotBlank String patientName,@NotBlank String pharmacyId,@NotBlank String medicationName,@NotBlank String dosage,@NotBlank String instructions,@Min(1) int quantity,@Min(0) int refillsAllowed) {}
  public record PrescriptionView(String prescriptionId,String doctorId,String patientId,String patientEmail,String patientName,String pharmacyId,String medicationName,String dosage,String instructions,int quantity,String status,String rejectionReason,Instant createdAt,Instant updatedAt,int refillsAllowed,int refillsUsed) {}
}
""",
)
w(
    ps / "service/PrescriptionService.java",
    """
package com.pharmacy.prescription.service;
import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.entity.Prescription;
import com.pharmacy.prescription.entity.PrescriptionStatus;
import com.pharmacy.prescription.messaging.PgmqService;
import com.pharmacy.prescription.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class PrescriptionService {
  private final PrescriptionRepository repository;
  private final PgmqService pgmqService;
  public PrescriptionService(PrescriptionRepository repository, PgmqService pgmqService){ this.repository = repository; this.pgmqService = pgmqService; }
  public List<PrescriptionDtos.PrescriptionView> byDoctor(String doctorId){ return repository.findByDoctorId(doctorId).stream().map(this::toView).toList(); }
  public PrescriptionDtos.PrescriptionView create(String doctorId, PrescriptionDtos.CreatePrescriptionRequest r){
    Prescription p = new Prescription();
    p.setDoctorId(doctorId); p.setPatientId(r.patientId()); p.setPatientEmail(r.patientEmail()); p.setPatientName(r.patientName());
    p.setPharmacyId(r.pharmacyId()); p.setMedicationName(r.medicationName()); p.setDosage(r.dosage()); p.setInstructions(r.instructions());
    p.setQuantity(r.quantity()); p.setStatus(PrescriptionStatus.NEW); p.setRefillsAllowed(r.refillsAllowed());
    Prescription saved = repository.save(p);
    pgmqService.sendMessage("prescription_created", Map.of(
      "prescriptionId", saved.getId().toString(),
      "doctorId", saved.getDoctorId(),
      "patientId", saved.getPatientId(),
      "patientEmail", saved.getPatientEmail(),
      "patientName", saved.getPatientName(),
      "pharmacyId", saved.getPharmacyId(),
      "medicationName", saved.getMedicationName(),
      "dosage", saved.getDosage(),
      "quantity", saved.getQuantity(),
      "createdAt", saved.getCreatedAt().toString()
    ));
    return toView(saved);
  }
  public String status(String doctorId, String id){
    var p = repository.findByIdAndDoctorId(UUID.fromString(id), doctorId).orElseThrow(() -> new IllegalArgumentException("Prescription not found"));
    return p.getStatus().name();
  }
  private PrescriptionDtos.PrescriptionView toView(Prescription p){
    return new PrescriptionDtos.PrescriptionView(p.getId().toString(), p.getDoctorId(), p.getPatientId(), p.getPatientEmail(), p.getPatientName(), p.getPharmacyId(), p.getMedicationName(), p.getDosage(), p.getInstructions(), p.getQuantity(), p.getStatus().name(), p.getRejectionReason(), p.getCreatedAt(), p.getUpdatedAt(), p.getRefillsAllowed(), p.getRefillsUsed());
  }
}
""",
)
w(
    ps / "controller/PrescriptionController.java",
    """
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
""",
)
w(
    ROOT / "prescription-service/src/main/resources/db/migration/V1__init_schema.sql",
    """
CREATE SCHEMA IF NOT EXISTS prescription_svc;
CREATE TABLE IF NOT EXISTS prescription_svc.prescriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  doctor_id TEXT NOT NULL,
  patient_id TEXT NOT NULL,
  patient_email TEXT NOT NULL,
  patient_name TEXT NOT NULL,
  pharmacy_id TEXT NOT NULL,
  medication_name TEXT NOT NULL,
  dosage TEXT NOT NULL,
  instructions TEXT NOT NULL,
  quantity INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'NEW',
  rejection_reason TEXT,
  refills_allowed INTEGER NOT NULL DEFAULT 0,
  refills_used INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
""",
)

# Pharmacy service
ph = ROOT / "pharmacy-service/src/main/java/com/pharmacy/pharmacy"
w(
    ph / "entity/PharmacyPrescriptionStatus.java",
    """
package com.pharmacy.pharmacy.entity;
public enum PharmacyPrescriptionStatus { NEW, PROCESSING, READY_FOR_PICKUP, REJECTED }
""",
)
w(
    ph / "entity/PharmacyPrescription.java",
    """
package com.pharmacy.pharmacy.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "pharmacy_prescriptions", schema = "pharmacy_svc")
public class PharmacyPrescription {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false, unique = true) private UUID prescriptionId;
  @Column(nullable = false) private String doctorId;
  @Column(nullable = false) private String patientId;
  @Column(nullable = false) private String patientEmail;
  @Column(nullable = false) private String patientName;
  @Column(nullable = false) private String pharmacyId;
  @Column(nullable = false) private String medicationName;
  @Column(nullable = false) private String dosage;
  @Column(nullable = false) private int quantity;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private PharmacyPrescriptionStatus status = PharmacyPrescriptionStatus.NEW;
  private String rejectionReason;
  @Column(nullable = false) private Instant createdAt;
  @Column(nullable = false) private Instant updatedAt;
  @PrePersist public void onCreate(){ Instant now = Instant.now(); createdAt=now; updatedAt=now; }
  @PreUpdate public void onUpdate(){ updatedAt = Instant.now(); }
  public UUID getId(){ return id; } public UUID getPrescriptionId(){ return prescriptionId; } public void setPrescriptionId(UUID v){ prescriptionId=v; }
  public String getDoctorId(){ return doctorId; } public void setDoctorId(String v){ doctorId=v; }
  public String getPatientId(){ return patientId; } public void setPatientId(String v){ patientId=v; }
  public String getPatientEmail(){ return patientEmail; } public void setPatientEmail(String v){ patientEmail=v; }
  public String getPatientName(){ return patientName; } public void setPatientName(String v){ patientName=v; }
  public String getPharmacyId(){ return pharmacyId; } public void setPharmacyId(String v){ pharmacyId=v; }
  public String getMedicationName(){ return medicationName; } public void setMedicationName(String v){ medicationName=v; }
  public String getDosage(){ return dosage; } public void setDosage(String v){ dosage=v; }
  public int getQuantity(){ return quantity; } public void setQuantity(int v){ quantity=v; }
  public PharmacyPrescriptionStatus getStatus(){ return status; } public void setStatus(PharmacyPrescriptionStatus v){ status=v; }
  public String getRejectionReason(){ return rejectionReason; } public void setRejectionReason(String v){ rejectionReason=v; }
  public Instant getCreatedAt(){ return createdAt; } public Instant getUpdatedAt(){ return updatedAt; }
}
""",
)
w(
    ph / "repository/PharmacyPrescriptionRepository.java",
    """
package com.pharmacy.pharmacy.repository;
import com.pharmacy.pharmacy.entity.PharmacyPrescription;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PharmacyPrescriptionRepository extends JpaRepository<PharmacyPrescription, UUID> {
  Optional<PharmacyPrescription> findByPrescriptionId(UUID prescriptionId);
}
""",
)
w(
    ph / "service/PharmacyService.java",
    """
package com.pharmacy.pharmacy.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy.entity.PharmacyPrescription;
import com.pharmacy.pharmacy.entity.PharmacyPrescriptionStatus;
import com.pharmacy.pharmacy.messaging.PgmqService;
import com.pharmacy.pharmacy.repository.PharmacyPrescriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class PharmacyService {
  private final PharmacyPrescriptionRepository repository;
  private final PgmqService pgmqService;
  private final ObjectMapper objectMapper;
  public PharmacyService(PharmacyPrescriptionRepository repository, PgmqService pgmqService, ObjectMapper objectMapper){
    this.repository=repository; this.pgmqService=pgmqService; this.objectMapper=objectMapper;
  }
  @Scheduled(fixedDelay = 5000)
  public void pollPrescriptionCreated() {
    List<Map<String,Object>> rows = pgmqService.readMessages("prescription_created", 30, 20);
    for (Map<String,Object> row : rows) {
      try {
        Object msgObj = row.get("message");
        Map<String,Object> m = (msgObj instanceof String s)
          ? objectMapper.readValue(s, new TypeReference<Map<String,Object>>() {})
          : objectMapper.convertValue(msgObj, new TypeReference<Map<String,Object>>() {});
        UUID prescriptionId = UUID.fromString(String.valueOf(m.get("prescriptionId")));
        if (repository.findByPrescriptionId(prescriptionId).isEmpty()) {
          PharmacyPrescription p = new PharmacyPrescription();
          p.setPrescriptionId(prescriptionId);
          p.setDoctorId(String.valueOf(m.get("doctorId")));
          p.setPatientId(String.valueOf(m.get("patientId")));
          p.setPatientEmail(String.valueOf(m.get("patientEmail")));
          p.setPatientName(String.valueOf(m.get("patientName")));
          p.setPharmacyId(String.valueOf(m.get("pharmacyId")));
          p.setMedicationName(String.valueOf(m.get("medicationName")));
          p.setDosage(String.valueOf(m.get("dosage")));
          p.setQuantity(Integer.parseInt(String.valueOf(m.get("quantity"))));
          p.setStatus(PharmacyPrescriptionStatus.NEW);
          repository.save(p);
        }
      } catch (Exception ignored) {}
      Object idObj = row.get("msg_id");
      if (idObj != null) pgmqService.deleteMessage("prescription_created", Long.parseLong(String.valueOf(idObj)));
    }
  }
  public List<PharmacyPrescription> list(){ return repository.findAll(); }
  public PharmacyPrescription one(String id){
    return repository.findByPrescriptionId(UUID.fromString(id)).orElseThrow(() -> new IllegalArgumentException("Prescription not found"));
  }
  public PharmacyPrescription update(String id, String status, String rejectionReason){
    PharmacyPrescription p = one(id);
    PharmacyPrescriptionStatus next = PharmacyPrescriptionStatus.valueOf(status);
    if (next == PharmacyPrescriptionStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
      throw new IllegalArgumentException("rejectionReason is required when status is REJECTED");
    }
    p.setStatus(next); p.setRejectionReason(rejectionReason);
    PharmacyPrescription saved = repository.save(p);
    Map<String,Object> payload = new LinkedHashMap<>();
    payload.put("prescriptionId", saved.getPrescriptionId().toString());
    payload.put("patientId", saved.getPatientId());
    payload.put("patientEmail", saved.getPatientEmail());
    payload.put("patientName", saved.getPatientName());
    payload.put("newStatus", saved.getStatus().name());
    payload.put("rejectionReason", saved.getRejectionReason());
    payload.put("pharmacyName", "Central Pharmacy");
    pgmqService.sendMessage("notification_prescription_status", payload);
    return saved;
  }
  public Map<String,Long> stats(){
    Map<String,Long> counts = new HashMap<>();
    for (PharmacyPrescription p : repository.findAll()) { counts.merge(p.getStatus().name(), 1L, Long::sum); }
    return counts;
  }
}
""",
)
w(
    ph / "controller/PharmacyController.java",
    """
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
""",
)
w(
    ROOT / "pharmacy-service/src/main/resources/db/migration/V1__init_schema.sql",
    """
CREATE SCHEMA IF NOT EXISTS pharmacy_svc;
CREATE TABLE IF NOT EXISTS pharmacy_svc.pharmacy_prescriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  prescription_id UUID NOT NULL UNIQUE,
  doctor_id TEXT NOT NULL,
  patient_id TEXT NOT NULL,
  patient_email TEXT NOT NULL,
  patient_name TEXT NOT NULL,
  pharmacy_id TEXT NOT NULL,
  medication_name TEXT NOT NULL,
  dosage TEXT NOT NULL,
  quantity INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'NEW',
  rejection_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
""",
)

print("Prescription and pharmacy upgraded to persistence-backed MVP.")
