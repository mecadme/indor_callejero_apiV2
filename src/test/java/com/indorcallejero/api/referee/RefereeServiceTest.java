package com.indorcallejero.api.referee;

import com.indorcallejero.api.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefereeServiceTest {

    @Mock
    private RefereeRepository refereeRepository;

    @Mock
    private RefereeMapper refereeMapper;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private RefereeService refereeService;

    @Test
    void getRefereeById_devuelveDto_cuandoExiste() {
        RefereeEntity entity = new RefereeEntity("Juan", "Pérez", "LIC-1");
        RefereeDTO dto = new RefereeDTO(1L, "Juan", "Pérez", "LIC-1", null);
        when(refereeRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(refereeMapper.toDto(entity)).thenReturn(dto);

        assertThat(refereeService.getRefereeById(1L)).isEqualTo(dto);
    }

    @Test
    void getRefereeById_lanzaRefereeNotFoundException_cuandoNoExiste() {
        when(refereeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refereeService.getRefereeById(999L))
                .isInstanceOf(RefereeNotFoundException.class);
    }

    @Test
    void createReferee_guardaConLosDatosDelRequest() {
        CreateRefereeRequest request = new CreateRefereeRequest("Juan", "Pérez", "LIC-1");
        RefereeDTO expectedDto = new RefereeDTO(1L, "Juan", "Pérez", "LIC-1", null);
        when(refereeRepository.save(any(RefereeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(refereeMapper.toDto(any(RefereeEntity.class))).thenReturn(expectedDto);

        RefereeDTO result = refereeService.createReferee(request);

        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void updateReferee_mutaLaEntidadYGuarda() {
        RefereeEntity entity = new RefereeEntity("Nombre Viejo", "Apellido Viejo", "LIC-1");
        UpdateRefereeRequest request = new UpdateRefereeRequest("Nombre Nuevo", "Apellido Nuevo", "LIC-2", "foto.png");
        when(refereeRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(refereeRepository.save(entity)).thenReturn(entity);
        when(refereeMapper.toDto(entity)).thenReturn(
                new RefereeDTO(1L, "Nombre Nuevo", "Apellido Nuevo", "LIC-2", "foto.png"));

        refereeService.updateReferee(1L, request);

        assertThat(entity.getFirstName()).isEqualTo("Nombre Nuevo");
        assertThat(entity.getLastName()).isEqualTo("Apellido Nuevo");
        assertThat(entity.getLicenseNumber()).isEqualTo("LIC-2");
    }

    @Test
    void updatePhoto_guardaElArchivoYActualizaPhotoUrl() {
        RefereeEntity entity = new RefereeEntity("Juan", "Pérez", "LIC-1");
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});
        when(refereeRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(storageService.store(eq(file), eq("referees"))).thenReturn("referees/abc123.png");
        when(refereeRepository.save(entity)).thenReturn(entity);
        when(refereeMapper.toDto(entity)).thenReturn(
                new RefereeDTO(1L, "Juan", "Pérez", "LIC-1", "/api/files/referees/abc123.png"));

        refereeService.updatePhoto(1L, file);

        assertThat(entity.getPhotoUrl()).isEqualTo("/api/files/referees/abc123.png");
    }

    @Test
    void updatePhoto_lanzaRefereeNotFoundException_sinTocarElStorage_cuandoNoExiste() {
        when(refereeRepository.findById(999L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> refereeService.updatePhoto(999L, file))
                .isInstanceOf(RefereeNotFoundException.class);

        verify(storageService, never()).store(any(), any());
    }

    @Test
    void deleteReferee_borra_cuandoExiste() {
        when(refereeRepository.existsById(1L)).thenReturn(true);

        refereeService.deleteReferee(1L);

        verify(refereeRepository).deleteById(1L);
    }

    @Test
    void deleteReferee_lanzaRefereeNotFoundException_sinBorrar_cuandoNoExiste() {
        when(refereeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> refereeService.deleteReferee(999L))
                .isInstanceOf(RefereeNotFoundException.class);

        verify(refereeRepository, never()).deleteById(any());
    }

    @Test
    void getReferees_mapeaCadaEntidadDeLaPaginaASuDto() {
        RefereeEntity entity = new RefereeEntity("Juan", "Pérez", "LIC-1");
        RefereeDTO dto = new RefereeDTO(1L, "Juan", "Pérez", "LIC-1", null);
        Pageable pageable = PageRequest.of(0, 20);
        when(refereeRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(refereeMapper.toDto(entity)).thenReturn(dto);

        Page<RefereeDTO> result = refereeService.getReferees(pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }
}
