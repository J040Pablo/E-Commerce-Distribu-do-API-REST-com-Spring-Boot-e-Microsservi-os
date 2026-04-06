package com.ecommerce.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para requisição de renovação de token.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token é obrigatório")
    @JsonProperty("refresh_token")
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
