package com.indorcallejero.api.information;

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
class InformationServiceTest {

    @Mock
    private InformationRepository informationRepository;

    @Mock
    private InformationMapper informationMapper;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private InformationService informationService;

    @Test
    void getInformationById_devuelveDto_cuandoExiste() {
        InformationEntity entity = new InformationEntity("Nueva fecha", "El torneo arranca el sabado");
        InformationDTO dto = new InformationDTO(1L, "Nueva fecha", "El torneo arranca el sabado", null);
        when(informationRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(informationMapper.toDto(entity)).thenReturn(dto);

        assertThat(informationService.getInformationById(1L)).isEqualTo(dto);
    }

    @Test
    void getInformationById_lanzaInformationNotFoundException_cuandoNoExiste() {
        when(informationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> informationService.getInformationById(999L))
                .isInstanceOf(InformationNotFoundException.class);
    }

    @Test
    void createInformation_guardaConLosDatosDelRequest() {
        CreateInformationRequest request = new CreateInformationRequest("Nueva fecha", "El torneo arranca el sabado");
        InformationDTO expectedDto = new InformationDTO(1L, "Nueva fecha", "El torneo arranca el sabado", null);
        when(informationRepository.save(any(InformationEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informationMapper.toDto(any(InformationEntity.class))).thenReturn(expectedDto);

        InformationDTO result = informationService.createInformation(request);

        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void updateInformation_mutaLaEntidadYGuarda() {
        InformationEntity entity = new InformationEntity("Titulo Viejo", "Contenido viejo");
        UpdateInformationRequest request = new UpdateInformationRequest("Titulo Nuevo", "Contenido nuevo", "foto.png");
        when(informationRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(informationRepository.save(entity)).thenReturn(entity);
        when(informationMapper.toDto(entity)).thenReturn(
                new InformationDTO(1L, "Titulo Nuevo", "Contenido nuevo", "foto.png"));

        informationService.updateInformation(1L, request);

        assertThat(entity.getTitle()).isEqualTo("Titulo Nuevo");
        assertThat(entity.getContent()).isEqualTo("Contenido nuevo");
    }

    @Test
    void updatePhoto_guardaElArchivoYActualizaPhotoUrl() {
        InformationEntity entity = new InformationEntity("Nueva fecha", "El torneo arranca el sabado");
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});
        when(informationRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(storageService.store(eq(file), eq("information"))).thenReturn("information/abc123.png");
        when(informationRepository.save(entity)).thenReturn(entity);
        when(informationMapper.toDto(entity)).thenReturn(
                new InformationDTO(1L, "Nueva fecha", "El torneo arranca el sabado", "/api/files/information/abc123.png"));

        informationService.updatePhoto(1L, file);

        assertThat(entity.getPhotoUrl()).isEqualTo("/api/files/information/abc123.png");
    }

    @Test
    void updatePhoto_lanzaInformationNotFoundException_sinTocarElStorage_cuandoNoExiste() {
        when(informationRepository.findById(999L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> informationService.updatePhoto(999L, file))
                .isInstanceOf(InformationNotFoundException.class);

        verify(storageService, never()).store(any(), any());
    }

    @Test
    void deleteInformation_borra_cuandoExiste() {
        when(informationRepository.existsById(1L)).thenReturn(true);

        informationService.deleteInformation(1L);

        verify(informationRepository).deleteById(1L);
    }

    @Test
    void deleteInformation_lanzaInformationNotFoundException_sinBorrar_cuandoNoExiste() {
        when(informationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> informationService.deleteInformation(999L))
                .isInstanceOf(InformationNotFoundException.class);

        verify(informationRepository, never()).deleteById(any());
    }

    @Test
    void getInformation_mapeaCadaEntidadDeLaPaginaASuDto() {
        InformationEntity entity = new InformationEntity("Nueva fecha", "El torneo arranca el sabado");
        InformationDTO dto = new InformationDTO(1L, "Nueva fecha", "El torneo arranca el sabado", null);
        Pageable pageable = PageRequest.of(0, 20);
        when(informationRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(informationMapper.toDto(entity)).thenReturn(dto);

        Page<InformationDTO> result = informationService.getInformation(pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }
}
