package com.indorcallejero.api.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Forma única de error para toda la API -- controller advice y los
 * handlers de seguridad (que no pasan por Spring MVC) escriben este mismo
 * shape, así el cliente nunca tiene que adivinar qué formato le va a
 * llegar según qué falló.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> errors
) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ErrorResponse validation(String path, List<FieldErrorDetail> errors) {
        return new ErrorResponse(
                Instant.now(), 400, HttpStatus.BAD_REQUEST.getReasonPhrase(), "Validación fallida", path, errors);
    }
}
