package com.indorcallejero.api.user;

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
        UserDTO dto = new UserDTO(1L, "Mauro", "Cadme", "mecadme", null, null, Instant.now());
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
        UserDTO expectedDto = new UserDTO(1L, "Nuevo", "Apellido", "mecadme", "bio nueva", null, null);
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
        UserDTO dto = new UserDTO(1L, "Mauro", "Cadme", "mecadme", null, null, Instant.now());
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(userMapper.toDto(entity)).thenReturn(dto);

        Page<UserDTO> result = userService.getUsers(pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }
}
