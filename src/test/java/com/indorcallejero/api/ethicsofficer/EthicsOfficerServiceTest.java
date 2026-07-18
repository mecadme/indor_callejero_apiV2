package com.indorcallejero.api.ethicsofficer;

import com.indorcallejero.api.storage.StorageService;
import com.indorcallejero.api.team.TeamEntity;
import com.indorcallejero.api.team.TeamGroup;
import com.indorcallejero.api.team.TeamNotFoundException;
import com.indorcallejero.api.team.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EthicsOfficerServiceTest {

    @Mock
    private EthicsOfficerRepository ethicsOfficerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private EthicsOfficerMapper ethicsOfficerMapper;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private EthicsOfficerService ethicsOfficerService;

    @Test
    void getEthicsOfficerById_lanzaEthicsOfficerNotFoundException_cuandoNoExiste() {
        when(ethicsOfficerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ethicsOfficerService.getEthicsOfficerById(999L))
                .isInstanceOf(EthicsOfficerNotFoundException.class);
    }

    @Test
    void createEthicsOfficer_guardaConLosDatosDelRequest() {
        CreateEthicsOfficerRequest request = new CreateEthicsOfficerRequest("Ana", "Gómez");
        EthicsOfficerDTO expectedDto = new EthicsOfficerDTO(1L, "Ana", "Gómez", null, null, null);
        when(ethicsOfficerRepository.save(any(EthicsOfficerEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ethicsOfficerMapper.toDto(any(EthicsOfficerEntity.class))).thenReturn(expectedDto);

        EthicsOfficerDTO result = ethicsOfficerService.createEthicsOfficer(request);

        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void assignTeam_asignaElEquipo_cuandoExiste() {
        EthicsOfficerEntity officer = new EthicsOfficerEntity("Ana", "Gómez");
        TeamEntity team = new TeamEntity("Los Tigres", "Naranja", "Centro", TeamGroup.A1);
        when(ethicsOfficerRepository.findById(1L)).thenReturn(Optional.of(officer));
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team));
        when(ethicsOfficerRepository.save(officer)).thenReturn(officer);
        when(ethicsOfficerMapper.toDto(officer)).thenReturn(
                new EthicsOfficerDTO(1L, "Ana", "Gómez", null, 5L, "Los Tigres"));

        ethicsOfficerService.assignTeam(1L, new AssignTeamRequest(5L));

        assertThat(officer.getTeam()).isEqualTo(team);
    }

    @Test
    void assignTeam_lanzaTeamNotFoundException_cuandoElEquipoNoExiste() {
        EthicsOfficerEntity officer = new EthicsOfficerEntity("Ana", "Gómez");
        when(ethicsOfficerRepository.findById(1L)).thenReturn(Optional.of(officer));
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ethicsOfficerService.assignTeam(1L, new AssignTeamRequest(999L)))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    void assignTeam_desasignaElEquipo_cuandoTeamIdEsNull() {
        EthicsOfficerEntity officer = new EthicsOfficerEntity("Ana", "Gómez");
        officer.setTeam(new TeamEntity("Los Tigres", "Naranja", "Centro", TeamGroup.A1));
        when(ethicsOfficerRepository.findById(1L)).thenReturn(Optional.of(officer));
        when(ethicsOfficerRepository.save(officer)).thenReturn(officer);
        when(ethicsOfficerMapper.toDto(officer)).thenReturn(
                new EthicsOfficerDTO(1L, "Ana", "Gómez", null, null, null));

        ethicsOfficerService.assignTeam(1L, new AssignTeamRequest(null));

        assertThat(officer.getTeam()).isNull();
    }

    @Test
    void updatePhoto_guardaElArchivoYActualizaPhotoUrl() {
        EthicsOfficerEntity officer = new EthicsOfficerEntity("Ana", "Gómez");
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});
        when(ethicsOfficerRepository.findById(1L)).thenReturn(Optional.of(officer));
        when(storageService.store(eq(file), eq("ethics-officers"))).thenReturn("ethics-officers/abc123.png");
        when(ethicsOfficerRepository.save(officer)).thenReturn(officer);
        when(ethicsOfficerMapper.toDto(officer)).thenReturn(
                new EthicsOfficerDTO(1L, "Ana", "Gómez", "/api/files/ethics-officers/abc123.png", null, null));

        ethicsOfficerService.updatePhoto(1L, file);

        assertThat(officer.getPhotoUrl()).isEqualTo("/api/files/ethics-officers/abc123.png");
    }

    @Test
    void deleteEthicsOfficer_lanzaEthicsOfficerNotFoundException_sinBorrar_cuandoNoExiste() {
        when(ethicsOfficerRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> ethicsOfficerService.deleteEthicsOfficer(999L))
                .isInstanceOf(EthicsOfficerNotFoundException.class);

        verify(ethicsOfficerRepository, never()).deleteById(any());
    }
}
