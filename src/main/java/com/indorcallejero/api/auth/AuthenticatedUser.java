package com.indorcallejero.api.auth;

/**
 * Lo que queda en el SecurityContext después de validar un access token.
 * No es UserPrincipal ni UserEntity -- no hubo consulta a la BD para
 * armarlo, todo sale de los claims del JWT. Eso es lo que hace al filtro
 * stateless de verdad.
 */
public record AuthenticatedUser(Long id, String username) {
}
