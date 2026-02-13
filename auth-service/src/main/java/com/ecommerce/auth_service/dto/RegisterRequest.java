package com.ecommerce.auth_service.dto;

import com.ecommerce.auth_service.model.User.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para requisição de registro
 */
public class RegisterRequest {

    private String username;
    private String email;
    private String password;

    @JsonProperty("confirm_password")
    private String confirmPassword;

    // Constructors
    public RegisterRequest() {
    }

    public RegisterRequest(String username, String email, String password, String confirmPassword) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
