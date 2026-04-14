package com.pharmacy.prescription.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "pharmacies", schema = "prescription_svc")
public class Pharmacy {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
