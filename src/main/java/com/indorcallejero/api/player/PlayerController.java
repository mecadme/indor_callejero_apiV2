package com.indorcallejero.api.player;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public Page<PlayerDTO> getPlayers(Pageable pageable) {
        return playerService.getPlayers(pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/without-team")
    public Page<PlayerDTO> getPlayersWithoutTeam(Pageable pageable) {
        return playerService.getPlayersWithoutTeam(pageable);
    }

    @GetMapping("/by-team/{teamId}")
    public Page<PlayerDTO> getPlayersByTeam(@PathVariable Long teamId, Pageable pageable) {
        return playerService.getPlayersByTeam(teamId, pageable);
    }

    @GetMapping("/{id}")
    public PlayerDTO getPlayerById(@PathVariable Long id) {
        return playerService.getPlayerById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PlayerDTO> createPlayer(@Valid @RequestBody CreatePlayerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerService.createPlayer(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public PlayerDTO updatePlayer(@PathVariable Long id, @Valid @RequestBody UpdatePlayerRequest request) {
        return playerService.updatePlayer(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/{id}/status")
    public PlayerDTO changeStatus(@PathVariable Long id, @Valid @RequestBody ChangePlayerStatusRequest request) {
        return playerService.changeStatus(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/team")
    public PlayerDTO assignTeam(@PathVariable Long id, @Valid @RequestBody AssignTeamRequest request) {
        return playerService.assignTeam(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
