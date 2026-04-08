package com.ecommerce.product_service.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler global para tratamento de exceções
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata exceção quando recurso não é encontrado
     */
    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            RuntimeException ex,
            HttpServletRequest request) {
        logger.warn("Resource not found at path={}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Trata exceção de validação de argumentos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String validationMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        logger.warn("Validation error at path={}: {}", request.getRequestURI(), validationMessage);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Error", validationMessage, request.getRequestURI());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {
        logger.warn("Business error at path={}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Business Rule Violation", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        logger.warn("Constraint violation at path={}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Data Integrity Violation",
                "Dados inválidos ou violação de restrição de banco de dados", request.getRequestURI());
    }

    /**
     * Trata exceção genérica
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        logger.error("Unexpected error at path={}", request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Erro interno do servidor", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String error, String message, String path) {
        String traceId = resolveTraceId();
        ErrorResponse response = new ErrorResponse(LocalDateTime.now(), status.value(), error, message, path, traceId);
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
