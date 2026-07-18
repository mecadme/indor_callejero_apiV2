package com.indorcallejero.api.user;

import com.indorcallejero.api.auth.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_devuelveDto_cuandoExiste() {
        UserEntity entity = new UserEntity("Mauro", "Cadme", "mecadme", "hashed");
        UserDTO dto = new UserDTO(1L, "Mauro", "Cadme", "mecadme", null, null, Instant.now(), Set.of(Role.USER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userMapper.toDto(entity)).thenReturn(dto);

        UserDTO result = userService.getUserById(1L);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void getUserById_lanzaUserNotFoundException_cuandoNoExiste() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProfile_mutaLaEntidadYGuarda() {
        UserEntity entity = new UserEntity("Mauro", "Cadme", "mecadme", "hashed");
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Nuevo", "Apellido", "bio nueva", null);
        UserDTO expectedDto = new UserDTO(1L, "Nuevo", "Apellido", "mecadme", "bio nueva", null, null, Set.of(Role.USER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toDto(entity)).thenReturn(expectedDto);

        UserDTO result = userService.updateProfile(1L, request);

        assertThat(entity.getFirstName()).isEqualTo("Nuevo");
        assertThat(entity.getLastName()).isEqualTo("Apellido");
        assertThat(entity.getBio()).isEqualTo("bio nueva");
        assertThat(result).isEqualTo(expectedDto);
        verify(userRepository).save(entity);
    }

    @Test
    void updateProfile_lanzaUserNotFoundException_cuandoNoExiste() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("X", "Y", null, null);

        assertThatThrownBy(() -> userService.updateProfile(404L, request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUsers_mapeaCadaEntidadDeLaPaginaASuDto() {
        UserEntity entity = new UserEntity("Mauro", "Cadme", "mecadme", "hashed");
        UserDTO dto = new UserDTO(1L, "Mauro", "Cadme", "mecadme", null, null, Instant.now(), Set.of(Role.USER));
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(userMapper.toDto(entity)).thenReturn(dto);

        Page<UserDTO> result = userService.getUsers(pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void assignRole_agregaElRolYGuarda() {
        UserEntity entity = new UserEntity("Mauro", "Cadme", "mecadme", "hashed");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toDto(entity)).thenAnswer(inv -> new UserDTO(
                1L, "Mauro", "Cadme", "mecadme", null, null, Instant.now(), entity.getRoles()));

        UserDTO result = userService.assignRole(1L, Role.MANAGER);

        assertThat(entity.getRoles()).contains(Role.MANAGER);
        assertThat(result.roles()).contains(Role.MANAGER);
        verify(userRepository).save(entity);
    }

    @Test
    void assignRole_lanzaUserNotFoundException_cuandoNoExiste() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.assignRole(404L, Role.MANAGER))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void revokeRole_sacaElRolYGuarda() {
        UserEntity entity = new UserEntity("Mauro", "Cadme", "mecadme", "hashed");
        entity.grantRole(Role.MANAGER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toDto(entity)).thenAnswer(inv -> new UserDTO(
                1L, "Mauro", "Cadme", "mecadme", null, null, Instant.now(), entity.getRoles()));

        UserDTO result = userService.revokeRole(1L, Role.MANAGER);

        assertThat(entity.getRoles()).doesNotContain(Role.MANAGER);
        assertThat(result.roles()).doesNotContain(Role.MANAGER);
        verify(userRepository).save(entity);
    }

    @Test
    void revokeRole_esIdempotente_cuandoElUsuarioNoTeniaEseRol() {
        UserEntity entity = new UserEntity("Mauro", "Cadme", "mecadme", "hashed");
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);
        when(userMapper.toDto(entity)).thenReturn(
                new UserDTO(1L, "Mauro", "Cadme", "mecadme", null, null, Instant.now(), Set.of()));

        UserDTO result = userService.revokeRole(1L, Role.MANAGER);

        assertThat(result.roles()).isEmpty();
    }

    @Test
    void revokeRole_lanzaUserNotFoundException_cuandoNoExiste() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.revokeRole(404L, Role.MANAGER))
                .isInstanceOf(UserNotFoundException.class);
    }
}
