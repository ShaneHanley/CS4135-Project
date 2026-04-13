package com.pharmacy.prescription.service;

import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.entity.Doctor;
import com.pharmacy.prescription.exception.DoctorProfileAlreadyExistsException;
import com.pharmacy.prescription.exception.DoctorProfileNotFoundException;
import com.pharmacy.prescription.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DoctorService {

    private final DoctorRepository repository;

    public DoctorService(DoctorRepository repository) {
        this.repository = repository;
    }

    public PrescriptionDtos.DoctorView createProfile(String doctorId, PrescriptionDtos.CreateDoctorProfileRequest r) {
        UUID id = UUID.fromString(doctorId);
        if (repository.existsById(id)) {
            throw new DoctorProfileAlreadyExistsException(doctorId);
        }
        if (repository.existsByLicenseNumber(r.licenseNumber())) {
            throw new IllegalArgumentException("License number already in use: " + r.licenseNumber());
        }
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setFirstName(r.firstName());
        doctor.setLastName(r.lastName());
        doctor.setEmail(r.email());
        doctor.setLicenseNumber(r.licenseNumber());
        doctor.setPhone(r.phone());
        return toView(repository.save(doctor));
    }

    public PrescriptionDtos.DoctorView getProfile(String doctorId) {
        return toView(findOrThrow(doctorId));
    }

    public PrescriptionDtos.DoctorView updateProfile(String doctorId, PrescriptionDtos.UpdateDoctorProfileRequest r) {
        Doctor doctor = findOrThrow(doctorId);
        doctor.setFirstName(r.firstName());
        doctor.setLastName(r.lastName());
        doctor.setPhone(r.phone());
        return toView(repository.save(doctor));
    }

    private Doctor findOrThrow(String doctorId) {
        return repository.findById(UUID.fromString(doctorId))
                .orElseThrow(() -> new DoctorProfileNotFoundException(doctorId));
    }

    private PrescriptionDtos.DoctorView toView(Doctor d) {
        return new PrescriptionDtos.DoctorView(
                d.getId().toString(),
                d.getFirstName(),
                d.getLastName(),
                d.getEmail(),
                d.getLicenseNumber(),
                d.getPhone(),
                d.isActive(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
