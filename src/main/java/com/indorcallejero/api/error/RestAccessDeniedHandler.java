package com.indorcallejero.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Se dispara cuando SÍ hay una identidad autenticada válida, pero la regla
 * de authorizeHttpRequests la rechaza igual -- en la práctica hoy, sobre
 * todo el anyRequest().denyAll() de última línea de SecurityConfig
 * (cualquier ruta que alguien agregue y se olvide de listar). 403, no 401:
 * sabemos quién sos, la respuesta es "no, igual".
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonErrorWriter jsonErrorWriter;

    public RestAccessDeniedHandler(JsonErrorWriter jsonErrorWriter) {
        this.jsonErrorWriter = jsonErrorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        jsonErrorWriter.write(request, response, HttpStatus.FORBIDDEN, "No tenés permiso para esta acción");
    }
}
