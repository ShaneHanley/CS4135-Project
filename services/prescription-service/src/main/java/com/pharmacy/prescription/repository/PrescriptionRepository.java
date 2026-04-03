package com.pharmacy.prescription.repository;
import com.pharmacy.prescription.entity.Prescription;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
  List<Prescription> findByDoctorId(String doctorId);
  Optional<Prescription> findByIdAndDoctorId(UUID id, String doctorId);
}
