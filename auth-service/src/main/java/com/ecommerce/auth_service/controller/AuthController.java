package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.dto.LoginRequest;
import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        logger.info("Requisição de registro: {}", request.getUsername());

        try {
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return buildErrorResponse("Username é obrigatório", HttpStatus.BAD_REQUEST);
            }
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return buildErrorResponse("Email é obrigatório", HttpStatus.BAD_REQUEST);
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return buildErrorResponse("Senha é obrigatória", HttpStatus.BAD_REQUEST);
            }
            if (request.getPassword().length() < 6) {
                return buildErrorResponse("Senha deve ter no mínimo 6 caracteres", HttpStatus.BAD_REQUEST);
            }

            AuthResponse authResponse = authService.register(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuário registrado com sucesso");
            response.put("data", authResponse);

            logger.info("Usuário registrado com sucesso: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Erro ao registrar usuário: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        logger.info("Requisição de login: {}", request.getUsername());

        try {
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return buildErrorResponse("Username é obrigatório", HttpStatus.BAD_REQUEST);
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return buildErrorResponse("Senha é obrigatória", HttpStatus.BAD_REQUEST);
            }

            AuthResponse authResponse = authService.login(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login realizado com sucesso");
            response.put("data", authResponse);

            logger.info("Login realizado com sucesso: {}", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao fazer login: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        logger.info("Requisição de refresh token");

        try {
            String refreshToken = request.get("refresh_token");

            if (refreshToken == null || refreshToken.isBlank()) {
                return buildErrorResponse("Refresh token é obrigatório", HttpStatus.BAD_REQUEST);
            }

            AuthResponse authResponse = authService.refreshToken(refreshToken);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token renovado com sucesso");
            response.put("data", authResponse);

            logger.info("Token renovado com sucesso");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro ao renovar token: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String token) {
        logger.info("Validando token");

        try {
            String cleanToken = token.replace("Bearer ", "");

            if (authService.validateToken(cleanToken)) {
                AuthResponse.UserInfo userInfo = authService.getTokenInfo(cleanToken);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Token é válido");
                response.put("user", userInfo);

                logger.info("Token validado com sucesso");
                return ResponseEntity.ok(response);
            } else {
                return buildErrorResponse("Token inválido ou expirado", HttpStatus.UNAUTHORIZED);
            }

        } catch (Exception e) {
            logger.error("Erro ao validar token: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service funcionando!");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}
