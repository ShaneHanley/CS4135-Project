package com.pharmacy.patient.exception;
import com.pharmacy.patient.common.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handle(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", ex.getMessage(), null));
  }
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
    return ResponseEntity.internalServerError().body(ErrorResponse.of("INTERNAL_ERROR", ex.getMessage(), null));
  }
}
