package com.ecommerce.product_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties
public class DatabaseSecretValidationConfig {

    private final org.springframework.boot.autoconfigure.jdbc.DataSourceProperties dataSourceProperties;

    public DatabaseSecretValidationConfig(
            org.springframework.boot.autoconfigure.jdbc.DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @PostConstruct
    public void validateDatabaseSecrets() {
        String password = dataSourceProperties.getPassword();

        if (password == null || password.isEmpty()) {
            throw new IllegalStateException(
                "CRITICAL: DB_PASSWORD environment variable is not set. " +
                "Database credentials must be provided via environment variables. " +
                "Set DB_PASSWORD environment variable before deployment."
            );
        }

        if ("ecommerce".equals(password) || "root".equals(password) || "guest".equals(password)) {
            throw new IllegalStateException(
                "CRITICAL: DB_PASSWORD contains default/weak value: " + password + ". " +
                "Production deployments must use strong passwords. " +
                "Generate using: openssl rand -base64 16"
            );
        }

        String username = dataSourceProperties.getUsername();
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException(
                "CRITICAL: DB_USERNAME environment variable is not set."
            );
        }

        String url = dataSourceProperties.getUrl();
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException(
                "CRITICAL: DB_HOST and DB_PORT environment variables must be configured."
            );
        }
    }
}
