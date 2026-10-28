package com.dki.meeting.foundation.http;

import com.dki.meeting.foundation.AppException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(AppException.class)
  ResponseEntity<?> application(AppException ex) {
    log.warn("Request rejected. code={} status={}", ex.code(), ex.status());
    return response(ex.status(), ex.code(), ex.getMessage(), ex.details());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
    var details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                e ->
                    Map.of(
                        "target",
                        e.getField(),
                        "message",
                        e.getDefaultMessage() == null ? "Invalid value" : e.getDefaultMessage()))
            .toList();
    return response(400, "Validation.InvalidRequest", "The request is not valid", details);
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> malformed(Exception ex) {
    return response(400, "Validation.InvalidRequest", "The request is not valid", List.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<?> denied(AccessDeniedException ex) {
    return response(403, "Authorization.Forbidden", "You cannot perform this action", null);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception ex) {
    if (ex instanceof ErrorResponse error && error.getStatusCode().is4xxClientError())
      return response(
          error.getStatusCode().value(),
          "Request.Invalid",
          "The requested resource or method is not available",
          null);
    log.error("Unexpected request failure.", ex);
    return response(500, "Internal.Unexpected", "An unexpected error occurred", null);
  }

  private ResponseEntity<?> response(int status, String code, String message, Object details) {
    return ResponseEntity.status(status).body(ApiResponse.failure(code, message, details));
  }
}
