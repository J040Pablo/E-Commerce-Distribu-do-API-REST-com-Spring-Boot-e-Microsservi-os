package com.ecommerce.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Componente para criação e validação de JWT
 */
@Component
public class JwtProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtProvider.class);

    @Value("${jwt.secret:ecommerce_secret_key_very_long_for_hs512_algorithm}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 horas em ms
    private long jwtExpirationMs;

    @Value("${jwt.refresh.expiration:604800000}") // 7 dias em ms
    private long jwtRefreshExpirationMs;

    /**
     * Gera token de acesso (access token)
     */
    public String generateAccessToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, jwtExpirationMs);
    }

    /**
     * Gera token de refresh
     */
    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, null, jwtRefreshExpirationMs);
    }

    /**
     * Gera um token JWT genérico
     */
    private String generateToken(Long userId, String username, String role, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        logger.debug("Token gerado com sucesso para usuário: {}", username);
        return token;
    }

    /**
     * Valida se o token é válido
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            logger.error("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrai o username do token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Extrai o userId do token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extrai a role do token
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * Extrai a data de expiração do token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Verifica se o token expirou
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extrai todas as claims do token
     */
    private Claims getAllClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Retorna o tempo de expiração do access token em milisegundos
     */
    public long getAccessTokenExpirationMs() {
        return jwtExpirationMs;
    }
}
