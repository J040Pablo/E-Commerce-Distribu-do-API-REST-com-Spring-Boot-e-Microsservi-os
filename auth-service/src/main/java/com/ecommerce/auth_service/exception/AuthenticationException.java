package com.ecommerce.auth_service.exception;

/**
 * Exceção customizada para autenticação falha
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
