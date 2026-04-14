package com.pharmacy.prescription.repository;

import com.pharmacy.prescription.entity.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PharmacyRepository extends JpaRepository<Pharmacy, UUID> {
    List<Pharmacy> findByActiveTrue();
}
