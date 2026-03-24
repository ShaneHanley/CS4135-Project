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
