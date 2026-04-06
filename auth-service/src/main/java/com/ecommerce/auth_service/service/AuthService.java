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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

        private final UserRepository userRepository;
        private final AuthenticationManager authenticationManager;
        private final PasswordEncoder passwordEncoder;
        private final JwtProvider jwtProvider;

        public AuthService(
                        UserRepository userRepository,
                        AuthenticationManager authenticationManager,
                        PasswordEncoder passwordEncoder,
                        JwtProvider jwtProvider) {
                this.userRepository = userRepository;
                this.authenticationManager = authenticationManager;
                this.passwordEncoder = passwordEncoder;
                this.jwtProvider = jwtProvider;
        }

    public AuthResponse register(RegisterRequest request) {
        logger.info("Registrando novo usuário: {}", request.getUsername());

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
            authenticationManager.authenticate(
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

                } catch (org.springframework.security.core.AuthenticationException e) {
                        logger.warn("Falha na autenticação para username={}", request.getUsername());
            throw new AuthenticationException("Username ou senha inválidos");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        logger.info("Renovando access token");

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("Refresh token inválido ou expirado");
        }

        String username = jwtProvider.getUsernameFromToken(refreshToken);

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
