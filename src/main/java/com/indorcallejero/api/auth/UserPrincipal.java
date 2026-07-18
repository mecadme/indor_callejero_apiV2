package com.indorcallejero.api.auth;

import com.indorcallejero.api.user.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Adapta un UserEntity al contrato de Spring Security. La entidad JPA no
 * sabe nada de UserDetails/GrantedAuthority -- esa es una preocupación de
 * seguridad, no de persistencia, y viven en paquetes distintos a propósito.
 */
public class UserPrincipal implements UserDetails {

    private final UserEntity user;

    public UserPrincipal(UserEntity user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    // Sin verificación de email ni suspensión de cuenta en esta etapa (fuera
    // de alcance a propósito -- ver el análisis de la Etapa 2). Si en el
    // futuro aparece esa necesidad real, estos 4 métodos son el único lugar
    // que hay que tocar.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
