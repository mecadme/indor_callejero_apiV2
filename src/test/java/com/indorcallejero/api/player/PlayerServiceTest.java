package com.indorcallejero.api.player;

import com.indorcallejero.api.team.TeamEntity;
import com.indorcallejero.api.team.TeamGroup;
import com.indorcallejero.api.team.TeamNotFoundException;
import com.indorcallejero.api.team.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void getPlayerById_lanzaPlayerNotFoundException_cuandoNoExiste() {
        when(playerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.getPlayerById(999L))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void createPlayer_arrancaConEstadoActive() {
        CreatePlayerRequest request = new CreatePlayerRequest("Leo", "Diaz", 10, PlayerPosition.MIDFIELDER, 25, 1.75f);
        when(playerRepository.save(any(PlayerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerMapper.toDto(any(PlayerEntity.class))).thenAnswer(invocation -> {
            PlayerEntity saved = invocation.getArgument(0);
            return new PlayerDTO(1L, saved.getFirstName(), saved.getLastName(), saved.getJerseyNumber(),
                    saved.getPosition(), saved.getAge(), saved.getHeight(), saved.getStatus(), null, null, null);
        });

        PlayerDTO result = playerService.createPlayer(request);

        assertThat(result.status()).isEqualTo(PlayerStatus.ACTIVE);
        assertThat(result.firstName()).isEqualTo("Leo");
    }

    @Test
    void assignTeam_asignaElEquipo_cuandoExiste() {
        PlayerEntity player = new PlayerEntity("Leo", "Diaz", 10, PlayerPosition.MIDFIELDER, 25, 1.75f);
        TeamEntity team = new TeamEntity("Los Tigres", "Naranja", "Centro", TeamGroup.A1);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team));
        when(playerRepository.save(player)).thenReturn(player);
        when(playerMapper.toDto(player)).thenReturn(
                new PlayerDTO(1L, "Leo", "Diaz", 10, PlayerPosition.MIDFIELDER, 25, 1.75f,
                        PlayerStatus.ACTIVE, null, 5L, "Los Tigres"));

        playerService.assignTeam(1L, new AssignTeamRequest(5L));

        assertThat(player.getTeam()).isEqualTo(team);
    }

    @Test
    void assignTeam_lanzaTeamNotFoundException_cuandoElEquipoNoExiste() {
        PlayerEntity player = new PlayerEntity("Leo", "Diaz", 10, PlayerPosition.MIDFIELDER, 25, 1.75f);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.assignTeam(1L, new AssignTeamRequest(999L)))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    void assignTeam_desasignaElEquipo_cuandoTeamIdEsNull() {
        PlayerEntity player = new PlayerEntity("Leo", "Diaz", 10, PlayerPosition.MIDFIELDER, 25, 1.75f);
        player.setTeam(new TeamEntity("Los Tigres", "Naranja", "Centro", TeamGroup.A1));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerRepository.save(player)).thenReturn(player);
        when(playerMapper.toDto(player)).thenReturn(
                new PlayerDTO(1L, "Leo", "Diaz", 10, PlayerPosition.MIDFIELDER, 25, 1.75f,
                        PlayerStatus.ACTIVE, null, null, null));

        playerService.assignTeam(1L, new AssignTeamRequest(null));

        assertThat(player.getTeam()).isNull();
    }

    @Test
    void changeStatus_actualizaElEstado() {
        PlayerEntity player = new PlayerEntity("Leo", "Diaz", 10, PlayerPosition.MIDFIELDER, 25, 1.75f);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(playerRepository.save(player)).thenReturn(player);
        when(playerMapper.toDto(player)).thenReturn(
                new PlayerDTO(1L, "Leo", "Diaz", 10, PlayerPosition.MIDFIELDER, 25, 1.75f,
                        PlayerStatus.INJURED, null, null, null));

        playerService.changeStatus(1L, new ChangePlayerStatusRequest(PlayerStatus.INJURED));

        assertThat(player.getStatus()).isEqualTo(PlayerStatus.INJURED);
    }

    @Test
    void deletePlayer_lanzaPlayerNotFoundException_sinBorrar_cuandoNoExiste() {
        when(playerRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> playerService.deletePlayer(999L))
                .isInstanceOf(PlayerNotFoundException.class);

        verify(playerRepository, never()).deleteById(any());
    }
}
