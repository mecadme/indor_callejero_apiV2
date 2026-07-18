package com.indorcallejero.api.auth;

/**
 * El refresh token viaja separado de AuthResponse porque el Controller lo
 * pone en una cookie httpOnly, nunca en el cuerpo JSON que ve JavaScript.
 */
public record LoginResult(AuthResponse response, String refreshToken) {
}
