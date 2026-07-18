package com.indorcallejero.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sin esto, Swagger UI carga (/v3/api-docs y /swagger-ui/** ya son públicos
 * en SecurityConfig) pero no tiene ningún esquema de seguridad registrado:
 * no aparece el botón "Authorize" para pegar el access token, así que
 * "Try it out" contra cualquier endpoint protegido siempre devuelve 401/403
 * sin importar que el token sea válido. bearerFormat "JWT" es solo
 * metadata para la UI, no cambia la validación real (eso lo sigue haciendo
 * JwtAuthenticationFilter).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI indorCallejeroOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
