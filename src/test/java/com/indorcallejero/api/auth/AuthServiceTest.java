package com.indorcallejero.api.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.indorcallejero.api.user.UserEntity;
import com.indorcallejero.api.user.UserNotFoundException;
import com.indorcallejero.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_hasheaLaPasswordYOtorgaRolUser() {
        RegisterRequest request = new RegisterRequest("Mauro", "Cadme", "mecadme", "unaClaveSegura123");
        when(passwordEncoder.encode("unaClaveSegura123")).thenReturn("hashed");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.createAccessToken(any(UserPrincipal.class))).thenReturn("access-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getRoles()).containsExactly(Role.USER);
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.username()).isEqualTo("mecadme");
    }

    @Test
    void login_delegaEnAuthenticationManagerYEmiteAmbosTokens() {
        LoginRequest request = new LoginRequest("mecadme", "unaClaveSegura123");
        UserEntity entity = new UserEntity("Mauro", "Cadme", "mecadme", "hashed");
        entity.grantRole(Role.USER);
        UserPrincipal principal = new UserPrincipal(entity);
        Authentication authResult = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authResult);
        when(jwtService.createAccessToken(principal)).thenReturn("access-token");
        when(jwtService.createRefreshToken(principal)).thenReturn("refresh-token");

        LoginResult result = authService.login(request);

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_traduceCualquierAuthenticationExceptionEnBadCredentials() {
        LoginRequest request = new LoginRequest("mecadme", "incorrecta");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("nope"));

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void changePassword_rechazaSiLaPasswordActualNoCoincide() {
        UserEntity entity = new UserEntity("Mauro", "Cadme", "mecadme", "hashedOld");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("incorrecta", "hashedOld")).thenReturn(false);

        assertThatThrownBy(() ->
                authService.changePassword(1L, new ChangePasswordRequest("incorrecta", "nuevaClave123")))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_actualizaElHash_cuandoLaPasswordActualCoincide() {
        UserEntity entity = new UserEntity("Mauro", "Cadme", "mecadme", "hashedOld");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("actual123", "hashedOld")).thenReturn(true);
        when(passwordEncoder.encode("nuevaClave123")).thenReturn("hashedNew");

        authService.changePassword(1L, new ChangePasswordRequest("actual123", "nuevaClave123"));

        assertThat(entity.getPassword()).isEqualTo("hashedNew");
        verify(userRepository).save(entity);
    }

    @Test
    void changePassword_lanzaUserNotFoundException_cuandoElUsuarioNoExiste() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.changePassword(404L, new ChangePasswordRequest("a", "nuevaClave123")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void logout_deslistaElJtiDelAccessToken_conSuFechaDeExpiracionReal() {
        // Date solo tiene precisión de milisegundos -- arrancar de un Date
        // (no de Instant.now(), que puede tener nanosegundos) evita un
        // falso negativo por precisión al ida-y-vuelta Date->Instant que
        // hace AuthService.logout() internamente.
        Date expiresAtDate = new Date(System.currentTimeMillis() + 900_000);
        DecodedJWT decoded = org.mockito.Mockito.mock(DecodedJWT.class);
        when(decoded.getId()).thenReturn("jti-123");
        when(decoded.getExpiresAt()).thenReturn(expiresAtDate);
        when(jwtService.validate(anyString())).thenReturn(decoded);

        authService.logout("access-token");

        verify(tokenBlacklist).blacklist("jti-123", expiresAtDate.toInstant());
    }
}
