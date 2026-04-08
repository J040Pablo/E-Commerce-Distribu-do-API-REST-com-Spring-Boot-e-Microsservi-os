package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.ApiResponse;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.dto.LoginRequest;
import com.ecommerce.auth_service.dto.RefreshTokenRequest;
import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@Validated
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Auth register request received for username={}", request.getUsername());

        AuthResponse authResponse = authService.register(request);

        logger.info("Auth register completed for username={}", request.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuário registrado com sucesso", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Auth login request received for username={}", request.getUsername());

        AuthResponse authResponse = authService.login(request);

        logger.info("Auth login completed for username={}", request.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Login realizado com sucesso", authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("Auth refresh token request received");

        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());

        logger.info("Auth refresh token completed");
        return ResponseEntity.ok(ApiResponse.success("Token renovado com sucesso", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        // Comportamento atual:
        // - token válido e ativo    -> revoga persistindo no banco
        // - token antigo/revogado   -> rejeita com AuthenticationException
        // - token inexistente       -> rejeita com AuthenticationException
        // - token inválido/curto    -> rejeita com AuthenticationException
        logger.info("Auth logout request received");
        authService.revokeRefreshToken(request.getRefreshToken());
        logger.info("Auth logout completed");
        return ResponseEntity.ok(ApiResponse.success("Sessão encerrada com sucesso", null));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> validateToken(
            @RequestHeader("Authorization") @NotBlank String authorizationHeader) {
        logger.info("Auth validate token request received");

        String token = extractBearerToken(authorizationHeader);
        AuthResponse.UserInfo userInfo = authService.getTokenInfo(token);

        logger.info("Auth validate token completed for userId={}", userInfo.getId());
        return ResponseEntity.ok(ApiResponse.success("Token é válido", userInfo));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Serviço disponível", "Auth Service funcionando!"));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Authorization header deve usar o formato Bearer <token>");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new IllegalArgumentException("Token Bearer não pode estar vazio");
        }
        return token;
    }
}
