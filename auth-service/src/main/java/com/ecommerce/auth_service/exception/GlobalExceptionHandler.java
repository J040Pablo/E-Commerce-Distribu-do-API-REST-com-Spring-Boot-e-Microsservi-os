package com.ecommerce.auth_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Handler global para tratamento de exceções
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata exceção de autenticação falha
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        logger.warn("Authentication error at path={}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication Failed", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Trata exceção de usuário não encontrado
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {
        logger.warn("User not found at path={}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Trata exceção de usuário já existente
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {
        logger.warn("Conflict at path={}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request.getRequestURI());
    }

    /**
     * Trata exceção de validação de argumentos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String validationMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        logger.warn("Validation error at path={}: {}", request.getRequestURI(), validationMessage);
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation Error",
            validationMessage,
            request.getRequestURI()
        );
    }

    /**
         * Trata exceções de argumentos inválidos.
     */
        @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
        public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            Exception ex,
            HttpServletRequest request) {
        logger.warn("Bad request at path={}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI());
        }

        /**
         * Trata exceção genérica
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        logger.error("Unexpected error at path={}", request.getRequestURI(), ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "Erro interno do servidor",
            request.getRequestURI()
        );
        }

        private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            String path) {
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            status.value(),
            error,
            message,
            path
        );
        return ResponseEntity.status(status).body(response);
    }
}
