package com.pharmacy.auth.exception;

import com.pharmacy.auth.common.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.of("VALIDATION_ERROR", "Validation failed", ex.getBindingResult().toString())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.of("BAD_REQUEST", ex.getMessage(), null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generic(Exception ex) {
        return ResponseEntity.internalServerError().body(
                ErrorResponse.of("INTERNAL_ERROR", ex.getMessage(), null)
        );
    }
}
