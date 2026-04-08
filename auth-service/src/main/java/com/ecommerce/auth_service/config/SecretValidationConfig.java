package com.ecommerce.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "jwt")
public class SecretValidationConfig {

    private String secret;
    private Long expiration;

    @PostConstruct
    public void validateSecrets() {
        if (secret == null || secret.isEmpty() || secret.startsWith("ecommerce_secret")) {
            throw new IllegalStateException(
                "CRITICAL: JWT_SECRET environment variable is not set or contains default value. " +
                "Application cannot start without a secure JWT secret. " +
                "Set JWT_SECRET environment variable before deployment."
            );
        }

        if (secret.length() < 32) {
            throw new IllegalStateException(
                "CRITICAL: JWT_SECRET must be at least 32 characters long for HS512 algorithm. " +
                "Current length: " + secret.length() + " characters. " +
                "Generate using: openssl rand -base64 32"
            );
        }

        if (expiration == null || expiration <= 0) {
            throw new IllegalStateException(
                "CRITICAL: JWT_EXPIRATION environment variable is not set or invalid."
            );
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }
}
