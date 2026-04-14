package com.pharmacy.prescription.service;

import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.repository.PharmacyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PharmacyService {

    private final PharmacyRepository repository;

    public PharmacyService(PharmacyRepository repository) {
        this.repository = repository;
    }

    public List<PrescriptionDtos.PharmacyView> listActive() {
        return repository.findByActiveTrue().stream()
                .map(p -> new PrescriptionDtos.PharmacyView(
                        p.getId().toString(),
                        p.getName(),
                        p.getAddress(),
                        p.getPhone()))
                .toList();
    }
}
