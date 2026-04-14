package com.pharmacy.prescription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pharmacy.prescription.dto.PrescriptionDtos;
import com.pharmacy.prescription.entity.Pharmacy;
import com.pharmacy.prescription.repository.PharmacyRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock
    PharmacyRepository repository;

    @InjectMocks
    PharmacyService service;

    @Test
    void listActive_returnsMappedViews() {
        Pharmacy p1 = pharmacyWith("Central Pharmacy", "1 Main St");
        Pharmacy p2 = pharmacyWith("Northside Pharmacy", "42 North Rd");
        when(repository.findByActiveTrue()).thenReturn(List.of(p1, p2));

        List<PrescriptionDtos.PharmacyView> views = service.listActive();

        assertThat(views).hasSize(2);
        assertThat(views.get(0).name()).isEqualTo("Central Pharmacy");
        assertThat(views.get(1).name()).isEqualTo("Northside Pharmacy");
    }

    @Test
    void listActive_noActivePharmacies_returnsEmptyList() {
        when(repository.findByActiveTrue()).thenReturn(List.of());

        assertThat(service.listActive()).isEmpty();
    }

    private Pharmacy pharmacyWith(String name, String address) {
        Pharmacy p = new Pharmacy();
        p.setName(name);
        p.setAddress(address);
        p.setPhone("061-100001");
        try {
            var f = Pharmacy.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, UUID.randomUUID());
        } catch (Exception ignored) {}
        return p;
    }
}
