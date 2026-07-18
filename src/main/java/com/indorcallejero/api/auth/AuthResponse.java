package com.indorcallejero.api.auth;

// El refresh token NO va acá -- viaja como cookie httpOnly (ver AuthController).
// Ponerlo en el body sería exponerlo a cualquier script de la página (XSS),
// justo lo que una cookie httpOnly evita por diseño.
public record AuthResponse(Long userId, String username, String accessToken) {
}
