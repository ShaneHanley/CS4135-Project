package com.pharmacy.prescription.exception;

public class DoctorProfileAlreadyExistsException extends RuntimeException {
    public DoctorProfileAlreadyExistsException(String doctorId) {
        super("Profile already exists for doctor: " + doctorId);
    }
}
