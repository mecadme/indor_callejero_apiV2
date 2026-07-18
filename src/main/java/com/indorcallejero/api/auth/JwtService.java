package com.indorcallejero.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// final: Algorithm.HMAC256(secret) puede lanzar en el constructor si el
// secret es inválido. SpotBugs marca eso como CT_CONSTRUCTOR_THROW -- una
// subclase maliciosa podría, en teoría, resucitar un objeto a medio
// construir capturando esa excepción vía un finalizer. "final" cierra esa
// puerta sin cambiar nada del comportamiento real.
@Component
public final class JwtService {

    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String USER_ID_CLAIM = "userId";

    private final Algorithm algorithm;
    private final String issuer;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${security.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String createAccessToken(UserPrincipal principal) {
        String authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(principal.getUsername())
                .withClaim(AUTHORITIES_CLAIM, authorities)
                .withClaim(USER_ID_CLAIM, principal.getId())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(accessTokenExpirationMs))
                .sign(algorithm);
    }

    // El refresh token NO lleva el claim de authorities a propósito: solo
    // sirve para pedir un access token nuevo, nunca para autorizar una
    // request de negocio directamente. Si alguien roba un refresh token
    // filtrado en un log, no puede usarlo para actuar como el usuario --
    // solo para conseguir un access token, que a su vez expira en minutos.
    public String createRefreshToken(UserPrincipal principal) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(principal.getUsername())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(refreshTokenExpirationMs))
                .sign(algorithm);
    }

    /**
     * Lanza JWTVerificationException (unchecked) si el token es inválido,
     * fue alterado, o expiró. La llamamos desde el filtro y desde el
     * endpoint de refresh -- ambos la dejan sin atrapar por ahora, mismo
     * criterio que el resto de la Etapa 2 (ver análisis de cierre).
     */
    public DecodedJWT validate(String token) {
        return JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token);
    }

    public String extractUsername(DecodedJWT decoded) {
        return decoded.getSubject();
    }

    public Long extractUserId(DecodedJWT decoded) {
        return decoded.getClaim(USER_ID_CLAIM).asLong();
    }

    public Collection<GrantedAuthority> extractAuthorities(DecodedJWT decoded) {
        String claim = decoded.getClaim(AUTHORITIES_CLAIM).asString();
        if (claim == null || claim.isBlank()) {
            return List.of();
        }
        return Arrays.stream(claim.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
