package com.indorcallejero.api.facebookvideo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacebookVideoServiceTest {

    @Mock
    private FacebookVideoRepository facebookVideoRepository;

    @Mock
    private FacebookVideoMapper facebookVideoMapper;

    @InjectMocks
    private FacebookVideoService facebookVideoService;

    @Test
    void getFacebookVideoById_devuelveDto_cuandoExiste() {
        FacebookVideoEntity entity = new FacebookVideoEntity("Resumen fecha 1", "https://facebook.com/video/1");
        FacebookVideoDTO dto = new FacebookVideoDTO(1L, "Resumen fecha 1", "https://facebook.com/video/1");
        when(facebookVideoRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(facebookVideoMapper.toDto(entity)).thenReturn(dto);

        assertThat(facebookVideoService.getFacebookVideoById(1L)).isEqualTo(dto);
    }

    @Test
    void getFacebookVideoById_lanzaFacebookVideoNotFoundException_cuandoNoExiste() {
        when(facebookVideoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facebookVideoService.getFacebookVideoById(999L))
                .isInstanceOf(FacebookVideoNotFoundException.class);
    }

    @Test
    void createFacebookVideo_guardaConLosDatosDelRequest() {
        CreateFacebookVideoRequest request = new CreateFacebookVideoRequest("Resumen fecha 1", "https://facebook.com/video/1");
        FacebookVideoDTO expectedDto = new FacebookVideoDTO(1L, "Resumen fecha 1", "https://facebook.com/video/1");
        when(facebookVideoRepository.save(any(FacebookVideoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(facebookVideoMapper.toDto(any(FacebookVideoEntity.class))).thenReturn(expectedDto);

        FacebookVideoDTO result = facebookVideoService.createFacebookVideo(request);

        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void updateFacebookVideo_mutaLaEntidadYGuarda() {
        FacebookVideoEntity entity = new FacebookVideoEntity("Titulo Viejo", "https://facebook.com/video/viejo");
        UpdateFacebookVideoRequest request = new UpdateFacebookVideoRequest("Titulo Nuevo", "https://facebook.com/video/nuevo");
        when(facebookVideoRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(facebookVideoRepository.save(entity)).thenReturn(entity);
        when(facebookVideoMapper.toDto(entity)).thenReturn(
                new FacebookVideoDTO(1L, "Titulo Nuevo", "https://facebook.com/video/nuevo"));

        facebookVideoService.updateFacebookVideo(1L, request);

        assertThat(entity.getTitle()).isEqualTo("Titulo Nuevo");
        assertThat(entity.getVideoUrl()).isEqualTo("https://facebook.com/video/nuevo");
    }

    @Test
    void deleteFacebookVideo_borra_cuandoExiste() {
        when(facebookVideoRepository.existsById(1L)).thenReturn(true);

        facebookVideoService.deleteFacebookVideo(1L);

        verify(facebookVideoRepository).deleteById(1L);
    }

    @Test
    void deleteFacebookVideo_lanzaFacebookVideoNotFoundException_sinBorrar_cuandoNoExiste() {
        when(facebookVideoRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> facebookVideoService.deleteFacebookVideo(999L))
                .isInstanceOf(FacebookVideoNotFoundException.class);

        verify(facebookVideoRepository, never()).deleteById(any());
    }

    @Test
    void getFacebookVideos_mapeaCadaEntidadDeLaPaginaASuDto() {
        FacebookVideoEntity entity = new FacebookVideoEntity("Resumen fecha 1", "https://facebook.com/video/1");
        FacebookVideoDTO dto = new FacebookVideoDTO(1L, "Resumen fecha 1", "https://facebook.com/video/1");
        Pageable pageable = PageRequest.of(0, 20);
        when(facebookVideoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(facebookVideoMapper.toDto(entity)).thenReturn(dto);

        Page<FacebookVideoDTO> result = facebookVideoService.getFacebookVideos(pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }
}
