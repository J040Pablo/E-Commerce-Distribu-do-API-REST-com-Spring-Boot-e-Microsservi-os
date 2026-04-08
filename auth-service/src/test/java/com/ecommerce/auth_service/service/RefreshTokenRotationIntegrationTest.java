package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.exception.AuthenticationException;
import com.ecommerce.auth_service.model.RefreshToken;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import com.ecommerce.auth_service.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth_service_test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false",
    "eureka.client.enabled=false",
    "jwt.secret=test-secret-key-with-at-least-64-characters-for-hs512-and-more-entropy-1234567890",
    "jwt.expiration=600000",
    "jwt.refresh.expiration=3600000",
    "LOG_LEVEL_ROOT=WARN",
    "LOG_LEVEL_APP=INFO"
})
@Transactional
@DisplayName("Refresh Token Rotation Integration Tests")
class RefreshTokenRotationIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("Should issue refresh token on register")
    void shouldIssueRefreshTokenOnRegister() {
        RegisterRequest registerRequest = buildRequest();

        AuthResponse authResponse = authService.register(registerRequest);

        assertThat(authResponse.getRefreshToken()).isNotBlank();
        String jwtId = extractJwtId(authResponse.getRefreshToken());
        Optional<RefreshToken> storedToken = refreshTokenRepository.findByJwtId(jwtId);

        assertThat(storedToken).isPresent();
        assertThat(storedToken.get().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("Should rotate refresh token and reject old token reuse")
    void shouldRotateAndRejectOldToken() {
        RegisterRequest registerRequest = buildRequest();
        AuthResponse initial = authService.register(registerRequest);

        String oldRefresh = initial.getRefreshToken();

        AuthResponse rotated = authService.refreshToken(oldRefresh);

        assertThat(rotated.getRefreshToken()).isNotBlank();
        assertThat(rotated.getRefreshToken()).isNotEqualTo(oldRefresh);

        assertThatThrownBy(() -> authService.refreshToken(oldRefresh))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("revogado");

        String oldJwtId = extractJwtId(oldRefresh);
        RefreshToken oldStored = refreshTokenRepository.findByJwtId(oldJwtId).orElseThrow();

        assertThat(oldStored.isRevoked()).isTrue();
        assertThat(oldStored.getReplacedByJwtId()).isNotBlank();
    }

    private RegisterRequest buildRequest() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return new RegisterRequest(
                "user_" + suffix,
                "user_" + suffix + "@example.com",
                "password123",
                "password123"
        );
    }

    private String extractJwtId(String refreshToken) {
        return jwtProvider.getTokenId(refreshToken);
    }
}
