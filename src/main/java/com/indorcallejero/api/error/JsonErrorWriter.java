package com.indorcallejero.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Las denegaciones que Spring Security resuelve antes de llegar al
 * DispatcherServlet (sin token, denyAll de última línea) no pasan por
 * @RestControllerAdvice -- no hay "controller" todavía. Este componente
 * escribe el mismo ErrorResponse a mano, para que el cliente vea la misma
 * forma de error sin importar en qué capa se decidió el rechazo.
 *
 * ObjectMapper es tools.jackson.databind, no com.fasterxml.jackson.databind:
 * Spring Boot 4 pasó a Jackson 3, que renombró su namespace de paquete. El
 * com.fasterxml.jackson.databind que ves en el dependency tree lo trae
 * java-jwt (Etapa 2) para su propio uso interno -- son dos librerías
 * Jackson distintas conviviendo, no una duplicada por error.
 */
@Component
public class JsonErrorWriter {

    private final ObjectMapper objectMapper;

    public JsonErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        // getWriter() por sí solo hereda ISO-8859-1 del default del Servlet
        // spec -- sin este setCharacterEncoding, cualquier tilde en el
        // mensaje sale corrupta (lo vimos en carne propia: "autenticación"
        // salió "autenticaci?n" antes de este fix).
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(ErrorResponse.of(status, message, request.getRequestURI())));
    }
}
