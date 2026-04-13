package com.pharmacy.prescription.exception;

public class DoctorProfileNotFoundException extends RuntimeException {
    public DoctorProfileNotFoundException(String doctorId) {
        super("No profile found for doctor: " + doctorId);
    }
}
