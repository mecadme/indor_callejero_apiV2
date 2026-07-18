package com.indorcallejero.api.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.indorcallejero.api.user.UserEntity;
import com.indorcallejero.api.user.UserNotFoundException;
import com.indorcallejero.api.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Transactional a nivel de clase, no un capricho: roles es LAZY (a
 * propósito, ver UserEntity) y open-in-view está apagado (a propósito, ver
 * application.yml de la Etapa 0). Sin una transacción que envuelva todo el
 * caso de uso, UserPrincipal.getAuthorities() explota con
 * LazyInitializationException apenas termina la consulta que trajo al
 * usuario -- pasó de verdad escribiendo esta clase, no es un ejemplo de
 * juguete. La respuesta NO es volver a EAGER ni reencender open-in-view:
 * ambas soluciones ocultarían este mismo problema en cualquier otro lugar
 * del código en vez de resolverlo acá, que es donde corresponde.
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final RateLimiter rateLimiter;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenBlacklist tokenBlacklist,
            RateLimiter rateLimiter,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
        this.rateLimiter = rateLimiter;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        rateLimiter.checkAllowed("register:" + request.username());

        String hashedPassword = passwordEncoder.encode(request.password());
        UserEntity user = new UserEntity(
                request.firstName(), request.lastName(), request.username(), hashedPassword);
        user.grantRole(Role.USER);

        UserEntity saved = userRepository.save(user);
        UserPrincipal principal = new UserPrincipal(saved);
        String accessToken = jwtService.createAccessToken(principal);

        return new AuthResponse(saved.getId(), saved.getUsername(), accessToken);
    }

    // El login pasa por AuthenticationManager, no por un passwordEncoder.matches()
    // manual acá -- eso delega la búsqueda de usuario (UserDetailsServiceImpl)
    // y la verificación de password al DaoAuthenticationProvider que Spring
    // Security arma solo a partir de esos dos beans. Menos código nuestro,
    // camino más probado.
    public LoginResult login(LoginRequest request) {
        rateLimiter.checkAllowed("login:" + request.username());

        UserPrincipal principal;
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            principal = (UserPrincipal) authentication.getPrincipal();
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Usuario o contraseña inválidos");
        }

        String accessToken = jwtService.createAccessToken(principal);
        String refreshToken = jwtService.createRefreshToken(principal);

        return new LoginResult(
                new AuthResponse(principal.getId(), principal.getUsername(), accessToken),
                refreshToken);
    }

    // Rotación: cada refresh invalida el refresh token que lo pidió y emite
    // uno nuevo. Si alguien roba un refresh token y lo usa, el dueño legítimo
    // lo va a notar en el próximo intento propio -- su token ya fue quemado.
    public LoginResult refresh(String refreshToken) {
        DecodedJWT decoded = jwtService.validate(refreshToken);
        String username = jwtService.extractUsername(decoded);

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Usuario inválido"));
        UserPrincipal principal = new UserPrincipal(user);

        tokenBlacklist.blacklist(decoded.getId(), decoded.getExpiresAt().toInstant());

        return new LoginResult(
                new AuthResponse(user.getId(), user.getUsername(), jwtService.createAccessToken(principal)),
                jwtService.createRefreshToken(principal));
    }

    public void logout(String accessToken) {
        DecodedJWT decoded = jwtService.validate(accessToken);
        tokenBlacklist.blacklist(decoded.getId(), decoded.getExpiresAt().toInstant());
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        rateLimiter.checkAllowed("change-password:" + userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BadCredentialsException("La contraseña actual no coincide");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
