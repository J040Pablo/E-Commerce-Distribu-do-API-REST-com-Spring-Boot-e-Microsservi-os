package com.ecommerce.auth_service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        String method = request.getMethod();

        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }

        return (HttpMethod.POST.matches(method)
                && ("/auth/register".equals(path)
                || "/api/auth/register".equals(path)
                || "/auth/login".equals(path)
                || "/api/auth/login".equals(path)
                || "/auth/refresh".equals(path)
                || "/api/auth/refresh".equals(path)
                || "/auth/logout".equals(path)
                || "/api/auth/logout".equals(path)))
                || (HttpMethod.GET.matches(method)
                && ("/auth/health".equals(path)
                || "/api/auth/health".equals(path)));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");

            // Sem token: segue a cadeia normalmente. Endpoints protegidos serão bloqueados pelo Security.
            if (!StringUtils.hasText(authorizationHeader)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Header inválido/malformado: não autentica, apenas segue a cadeia.
            if (!authorizationHeader.startsWith("Bearer ")) {
                logger.debug("Authorization header sem prefixo Bearer em path={}", request.getServletPath());
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("JWT validado para usuário: {}", username);
            } else {
                logger.debug("JWT ausente/inválido para path={}", request.getServletPath());
            }
        } catch (Exception ex) {
            logger.warn("Falha ao processar JWT em path={}: {}", request.getServletPath(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
