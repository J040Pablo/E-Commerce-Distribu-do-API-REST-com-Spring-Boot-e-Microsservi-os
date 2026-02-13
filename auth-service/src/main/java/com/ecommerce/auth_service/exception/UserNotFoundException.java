package com.ecommerce.auth_service.exception;

/**
 * Exceção customizada para usuário não encontrado
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
