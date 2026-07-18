package com.indorcallejero.api.team;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMapper teamMapper;

    @InjectMocks
    private TeamService teamService;

    @Test
    void getTeamById_devuelveDto_cuandoExiste() {
        TeamEntity entity = new TeamEntity("Los Tigres", "Naranja", "Centro", TeamGroup.A1);
        TeamDTO dto = new TeamDTO(1L, "Los Tigres", "Naranja", "Centro", null, TeamGroup.A1);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(teamMapper.toDto(entity)).thenReturn(dto);

        assertThat(teamService.getTeamById(1L)).isEqualTo(dto);
    }

    @Test
    void getTeamById_lanzaTeamNotFoundException_cuandoNoExiste() {
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamById(999L))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    void createTeam_guardaConLosDatosDelRequest() {
        CreateTeamRequest request = new CreateTeamRequest("Los Tigres", "Naranja", "Centro", TeamGroup.A1);
        TeamDTO expectedDto = new TeamDTO(1L, "Los Tigres", "Naranja", "Centro", null, TeamGroup.A1);
        when(teamRepository.save(org.mockito.ArgumentMatchers.any(TeamEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMapper.toDto(org.mockito.ArgumentMatchers.any(TeamEntity.class))).thenReturn(expectedDto);

        TeamDTO result = teamService.createTeam(request);

        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void updateTeam_mutaLaEntidadYGuarda() {
        TeamEntity entity = new TeamEntity("Nombre Viejo", "Rojo", "Centro", TeamGroup.A1);
        UpdateTeamRequest request = new UpdateTeamRequest("Nombre Nuevo", "Azul", "Norte", "logo.png", TeamGroup.B2);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(teamRepository.save(entity)).thenReturn(entity);
        when(teamMapper.toDto(entity)).thenReturn(
                new TeamDTO(1L, "Nombre Nuevo", "Azul", "Norte", "logo.png", TeamGroup.B2));

        teamService.updateTeam(1L, request);

        assertThat(entity.getName()).isEqualTo("Nombre Nuevo");
        assertThat(entity.getColor()).isEqualTo("Azul");
        assertThat(entity.getNeighborhood()).isEqualTo("Norte");
        assertThat(entity.getGroup()).isEqualTo(TeamGroup.B2);
    }

    @Test
    void deleteTeam_borra_cuandoExiste() {
        when(teamRepository.existsById(1L)).thenReturn(true);

        teamService.deleteTeam(1L);

        verify(teamRepository).deleteById(1L);
    }

    @Test
    void deleteTeam_lanzaTeamNotFoundException_sinBorrar_cuandoNoExiste() {
        when(teamRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> teamService.deleteTeam(999L))
                .isInstanceOf(TeamNotFoundException.class);

        verify(teamRepository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getTeams_mapeaCadaEntidadDeLaPaginaASuDto() {
        TeamEntity entity = new TeamEntity("Los Tigres", "Naranja", "Centro", TeamGroup.A1);
        TeamDTO dto = new TeamDTO(1L, "Los Tigres", "Naranja", "Centro", null, TeamGroup.A1);
        Pageable pageable = PageRequest.of(0, 20);
        when(teamRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(teamMapper.toDto(entity)).thenReturn(dto);

        Page<TeamDTO> result = teamService.getTeams(pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }
}
