package com.pharmacy.prescription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.entity.Doctor;
import com.pharmacy.prescription.exception.DoctorProfileAlreadyExistsException;
import com.pharmacy.prescription.exception.DoctorProfileNotFoundException;
import com.pharmacy.prescription.repository.DoctorRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    DoctorRepository repository;

    @InjectMocks
    DoctorService service;

    @Test
    void createProfile_success_savesAndReturnsView() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);
        when(repository.existsByLicenseNumber("LIC-001")).thenReturn(false);
        when(repository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionDtos.CreateDoctorProfileRequest req =
                new PrescriptionDtos.CreateDoctorProfileRequest("Alice", "Smith", "alice@demo.com", "LIC-001", "061-999");

        PrescriptionDtos.DoctorView view = service.createProfile(id.toString(), req);

        assertThat(view.firstName()).isEqualTo("Alice");
        assertThat(view.licenseNumber()).isEqualTo("LIC-001");
        assertThat(view.email()).isEqualTo("alice@demo.com");
        verify(repository).save(any(Doctor.class));
    }

    @Test
    void createProfile_alreadyExists_throwsConflict() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        PrescriptionDtos.CreateDoctorProfileRequest req =
                new PrescriptionDtos.CreateDoctorProfileRequest("Alice", "Smith", "alice@demo.com", "LIC-001", null);

        assertThatThrownBy(() -> service.createProfile(id.toString(), req))
                .isInstanceOf(DoctorProfileAlreadyExistsException.class);
    }

    @Test
    void createProfile_duplicateLicense_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);
        when(repository.existsByLicenseNumber("LIC-001")).thenReturn(true);

        PrescriptionDtos.CreateDoctorProfileRequest req =
                new PrescriptionDtos.CreateDoctorProfileRequest("Alice", "Smith", "alice@demo.com", "LIC-001", null);

        assertThatThrownBy(() -> service.createProfile(id.toString(), req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LIC-001");
    }

    @Test
    void getProfile_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(id.toString()))
                .isInstanceOf(DoctorProfileNotFoundException.class);
    }

    @Test
    void updateProfile_updatesNameAndPhone() {
        UUID id = UUID.randomUUID();
        Doctor existing = doctorWith(id, "Alice", "Smith", "061-999");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionDtos.UpdateDoctorProfileRequest req =
                new PrescriptionDtos.UpdateDoctorProfileRequest("Bob", "Jones", "061-000");

        PrescriptionDtos.DoctorView view = service.updateProfile(id.toString(), req);

        assertThat(view.firstName()).isEqualTo("Bob");
        assertThat(view.lastName()).isEqualTo("Jones");
        assertThat(view.phone()).isEqualTo("061-000");
    }

    private Doctor doctorWith(UUID id, String firstName, String lastName, String phone) {
        Doctor d = new Doctor();
        d.setId(id);
        d.setFirstName(firstName);
        d.setLastName(lastName);
        d.setEmail("doctor@demo.com");
        d.setLicenseNumber("LIC-001");
        d.setPhone(phone);
        return d;
    }
}
