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
