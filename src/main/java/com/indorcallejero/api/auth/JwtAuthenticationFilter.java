package com.indorcallejero.api.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

/**
 * SEC-05 del audit: el filtro original dejaba escapar la excepción de
 * validación del JWT (token corrupto/expirado) como un 500 sin control.
 * Acá SÍ atrapamos esa excepción, a diferencia del resto de los errores de
 * esta etapa -- porque un filtro roto tumba TODA la app para cualquiera con
 * un token viejo, no un endpoint puntual. El radio de explosión es
 * demasiado grande para dejarlo pendiente hasta la Etapa 3.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklist tokenBlacklist) {
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            DecodedJWT decoded = jwtService.validate(token);

            if (!tokenBlacklist.isBlacklisted(decoded.getId())) {
                Collection<GrantedAuthority> authorities = jwtService.extractAuthorities(decoded);
                AuthenticatedUser principal = new AuthenticatedUser(
                        jwtService.extractUserId(decoded),
                        jwtService.extractUsername(decoded)
                );
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JWTVerificationException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
