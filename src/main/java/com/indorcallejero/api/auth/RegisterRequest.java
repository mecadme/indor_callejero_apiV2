package com.indorcallejero.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// SEC-11 del audit resuelto: una contraseña vacía o de 1 caracter ya no
// pasa -- @Valid en AuthController.register hace que MethodArgumentNotValidException
// la frene antes de tocar el service.
public record RegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String username,
        @NotBlank @Size(min = 8, message = "debe tener al menos 8 caracteres") String password
) {
}
