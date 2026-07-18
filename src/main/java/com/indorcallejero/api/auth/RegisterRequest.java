package com.indorcallejero.api.auth;

// Sin @NotBlank todavía -- una contraseña vacía o de 1 caracter pasa hoy
// (SEC-11 del audit). Se cierra en la Etapa 3 junto con el resto de Bean
// Validation, a propósito, mismo criterio que UserNotFoundException.
public record RegisterRequest(String firstName, String lastName, String username, String password) {
}
