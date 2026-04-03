package com.pharmacy.pharmacy.repository;
import com.pharmacy.pharmacy.entity.PharmacyPrescription;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PharmacyPrescriptionRepository extends JpaRepository<PharmacyPrescription, UUID> {
  Optional<PharmacyPrescription> findByPrescriptionId(UUID prescriptionId);
}
