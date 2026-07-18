package com.indorcallejero.api.user;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<UserDTO> getUsers(Pageable pageable) {
        return userService.getUsers(pageable);
    }

    @GetMapping("/{id}")
    public UserDTO getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // Capa 1 (SecurityConfig): hace falta estar autenticado para llegar acá.
    // Capa 2 (esta anotación): estar autenticado no alcanza -- tenés que ser
    // vos mismo o un ADMIN. Son dos preguntas distintas (¿quién sos? vs.
    // ¿podés tocar ESTE recurso?) y por eso viven en dos lugares distintos.
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id()")
    @PatchMapping("/{id}")
    public UserDTO updateProfile(@PathVariable Long id, @Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateProfile(id, request);
    }
}
