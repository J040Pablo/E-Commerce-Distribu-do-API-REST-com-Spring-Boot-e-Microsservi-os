package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.exception.AuthenticationException;
import com.ecommerce.auth_service.model.RefreshToken;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.repository.RefreshTokenRepository;
import com.ecommerce.auth_service.security.JwtProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;

@Service
@Transactional
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final MeterRegistry meterRegistry;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtProvider jwtProvider,
                               ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    public RefreshTokenIssueResult issueToken(User user) {
        String rawToken = jwtProvider.generateRefreshToken(user.getId(), user.getUsername());
        String jwtId = jwtProvider.getTokenId(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setJwtId(jwtId);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setToken("ref:" + jwtId);
        refreshToken.setExpiryDate(toLocalDateTimeUtc(jwtProvider.getExpirationDateFromToken(rawToken)));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        incrementMetric("auth.refresh.issued");

        logger.info("Refresh token emitido para userId={} jwtId={}", user.getId(), jwtId);
        return new RefreshTokenIssueResult(rawToken, jwtId);
    }

    public RotationResult rotateToken(String rawToken) {
        if (!jwtProvider.validateToken(rawToken) || !jwtProvider.isRefreshToken(rawToken)) {
            incrementMetric("auth.refresh.rejected", "reason", "invalid_or_expired");
            throw new AuthenticationException("Refresh token inválido ou expirado");
        }

        String jwtId = jwtProvider.getTokenId(rawToken);
        RefreshToken storedToken = refreshTokenRepository.findByJwtId(jwtId)
                .orElseThrow(() -> {
                    incrementMetric("auth.refresh.rejected", "reason", "not_found");
                    return new AuthenticationException("Refresh token não reconhecido");
                });

        String incomingTokenHash = hashToken(rawToken);
        if (!constantTimeEquals(storedToken.getTokenHash(), incomingTokenHash)) {
            incrementMetric("auth.refresh.rejected", "reason", "hash_mismatch");
            logger.warn("Hash de refresh token inconsistente para userId={} jwtId={}", storedToken.getUser().getId(), jwtId);
            throw new AuthenticationException("Refresh token inválido");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!storedToken.isActive(now)) {
            incrementMetric("auth.refresh.rejected", "reason", storedToken.isRevoked() ? "reused" : "expired");
            handleReplayAttempt(storedToken, now);
            throw new AuthenticationException("Refresh token já utilizado, revogado ou expirado");
        }

        User user = storedToken.getUser();
        RefreshTokenIssueResult newToken = issueToken(user);

        storedToken.setRevoked(true);
        storedToken.setRevokedAt(now);
        storedToken.setRevokeReason("ROTATED");
        storedToken.setReplacedByJwtId(newToken.jwtId());
        storedToken.setLastUsedAt(now);

        refreshTokenRepository.save(storedToken);
        incrementMetric("auth.refresh.rotated");

        logger.info("Refresh token rotacionado com sucesso userId={} oldJwtId={} newJwtId={}",
                user.getId(), storedToken.getJwtId(), newToken.jwtId());

        return new RotationResult(user, newToken.rawToken());
    }

    public void revokeToken(String rawToken, String reason) {
        if (rawToken == null || rawToken.isBlank()) {
            incrementMetric("auth.refresh.revoke.rejected", "reason", "blank");
            throw new AuthenticationException("Refresh token inválido ou expirado");
        }

        if (!jwtProvider.validateToken(rawToken)) {
            logger.info("Logout rejeitado: refresh token inválido ou expirado");
            incrementMetric("auth.refresh.revoke.rejected", "reason", "invalid_or_expired");
            throw new AuthenticationException("Refresh token inválido ou expirado");
        }

        if (!jwtProvider.isRefreshToken(rawToken)) {
            logger.info("Logout rejeitado: token não é refresh token");
            incrementMetric("auth.refresh.revoke.rejected", "reason", "wrong_type");
            throw new AuthenticationException("Refresh token inválido ou expirado");
        }

        String jwtId = jwtProvider.getTokenId(rawToken);
        if (jwtId == null || jwtId.isBlank()) {
            logger.info("Logout rejeitado: refresh token sem jti");
            incrementMetric("auth.refresh.revoke.rejected", "reason", "missing_jti");
            throw new AuthenticationException("Refresh token inválido ou expirado");
        }

        RefreshToken token = refreshTokenRepository.findByJwtId(jwtId)
                .orElseThrow(() -> {
                    incrementMetric("auth.refresh.revoke.rejected", "reason", "not_found");
                    return new AuthenticationException("Refresh token não reconhecido");
                });

        if (token.isRevoked()) {
            incrementMetric("auth.refresh.revoke.rejected", "reason", "already_revoked");
            throw new AuthenticationException("Refresh token já revogado");
        }

        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
        token.setRevokeReason(reason);
        refreshTokenRepository.save(token);
        incrementMetric("auth.refresh.revoked", "reason", reason);
        logger.info("Refresh token revogado userId={} jwtId={} reason={}", token.getUser().getId(), jwtId, reason);
    }

    public int purgeExpiredTokens() {
        int removed = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now(ZoneOffset.UTC));
        logger.info("Limpeza de refresh tokens expirados concluída. removidos={}", removed);
        return removed;
    }

    private void handleReplayAttempt(RefreshToken token, LocalDateTime now) {
        if (token.isRevoked()) {
            int revoked = refreshTokenRepository.revokeAllActiveByUserId(
                    token.getUser().getId(),
                    "REPLAY_DETECTED",
                    now
            );
            logger.warn("Possível replay detectado para userId={}. Tokens ativos revogados={}", token.getUser().getId(), revoked);
            incrementMetric("auth.refresh.replay.detected");
        }
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private LocalDateTime toLocalDateTimeUtc(java.util.Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    private void incrementMetric(String name, String... tags) {
        if (meterRegistry == null) {
            return;
        }

        Counter.builder(name)
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }

    public record RefreshTokenIssueResult(String rawToken, String jwtId) {
    }

    public record RotationResult(User user, String newRefreshToken) {
    }
}
