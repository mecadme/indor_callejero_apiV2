package com.indorcallejero.api.standing;

import com.indorcallejero.api.team.TeamEntity;
import com.indorcallejero.api.team.TeamGroup;
import com.indorcallejero.api.team.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandingServiceTest {

    @Mock
    private StandingRepository standingRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private StandingMapper standingMapper;

    @InjectMocks
    private StandingService standingService;

    @Test
    void recordMatchResult_creaElStanding_cuandoEsElPrimerPartidoDelEquipo() {
        TeamEntity team = new TeamEntity("Los Tigres", null, null, TeamGroup.A1);
        when(standingRepository.findByTeamId(1L)).thenReturn(Optional.empty());
        when(teamRepository.getReferenceById(1L)).thenReturn(team);
        when(standingRepository.save(any(StandingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        standingService.recordMatchResult(1L, 3, 1);

        ArgumentCaptor<StandingEntity> captor = ArgumentCaptor.forClass(StandingEntity.class);
        verify(standingRepository).save(captor.capture());
        StandingEntity saved = captor.getValue();
        assertThat(saved.getGamesPlayed()).isEqualTo(1);
        assertThat(saved.getWins()).isEqualTo(1);
        assertThat(saved.getPoints()).isEqualTo(3);
        assertThat(saved.getGoalsFor()).isEqualTo(3);
        assertThat(saved.getGoalsAgainst()).isEqualTo(1);
    }

    @Test
    void recordMatchResult_acumulaSobreElStandingExistente() {
        TeamEntity team = new TeamEntity("Los Tigres", null, null, TeamGroup.A1);
        StandingEntity existing = new StandingEntity(team);
        existing.recordMatch(2, 0);
        when(standingRepository.findByTeamId(1L)).thenReturn(Optional.of(existing));
        when(standingRepository.save(existing)).thenReturn(existing);

        standingService.recordMatchResult(1L, 1, 1);

        assertThat(existing.getGamesPlayed()).isEqualTo(2);
        assertThat(existing.getWins()).isEqualTo(1);
        assertThat(existing.getDraws()).isEqualTo(1);
        assertThat(existing.getPoints()).isEqualTo(4);
        assertThat(existing.getGoalsFor()).isEqualTo(3);
        assertThat(existing.getGoalsAgainst()).isEqualTo(1);
    }

    @Test
    void recordMatch_sumaDerrota_sinPuntos_cuandoPierde() {
        TeamEntity team = new TeamEntity("Los Tigres", null, null, TeamGroup.A1);
        StandingEntity standing = new StandingEntity(team);

        standing.recordMatch(0, 2);

        assertThat(standing.getLosses()).isEqualTo(1);
        assertThat(standing.getPoints()).isZero();
    }
}
