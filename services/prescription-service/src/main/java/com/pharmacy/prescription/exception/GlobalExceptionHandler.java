package com.pharmacy.prescription.exception;
import com.pharmacy.prescription.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(DoctorProfileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handle(DoctorProfileNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("NOT_FOUND", ex.getMessage(), null));
  }
  @ExceptionHandler(DoctorProfileAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handle(DoctorProfileAlreadyExistsException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("CONFLICT", ex.getMessage(), null));
  }
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handle(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", ex.getMessage(), null));
  }
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
    return ResponseEntity.internalServerError().body(ErrorResponse.of("INTERNAL_ERROR", ex.getMessage(), null));
  }
}
