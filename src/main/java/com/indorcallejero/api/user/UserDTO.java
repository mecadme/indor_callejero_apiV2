package com.indorcallejero.api.user;

import com.indorcallejero.api.auth.Role;

import java.time.Instant;
import java.util.Set;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String username,
        String bio,
        String imageUrl,
        Instant createdAt,
        Set<Role> roles
) {
    // Copia inmutable, no la referencia tal cual: MapStruct mapea roles
    // directo desde UserEntity.getRoles() (que ya devuelve un
    // unmodifiableSet, pero SpotBugs no puede ver eso desde acá) y algún
    // caller manual podría pasar un HashSet mutable -- este constructor
    // compacto es el único punto de entrada, así que alcanza con
    // blindarlo una vez.
    public UserDTO {
        roles = Set.copyOf(roles);
    }
}
