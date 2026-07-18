package com.indorcallejero.api.match;

import com.indorcallejero.api.team.TeamEntity;
import com.indorcallejero.api.team.TeamGroup;
import com.indorcallejero.api.team.TeamNotFoundException;
import com.indorcallejero.api.team.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MatchService matchService;

    @Test
    void createMatch_lanzaTeamNotFoundException_siElEquipoLocalNoExiste() {
        CreateMatchRequest request = new CreateMatchRequest(1L, 2L, Instant.now().plus(1, ChronoUnit.DAYS));
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.createMatch(request))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    void startMatch_lanzaInvalidMatchStateException_siYaEstaEnCurso() {
        TeamEntity home = new TeamEntity("Los Tigres", null, null, TeamGroup.A1);
        TeamEntity away = new TeamEntity("Los Leones", null, null, TeamGroup.A1);
        MatchEntity match = new MatchEntity(home, away, Instant.now().plus(1, ChronoUnit.DAYS));
        match.start();
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.startMatch(1L))
                .isInstanceOf(InvalidMatchStateException.class);
    }

    @Test
    void recordResult_lanzaInvalidMatchStateException_siElPartidoNoArranco() {
        TeamEntity home = new TeamEntity("Los Tigres", null, null, TeamGroup.A1);
        TeamEntity away = new TeamEntity("Los Leones", null, null, TeamGroup.A1);
        MatchEntity match = new MatchEntity(home, away, Instant.now().plus(1, ChronoUnit.DAYS));
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.recordResult(1L, new RecordResultRequest(2, 1)))
                .isInstanceOf(InvalidMatchStateException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void recordResult_publicaElEventoConLosGolesYLosEquiposCorrectos() {
        TeamEntity home = teamWithId(10L, "Los Tigres");
        TeamEntity away = teamWithId(20L, "Los Leones");
        MatchEntity match = new MatchEntity(home, away, Instant.now().plus(1, ChronoUnit.DAYS));
        match.start();
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchRepository.save(match)).thenReturn(match);

        matchService.recordResult(1L, new RecordResultRequest(2, 1));

        ArgumentCaptor<MatchResultRecordedEvent> captor = ArgumentCaptor.forClass(MatchResultRecordedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        MatchResultRecordedEvent event = captor.getValue();
        assertThat(event.homeTeamId()).isEqualTo(10L);
        assertThat(event.awayTeamId()).isEqualTo(20L);
        assertThat(event.goalsHomeTeam()).isEqualTo(2);
        assertThat(event.goalsAwayTeam()).isEqualTo(1);
    }

    // Refleja un ID en un TeamEntity recién creado (normalmente lo pone
    // JPA al persistir) -- necesario acá porque el evento lleva homeTeamId/
    // awayTeamId y en un test unitario no hay base de datos que lo asigne.
    private TeamEntity teamWithId(Long id, String name) {
        TeamEntity team = new TeamEntity(name, null, null, TeamGroup.A1);
        try {
            var field = TeamEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(team, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return team;
    }
}
