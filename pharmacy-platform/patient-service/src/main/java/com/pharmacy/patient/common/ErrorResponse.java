package com.pharmacy.patient.common;
import java.time.Instant;
public record ErrorResponse(boolean success, ErrorBody error, Instant timestamp) {
  public static ErrorResponse of(String code, String message, Object details) { return new ErrorResponse(false, new ErrorBody(code, message, details), Instant.now()); }
  public record ErrorBody(String code, String message, Object details) {}
}
