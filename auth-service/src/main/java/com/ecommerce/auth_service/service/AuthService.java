package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.dto.LoginRequest;
import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.exception.AuthenticationException;
import com.ecommerce.auth_service.exception.UserAlreadyExistsException;
import com.ecommerce.auth_service.exception.UserNotFoundException;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.model.User.UserRole;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.auth_service.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    public AuthResponse register(RegisterRequest request) {
        logger.info("Registrando novo usuário: {}", request.getUsername());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("As senhas não coincidem");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username já existe: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email já existe: " + request.getEmail());
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                UserRole.USER
        );

        User savedUser = userRepository.save(user);
        logger.info("Usuário registrado com sucesso. ID: {}", savedUser.getId());

        String accessToken = jwtProvider.generateAccessToken(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole().toString()
        );

        String refreshToken = jwtProvider.generateRefreshToken(
                savedUser.getId(),
                savedUser.getUsername()
        );

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().toString()
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtProvider.getAccessTokenExpirationMs(),
                userInfo
        );
    }

    public AuthResponse login(LoginRequest request) {
        logger.info("Login do usuário: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UserNotFoundException(
                            "Usuário não encontrado: " + request.getUsername()
                    ));

            String accessToken = jwtProvider.generateAccessToken(
                    user.getId(),
                    user.getUsername(),
                    user.getRole().toString()
            );

            String refreshToken = jwtProvider.generateRefreshToken(
                    user.getId(),
                    user.getUsername()
            );

            logger.info("Login bem-sucedido para usuário: {}", request.getUsername());

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().toString()
            );

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    jwtProvider.getAccessTokenExpirationMs(),
                    userInfo
            );

        } catch (Exception e) {
            logger.error("Falha na autenticação: {}", e.getMessage());
            throw new AuthenticationException("Username ou senha inválidos");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        logger.info("Renovando access token");

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("Refresh token inválido ou expirado");
        }

        String username = jwtProvider.getUsernameFromToken(refreshToken);
        Long userId = jwtProvider.getUserIdFromToken(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário não encontrado: " + username
                ));

        String accessToken = jwtProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().toString()
        );

        String newRefreshToken = jwtProvider.generateRefreshToken(
                user.getId(),
                user.getUsername()
        );

        logger.info("Token renovado com sucesso para usuário: {}", username);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().toString()
        );

        return new AuthResponse(
                accessToken,
                newRefreshToken,
                jwtProvider.getAccessTokenExpirationMs(),
                userInfo
        );
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return jwtProvider.validateToken(token);
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getTokenInfo(String token) {
        if (!jwtProvider.validateToken(token)) {
            throw new AuthenticationException("Token inválido ou expirado");
        }

        String username = jwtProvider.getUsernameFromToken(token);
        Long userId = jwtProvider.getUserIdFromToken(token);
        String role = jwtProvider.getRoleFromToken(token);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário não encontrado: " + username
                ));

        return new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                role
        );
    }
}
