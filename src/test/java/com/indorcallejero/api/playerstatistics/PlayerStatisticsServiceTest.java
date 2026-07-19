package com.indorcallejero.api.playerstatistics;

import com.indorcallejero.api.match.InvalidMatchStateException;
import com.indorcallejero.api.match.MatchEntity;
import com.indorcallejero.api.match.MatchNotFoundException;
import com.indorcallejero.api.match.MatchRepository;
import com.indorcallejero.api.player.PlayerEntity;
import com.indorcallejero.api.player.PlayerNotFoundException;
import com.indorcallejero.api.player.PlayerPosition;
import com.indorcallejero.api.player.PlayerRepository;
import com.indorcallejero.api.team.TeamEntity;
import com.indorcallejero.api.team.TeamGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatisticsServiceTest {

    @Mock
    private PlayerMatchEventRepository eventRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMatchEventMapper eventMapper;

    @InjectMocks
    private PlayerStatisticsService playerStatisticsService;

    @Test
    void recordEvents_lanzaMatchNotFoundException_siElPartidoNoExiste() {
        RecordPlayerEventsRequest request = new RecordPlayerEventsRequest(
                List.of(new RecordPlayerEventRequest(1L, StatType.GOAL, 10)));
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerStatisticsService.recordEvents(999L, request))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void recordEvents_lanzaInvalidMatchStateException_siElPartidoNoEstaEnCurso() {
        MatchEntity match = newScheduledMatch();
        RecordPlayerEventsRequest request = new RecordPlayerEventsRequest(
                List.of(new RecordPlayerEventRequest(1L, StatType.GOAL, 10)));
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> playerStatisticsService.recordEvents(1L, request))
                .isInstanceOf(InvalidMatchStateException.class);
    }

    @Test
    void recordEvents_lanzaPlayerNotFoundException_siElJugadorNoExiste() {
        MatchEntity match = newScheduledMatch();
        match.start();
        RecordPlayerEventsRequest request = new RecordPlayerEventsRequest(
                List.of(new RecordPlayerEventRequest(404L, StatType.GOAL, 10)));
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerStatisticsService.recordEvents(1L, request))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void recordEvents_guardaUnEventoPorCadaEntradaDelLote() {
        MatchEntity match = newScheduledMatch();
        match.start();
        PlayerEntity scorer = new PlayerEntity("Leo", "Diaz", 10, PlayerPosition.ATTACKER, 25, 1.75f);
        PlayerEntity assister = new PlayerEntity("Juan", "Perez", 7, PlayerPosition.MIDFIELDER, 27, 1.70f);
        RecordPlayerEventsRequest request = new RecordPlayerEventsRequest(List.of(
                new RecordPlayerEventRequest(1L, StatType.GOAL, 34),
                new RecordPlayerEventRequest(2L, StatType.ASSIST, 34)
        ));
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(scorer));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(assister));
        when(eventRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toDto(org.mockito.ArgumentMatchers.any(PlayerMatchEventEntity.class)))
                .thenAnswer(inv -> {
                    PlayerMatchEventEntity e = inv.getArgument(0);
                    return new PlayerMatchEventDTO(null, 1L, e.getPlayer().getId(), e.getPlayer().getFirstName(),
                            e.getPlayer().getLastName(), e.getStatType(), e.getMinute(), Instant.now());
                });

        List<PlayerMatchEventDTO> result = playerStatisticsService.recordEvents(1L, request);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).statType()).isEqualTo(StatType.GOAL);
        assertThat(result.get(1).statType()).isEqualTo(StatType.ASSIST);

        ArgumentCaptor<List<PlayerMatchEventEntity>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(eventRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(PlayerMatchEventEntity::getMinute).containsExactly(34, 34);
    }

    @Test
    void getStatsByPlayer_delegaEnLaConsultaDeAgregacion() {
        List<PlayerStatCountDTO> expected = List.of(new PlayerStatCountDTO(1L, "Leo", "Diaz", StatType.GOAL, 3L));
        when(eventRepository.countByPlayerGroupedByStatType(1L)).thenReturn(expected);

        assertThat(playerStatisticsService.getStatsByPlayer(1L)).isEqualTo(expected);
    }

    @Test
    void getStatsByMatch_delegaEnLaConsultaDeAgregacion() {
        List<PlayerStatCountDTO> expected = List.of(new PlayerStatCountDTO(1L, "Leo", "Diaz", StatType.GOAL, 1L));
        when(eventRepository.countByMatchGroupedByPlayerAndStatType(1L)).thenReturn(expected);

        assertThat(playerStatisticsService.getStatsByMatch(1L)).isEqualTo(expected);
    }

    private MatchEntity newScheduledMatch() {
        TeamEntity home = new TeamEntity("Los Tigres", null, null, TeamGroup.A1);
        TeamEntity away = new TeamEntity("Los Leones", null, null, TeamGroup.A1);
        return new MatchEntity(home, away, Instant.now().plus(1, ChronoUnit.DAYS));
    }
}
