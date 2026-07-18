package com.indorcallejero.api.round;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoundMapper roundMapper;

    @InjectMocks
    private RoundService roundService;

    @Test
    void getRoundById_devuelveDto_cuandoExiste() {
        RoundEntity entity = new RoundEntity("Fecha 1", 1);
        RoundDTO dto = new RoundDTO(1L, "Fecha 1", 1);
        when(roundRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(roundMapper.toDto(entity)).thenReturn(dto);

        assertThat(roundService.getRoundById(1L)).isEqualTo(dto);
    }

    @Test
    void getRoundById_lanzaRoundNotFoundException_cuandoNoExiste() {
        when(roundRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roundService.getRoundById(999L))
                .isInstanceOf(RoundNotFoundException.class);
    }

    @Test
    void createRound_guardaConLosDatosDelRequest() {
        CreateRoundRequest request = new CreateRoundRequest("Fecha 1", 1);
        RoundDTO expectedDto = new RoundDTO(1L, "Fecha 1", 1);
        when(roundRepository.save(any(RoundEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toDto(any(RoundEntity.class))).thenReturn(expectedDto);

        RoundDTO result = roundService.createRound(request);

        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void getRounds_mapeaCadaEntidadDeLaPaginaASuDto_ordenadaPorNumero() {
        RoundEntity entity = new RoundEntity("Fecha 1", 1);
        RoundDTO dto = new RoundDTO(1L, "Fecha 1", 1);
        Pageable pageable = PageRequest.of(0, 20);
        when(roundRepository.findAllByOrderByNumberAsc(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(roundMapper.toDto(entity)).thenReturn(dto);

        Page<RoundDTO> result = roundService.getRounds(pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }
}
