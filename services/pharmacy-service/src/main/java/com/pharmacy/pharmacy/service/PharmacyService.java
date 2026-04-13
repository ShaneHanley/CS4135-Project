package com.pharmacy.pharmacy.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.pharmacy.entity.PharmacyPrescription;
import com.pharmacy.pharmacy.entity.PharmacyPrescriptionStatus;
import com.pharmacy.messaging.PgmqService;
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
    payload.put("channel", "email");
    payload.put("type", switch (saved.getStatus()) {
      case PROCESSING       -> "processing";
      case READY_FOR_PICKUP -> "collect";
      case REJECTED         -> "rejected";
      default               -> "received";
    });
    payload.put("recipient", saved.getPatientEmail());
    payload.put("subject", switch (saved.getStatus()) {
      case PROCESSING       -> "Your prescription is being prepared";
      case READY_FOR_PICKUP -> "Your prescription is ready for pickup";
      case REJECTED         -> "Your prescription could not be filled";
      default               -> "Prescription update";
    });
    payload.put("body", switch (saved.getStatus()) {
      case PROCESSING -> String.format(
          "Hi %s, your prescription for %s is now being prepared at Central Pharmacy.",
          saved.getPatientName(), saved.getMedicationName());
      case READY_FOR_PICKUP -> String.format(
          "Hi %s, your prescription for %s is ready for pickup at Central Pharmacy.",
          saved.getPatientName(), saved.getMedicationName());
      case REJECTED -> String.format(
          "Hi %s, your prescription for %s could not be filled. Reason: %s",
          saved.getPatientName(), saved.getMedicationName(), saved.getRejectionReason());
      default -> String.format(
          "Hi %s, your prescription status has been updated.",
          saved.getPatientName());
    });
    pgmqService.sendMessage("notifications", payload);
    return saved;
  }
  public Map<String,Long> stats(){
    Map<String,Long> counts = new HashMap<>();
    for (PharmacyPrescription p : repository.findAll()) { counts.merge(p.getStatus().name(), 1L, Long::sum); }
    return counts;
  }
}
