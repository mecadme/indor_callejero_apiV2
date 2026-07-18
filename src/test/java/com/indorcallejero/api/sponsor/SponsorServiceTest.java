package com.indorcallejero.api.sponsor;

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
class SponsorServiceTest {

    @Mock
    private SponsorRepository sponsorRepository;

    @Mock
    private SponsorMapper sponsorMapper;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private SponsorService sponsorService;

    @Test
    void getSponsorById_devuelveDto_cuandoExiste() {
        SponsorEntity entity = new SponsorEntity("Coca-Cola", "https://coca-cola.com");
        SponsorDTO dto = new SponsorDTO(1L, "Coca-Cola", "https://coca-cola.com", null);
        when(sponsorRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sponsorMapper.toDto(entity)).thenReturn(dto);

        assertThat(sponsorService.getSponsorById(1L)).isEqualTo(dto);
    }

    @Test
    void getSponsorById_lanzaSponsorNotFoundException_cuandoNoExiste() {
        when(sponsorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sponsorService.getSponsorById(999L))
                .isInstanceOf(SponsorNotFoundException.class);
    }

    @Test
    void createSponsor_guardaConLosDatosDelRequest() {
        CreateSponsorRequest request = new CreateSponsorRequest("Coca-Cola", "https://coca-cola.com");
        SponsorDTO expectedDto = new SponsorDTO(1L, "Coca-Cola", "https://coca-cola.com", null);
        when(sponsorRepository.save(any(SponsorEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sponsorMapper.toDto(any(SponsorEntity.class))).thenReturn(expectedDto);

        SponsorDTO result = sponsorService.createSponsor(request);

        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void updateSponsor_mutaLaEntidadYGuarda() {
        SponsorEntity entity = new SponsorEntity("Nombre Viejo", "https://viejo.com");
        UpdateSponsorRequest request = new UpdateSponsorRequest("Nombre Nuevo", "https://nuevo.com", "foto.png");
        when(sponsorRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sponsorRepository.save(entity)).thenReturn(entity);
        when(sponsorMapper.toDto(entity)).thenReturn(
                new SponsorDTO(1L, "Nombre Nuevo", "https://nuevo.com", "foto.png"));

        sponsorService.updateSponsor(1L, request);

        assertThat(entity.getName()).isEqualTo("Nombre Nuevo");
        assertThat(entity.getWebsiteUrl()).isEqualTo("https://nuevo.com");
    }

    @Test
    void updatePhoto_guardaElArchivoYActualizaPhotoUrl() {
        SponsorEntity entity = new SponsorEntity("Coca-Cola", "https://coca-cola.com");
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});
        when(sponsorRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(storageService.store(eq(file), eq("sponsors"))).thenReturn("sponsors/abc123.png");
        when(sponsorRepository.save(entity)).thenReturn(entity);
        when(sponsorMapper.toDto(entity)).thenReturn(
                new SponsorDTO(1L, "Coca-Cola", "https://coca-cola.com", "/api/files/sponsors/abc123.png"));

        sponsorService.updatePhoto(1L, file);

        assertThat(entity.getPhotoUrl()).isEqualTo("/api/files/sponsors/abc123.png");
    }

    @Test
    void updatePhoto_lanzaSponsorNotFoundException_sinTocarElStorage_cuandoNoExiste() {
        when(sponsorRepository.findById(999L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> sponsorService.updatePhoto(999L, file))
                .isInstanceOf(SponsorNotFoundException.class);

        verify(storageService, never()).store(any(), any());
    }

    @Test
    void deleteSponsor_borra_cuandoExiste() {
        when(sponsorRepository.existsById(1L)).thenReturn(true);

        sponsorService.deleteSponsor(1L);

        verify(sponsorRepository).deleteById(1L);
    }

    @Test
    void deleteSponsor_lanzaSponsorNotFoundException_sinBorrar_cuandoNoExiste() {
        when(sponsorRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> sponsorService.deleteSponsor(999L))
                .isInstanceOf(SponsorNotFoundException.class);

        verify(sponsorRepository, never()).deleteById(any());
    }

    @Test
    void getSponsors_mapeaCadaEntidadDeLaPaginaASuDto() {
        SponsorEntity entity = new SponsorEntity("Coca-Cola", "https://coca-cola.com");
        SponsorDTO dto = new SponsorDTO(1L, "Coca-Cola", "https://coca-cola.com", null);
        Pageable pageable = PageRequest.of(0, 20);
        when(sponsorRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(sponsorMapper.toDto(entity)).thenReturn(dto);

        Page<SponsorDTO> result = sponsorService.getSponsors(pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }
}
