package com.indorcallejero.api.config;

import com.indorcallejero.api.auth.JwtAuthenticationFilter;
import com.indorcallejero.api.error.RestAccessDeniedHandler;
import com.indorcallejero.api.error.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * SEC-03 del audit: la versión original nunca llamaba a
 * authorizeHttpRequests, así que cualquier endpoint sin @PreAuthorize
 * quedaba público por accidente ("fail-open"). Acá se invierte: TODO
 * requiere autenticación salvo lo que se permite a mano, y lo que no
 * coincide con ninguna regla termina en denyAll() ("fail-closed"). Un
 * desarrollador que agregue un controller nuevo y se olvide de esta clase
 * obtiene un 403 al probarlo, no un endpoint abierto al mundo.
 *
 * proxyBeanMethods = false + final: ningún @Bean de acá llama a otro
 * @Bean por método (cada uno recibe lo que necesita como parámetro, no
 * como this.otroBean()), así que no hace falta que Spring genere un proxy
 * CGLIB de esta clase -- arranca más rápido y permite "final", que cierra
 * el mismo CT_CONSTRUCTOR_THROW que en JwtService (el constructor puede
 * lanzar IllegalStateException por el chequeo de SEC-12 de abajo).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public final class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler,
            @Value("${security.cors.allowed-origins}") String allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
        // SEC-12 del audit: origin comodín + allowCredentials(true) es una
        // combinación que el propio spec de CORS prohíbe -- lo rechazamos acá
        // en el arranque en vez de descubrirlo en producción.
        if (allowedOrigins.contains("*")) {
            throw new IllegalStateException(
                    "security.cors.allowed-origins no puede incluir '*' junto con credenciales habilitadas (SEC-12)");
        }
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /error también pasa por este filtro -- es un forward interno de
                        // Spring Boot, no un endpoint nuestro. Sin esta línea, denyAll()
                        // de más abajo intercepta ESE forward y cualquier 500 real (una
                        // excepción sin manejar en el service, por ejemplo) sale como un
                        // 403 que no tiene nada que ver con seguridad -- nos pasó de
                        // verdad probando el rate limiter de esta misma etapa.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        // Logos/fotos servidos son contenido público (marketing), no
                        // datos sensibles -- lo que sube un archivo sigue siendo
                        // authenticated() + @PreAuthorize(ADMIN) más abajo, esto es
                        // solo la lectura.
                        .requestMatchers(HttpMethod.GET, "/api/files/**")
                        .permitAll()
                        // Un load balancer/orquestador tiene que poder pegarle a
                        // /health sin JWT -- es lo único de Actuator que es seguro
                        // dejar público (con show-details=when-authorized, un
                        // anónimo solo ve "UP"/"DOWN", no el detalle de cada
                        // componente).
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                        .permitAll()
                        // El resto de Actuator (métricas, prometheus) puede filtrar
                        // información interna -- consultas SQL, uso de memoria,
                        // conteo de requests por endpoint. Ni siquiera "authenticated()"
                        // alcanza acá, hace falta ser ADMIN.
                        .requestMatchers("/actuator/**")
                        .hasRole("ADMIN")
                        // Cada dominio nuevo tiene que sumarse acá a mano -- es la
                        // fricción a propósito de SEC-03: si te olvidás, el dominio
                        // entero devuelve denyAll(), no queda público por accidente.
                        .requestMatchers("/api/auth/**", "/api/users/**", "/api/teams/**", "/api/players/**",
                                "/api/matches/**", "/api/standings/**", "/api/rounds/**", "/api/referees/**",
                                "/api/ethics-officers/**", "/api/sponsors/**", "/api/information/**",
                                "/api/facebook-videos/**")
                        .authenticated()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Boot arma un DaoAuthenticationProvider solo a partir de estos
    // dos beans (UserDetailsService + PasswordEncoder) cuando pedimos el
    // AuthenticationManager acá -- no hace falta declarar el provider a mano.
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
