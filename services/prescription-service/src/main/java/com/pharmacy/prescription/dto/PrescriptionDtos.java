package com.pharmacy.prescription.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
public class PrescriptionDtos {
  public record CreatePrescriptionRequest(@NotBlank String patientId,@NotBlank String patientEmail,@NotBlank String patientName,@NotBlank String pharmacyId,@NotBlank String medicationName,@NotBlank String dosage,@NotBlank String instructions,@Min(1) int quantity,@Min(0) int refillsAllowed) {}
  public record PrescriptionView(String prescriptionId,String doctorId,String patientId,String patientEmail,String patientName,String pharmacyId,String medicationName,String dosage,String instructions,int quantity,String status,String rejectionReason,Instant createdAt,Instant updatedAt,int refillsAllowed,int refillsUsed) {}
  public record DoctorView(String id,String firstName,String lastName,String email,String licenseNumber,String phone,boolean active,Instant createdAt,Instant updatedAt) {}
}
