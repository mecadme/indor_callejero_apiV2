package com.indorcallejero.api.player;

import com.indorcallejero.api.team.TeamEntity;
import com.indorcallejero.api.team.TeamNotFoundException;
import com.indorcallejero.api.team.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Transactional acá por la misma razón que en AuthService (Etapa 2):
 * "team" es LAZY y open-in-view está apagado, así que
 * PlayerMapper.toDto() -- que lee player.getTeam().getName() -- necesita
 * la sesión de Hibernate todavía abierta al momento de mapear. Sin esto,
 * el mismo LazyInitializationException de la Etapa 2 vuelve a aparecer,
 * ahora con "team" en vez de "roles". Reconocer el patrón de una vez, no
 * hace falta volver a chocarse con él para creerlo.
 */
@Service
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final PlayerMapper playerMapper;

    public PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository, PlayerMapper playerMapper) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.playerMapper = playerMapper;
    }

    @Transactional(readOnly = true)
    public Page<PlayerDTO> getPlayers(Pageable pageable) {
        return playerRepository.findAll(pageable).map(playerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PlayerDTO> getPlayersWithoutTeam(Pageable pageable) {
        return playerRepository.findByTeamIsNull(pageable).map(playerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PlayerDTO> getPlayersByTeam(Long teamId, Pageable pageable) {
        return playerRepository.findByTeamId(teamId, pageable).map(playerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public PlayerDTO getPlayerById(Long id) {
        return playerMapper.toDto(findOrThrow(id));
    }

    public PlayerDTO createPlayer(CreatePlayerRequest request) {
        PlayerEntity player = new PlayerEntity(
                request.firstName(), request.lastName(), request.jerseyNumber(),
                request.position(), request.age(), request.height());
        return playerMapper.toDto(playerRepository.save(player));
    }

    public PlayerDTO updatePlayer(Long id, UpdatePlayerRequest request) {
        PlayerEntity player = findOrThrow(id);
        player.setFirstName(request.firstName());
        player.setLastName(request.lastName());
        player.setJerseyNumber(request.jerseyNumber());
        player.setPosition(request.position());
        player.setAge(request.age());
        player.setHeight(request.height());
        player.setPhotoUrl(request.photoUrl());
        return playerMapper.toDto(playerRepository.save(player));
    }

    public PlayerDTO changeStatus(Long id, ChangePlayerStatusRequest request) {
        PlayerEntity player = findOrThrow(id);
        player.setStatus(request.status());
        return playerMapper.toDto(playerRepository.save(player));
    }

    public PlayerDTO assignTeam(Long id, AssignTeamRequest request) {
        PlayerEntity player = findOrThrow(id);
        if (request.teamId() == null) {
            player.setTeam(null);
        } else {
            TeamEntity team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new TeamNotFoundException(request.teamId()));
            player.setTeam(team);
        }
        return playerMapper.toDto(playerRepository.save(player));
    }

    public void deletePlayer(Long id) {
        if (!playerRepository.existsById(id)) {
            throw new PlayerNotFoundException(id);
        }
        playerRepository.deleteById(id);
    }

    private PlayerEntity findOrThrow(Long id) {
        return playerRepository.findById(id).orElseThrow(() -> new PlayerNotFoundException(id));
    }
}
