package com.ecommerce.product_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();

        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }

        return "/health".equals(path)
                || "/api/products/health".equals(path)
                || "/api/products/public/health".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");

            // Sem token: segue fluxo. SecurityConfig decide se endpoint exige autenticação.
            if (!StringUtils.hasText(authorizationHeader)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Header malformado: não autentica.
            if (!authorizationHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authorizationHeader.substring(7).trim();
            if (!StringUtils.hasText(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtProvider.validateToken(token)) {
                String username = jwtProvider.getUsernameFromToken(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.emptyList()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("JWT válido para usuário={}", username);
            } else {
                logger.debug("JWT inválido para path={}", request.getServletPath());
            }
        } catch (Exception ex) {
            logger.warn("Falha ao processar JWT no product-service path={}: {}",
                    request.getServletPath(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
