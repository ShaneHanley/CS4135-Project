package com.pharmacy.prescription.service;
import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.entity.Prescription;
import com.pharmacy.prescription.entity.PrescriptionStatus;
import com.pharmacy.messaging.PgmqService;
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
