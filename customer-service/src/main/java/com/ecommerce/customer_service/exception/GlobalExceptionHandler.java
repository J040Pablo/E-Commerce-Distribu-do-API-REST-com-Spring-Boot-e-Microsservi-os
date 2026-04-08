package com.ecommerce.customer_service.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({CustomerNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleCustomerNotFoundException(RuntimeException ex, HttpServletRequest request) {
        logger.warn("Customer not found at path={}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCustomerAlreadyExistsException(CustomerAlreadyExistsException ex, HttpServletRequest request) {
        logger.warn("Customer conflict at path={}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String validationMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        logger.warn("Validation error at path={}: {}", request.getRequestURI(), validationMessage);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Error", validationMessage, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error at path={}", request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Erro interno do servidor", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String error, String message, String path) {
        ErrorResponse response = new ErrorResponse(LocalDateTime.now(), status.value(), error, message, path, resolveTraceId());
        return ResponseEntity.status(status).body(response);
    }

    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("trace_id");
        }
        return (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
    }
}
