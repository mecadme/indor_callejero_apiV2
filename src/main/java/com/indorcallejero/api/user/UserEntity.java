package com.indorcallejero.api.user;

import com.indorcallejero.api.auth.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String bio;

    private String imageUrl;

    @Column(updatable = false)
    private Instant createdAt;

    // FetchType.LAZY explícito, no el EAGER que tenía la entidad original para
    // esta misma relación. Cargar los roles es barato (un join chico), pero la
    // regla del proyecto es LAZY por defecto siempre — sin excepciones caso a
    // caso, para no reabrir la discusión en cada entidad nueva (PERF-02).
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = new HashSet<>();

    protected UserEntity() {
        // JPA exige un constructor sin argumentos
    }

    public UserEntity(String firstName, String lastName, String username, String hashedPassword) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = hashedPassword;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void grantRole(Role role) {
        this.roles.add(role);
    }

    // Idempotente a propósito, como grantRole: otorgar un rol que ya tenés
    // o sacar uno que ya no tenés no es un error, es un no-op -- el estado
    // final que el caller quería ya es el que hay.
    public void revokeRole(Role role) {
        this.roles.remove(role);
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public String getPassword() {
        return password;
    }

    public void changePassword(String newHashedPassword) {
        this.password = newHashedPassword;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
