package com.ecommerce.api_gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Configuration
public class RateLimitingConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey jwtSigningKey;

    public RateLimitingConfig(@Value("${JWT_SECRET:local-dev-secret-key-with-at-least-64-characters-for-hs512-signature-1234567890}") String jwtSecret) {
        this.jwtSigningKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public KeyResolver gatewayRateLimitKeyResolver() {
        return exchange -> Mono.just(resolveRateLimitKey(exchange));
    }

    private String resolveRateLimitKey(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/auth/")) {
            return "ip:" + resolveClientIp(exchange);
        }

        return extractBearerToken(exchange)
                .flatMap(this::extractSubjectSafely)
                .filter(StringUtils::hasText)
                .map(subject -> "user:" + subject)
                .orElseGet(() -> "ip:" + resolveClientIp(exchange));
    }

    private Optional<String> extractBearerToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
    }

    private Optional<String> extractSubjectSafely(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String clientIp = forwardedFor.split(",")[0].trim();
            if (StringUtils.hasText(clientIp)) {
                return clientIp;
            }
        }

        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(address -> address.getAddress().getHostAddress())
                .orElse("unknown");
    }
}
