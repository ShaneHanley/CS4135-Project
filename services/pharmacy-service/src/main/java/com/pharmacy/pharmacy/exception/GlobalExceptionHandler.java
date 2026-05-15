package com.pharmacy.pharmacy.exception;
import com.pharmacy.pharmacy.common.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handle(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", ex.getMessage(), null));
  }
  @ExceptionHandler(IllegalStateTransitionException.class)
  public ResponseEntity<ErrorResponse> handle(IllegalStateTransitionException ex) {
    return ResponseEntity.unprocessableEntity().body(ErrorResponse.of("INVALID_TRANSITION", ex.getMessage(), null));
  }
  @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLock(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
    return ResponseEntity.status(409).body(ErrorResponse.of("CONCURRENT_UPDATE", "This prescription was updated by another user. Please refresh and try again.", null));
  }
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAny(Exception ex) throws Exception {
    if (ex instanceof org.springframework.security.access.AccessDeniedException) throw ex;
    String raw = ex.getMessage();
    String msg = raw != null ? raw : ex.getClass().getSimpleName();
    return ResponseEntity.internalServerError().body(ErrorResponse.of("INTERNAL_ERROR", msg, null));
  }
}
