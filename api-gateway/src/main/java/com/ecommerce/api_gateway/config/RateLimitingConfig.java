package com.ecommerce.api_gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Configuration
public class RateLimitingConfig {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingConfig.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private final SecretKey jwtSigningKey;

    @Value("${gateway.rate-limit.log-keys:true}")
    private boolean logKeys;

    public RateLimitingConfig(
            @Value("${JWT_SECRET}") String jwtSecret
    ) {
        this.jwtSigningKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public KeyResolver gatewayRateLimitKeyResolver() {
        return exchange -> Mono.just(resolveRateLimitKey(exchange));
    }

    private String resolveRateLimitKey(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/auth/")) {
            String ipKey = "ip:" + resolveClientIp(exchange);
            logResolvedKey(path, ipKey);
            return ipKey;
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            if (StringUtils.hasText(token)) {
                Optional<String> subject = extractSubjectSafely(token);
                if (subject.isPresent()) {
                    String userKey = "user:" + subject.get();
                    logResolvedKey(path, userKey);
                    return userKey;
                }
                logger.debug("Invalid JWT while resolving rate-limit key on path {}, fallback to IP", path);
            }
        }

        String fallbackKey = "ip:" + resolveClientIp(exchange);
        logResolvedKey(path, fallbackKey);
        return fallbackKey;
    }

    private Optional<String> extractSubjectSafely(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject()).filter(StringUtils::hasText);
        } catch (Exception ex) {
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

    private void logResolvedKey(String path, String key) {
        if (logKeys) {
            logger.debug("Resolved rate limit key for path {} => {}", path, key);
        }
    }
}
