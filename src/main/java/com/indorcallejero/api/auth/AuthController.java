package com.indorcallejero.api.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final long refreshTokenExpirationMs;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            @Value("${security.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
            @Value("${security.jwt.cookie-secure}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(request);
        setRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(REFRESH_COOKIE_NAME) String refreshToken,
            HttpServletResponse response
    ) {
        LoginResult result = authService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return result.response();
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String authorizationHeader, HttpServletResponse response) {
        authService.logout(authorizationHeader.replaceFirst("^" + BEARER_PREFIX, ""));
        clearRefreshCookie(response);
    }

    @PostMapping("/change-password")
    public void changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal.id(), request);
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken, Duration.ofMillis(refreshTokenExpirationMs)).toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
