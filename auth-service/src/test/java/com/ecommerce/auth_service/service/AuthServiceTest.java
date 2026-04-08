package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.dto.LoginRequest;
import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.exception.AuthenticationException;
import com.ecommerce.auth_service.exception.UserAlreadyExistsException;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.model.User.UserRole;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.auth_service.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password123", "password123");
        loginRequest = new LoginRequest("testuser", "password123");
        
        testUser = new User("testuser", "test@example.com", "encodedPassword", UserRole.USER);
        testUser.setId(1L);
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void testRegisterSuccess() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtProvider.generateAccessToken(1L, "testuser", "USER")).thenReturn("accessToken");
        when(refreshTokenService.issueToken(testUser))
            .thenReturn(new RefreshTokenService.RefreshTokenIssueResult("refreshToken", "jti-1"));
        when(jwtProvider.getAccessTokenExpirationMs()).thenReturn(86400000L);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(jwtProvider, times(1)).generateAccessToken(1L, "testuser", "USER");
        verify(refreshTokenService).issueToken(testUser);
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when username already exists")
    void testRegisterUserAlreadyExistsUsername() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username já existe");

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when email already exists")
    void testRegisterUserAlreadyExistsEmail() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email já existe");

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("testuser", "password123"));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessToken(1L, "testuser", "USER")).thenReturn("accessToken");
        when(refreshTokenService.issueToken(testUser))
            .thenReturn(new RefreshTokenService.RefreshTokenIssueResult("refreshToken", "jti-2"));
        when(jwtProvider.getAccessTokenExpirationMs()).thenReturn(86400000L);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw AuthenticationException with invalid credentials")
    void testLoginInvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Username ou senha inválidos");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should throw AuthenticationException when refresh token is invalid")
    void testRefreshTokenInvalid() {
        // Arrange
        String invalidToken = "invalid_refresh_token";
        when(refreshTokenService.rotateToken(invalidToken))
                .thenThrow(new AuthenticationException("Refresh token inválido ou expirado"));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Refresh token inválido ou expirado");

        verify(refreshTokenService).rotateToken(invalidToken);
        verify(jwtProvider, never()).generateAccessToken(anyLong(), anyString(), anyString());
    }

    @Test
        @DisplayName("Should rotate refresh token and generate new access token")
    void testRefreshTokenSuccess() {
        // Arrange
        String refreshToken = "valid_refresh_token";
        when(refreshTokenService.rotateToken(refreshToken))
            .thenReturn(new RefreshTokenService.RotationResult(testUser, "rotatedRefreshToken"));
        when(jwtProvider.generateAccessToken(1L, "testuser", "USER")).thenReturn("newAccessToken");
        when(jwtProvider.getAccessTokenExpirationMs()).thenReturn(86400000L);

        // Act
        AuthResponse response = authService.refreshToken(refreshToken);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("rotatedRefreshToken");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");

        verify(refreshTokenService).rotateToken(refreshToken);
    }

    @Test
    @DisplayName("Should revoke refresh token on logout")
    void testRevokeRefreshToken() {
        // Arrange
        String refreshToken = "valid_refresh_token";

        // Act
        authService.revokeRefreshToken(refreshToken);

        // Assert
        verify(refreshTokenService).revokeToken(refreshToken, "LOGOUT");
    }

    @Test
    @DisplayName("Should reject logout when refresh token is invalid")
    void testRevokeRefreshTokenInvalid() {
        // Arrange
        String invalidToken = "x";
        doThrow(new AuthenticationException("Refresh token inválido ou expirado"))
                .when(refreshTokenService).revokeToken(invalidToken, "LOGOUT");

        // Act & Assert
        assertThatThrownBy(() -> authService.revokeRefreshToken(invalidToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Refresh token inválido ou expirado");

        verify(refreshTokenService).revokeToken(invalidToken, "LOGOUT");
    }
}
