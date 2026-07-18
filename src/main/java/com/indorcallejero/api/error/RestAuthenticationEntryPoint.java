package com.indorcallejero.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Se dispara cuando una ruta "authenticated()" recibe un request sin
 * autenticación válida (sin token, token corrupto, o expirado). Antes de
 * esta clase, Spring Security devolvía un 403 vacío por default -- 401 es
 * lo correcto acá: "no sabemos quién sos", no "sabemos quién sos y no
 * podés". Ver RestAccessDeniedHandler para la diferencia con 403.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonErrorWriter jsonErrorWriter;

    public RestAuthenticationEntryPoint(JsonErrorWriter jsonErrorWriter) {
        this.jsonErrorWriter = jsonErrorWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        jsonErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Se requiere autenticación");
    }
}
