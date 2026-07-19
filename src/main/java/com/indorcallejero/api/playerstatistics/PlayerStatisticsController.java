package com.indorcallejero.api.playerstatistics;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/player-statistics")
public class PlayerStatisticsController {

    private final PlayerStatisticsService playerStatisticsService;

    public PlayerStatisticsController(PlayerStatisticsService playerStatisticsService) {
        this.playerStatisticsService = playerStatisticsService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/matches/{matchId}/events")
    public ResponseEntity<List<PlayerMatchEventDTO>> recordEvents(
            @PathVariable Long matchId, @Valid @RequestBody RecordPlayerEventsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerStatisticsService.recordEvents(matchId, request));
    }

    @GetMapping("/matches/{matchId}/events")
    public Page<PlayerMatchEventDTO> getEventsByMatch(@PathVariable Long matchId, Pageable pageable) {
        return playerStatisticsService.getEventsByMatch(matchId, pageable);
    }

    @GetMapping("/matches/{matchId}")
    public List<PlayerStatCountDTO> getStatsByMatch(@PathVariable Long matchId) {
        return playerStatisticsService.getStatsByMatch(matchId);
    }

    @GetMapping("/players/{playerId}")
    public List<PlayerStatCountDTO> getStatsByPlayer(@PathVariable Long playerId) {
        return playerStatisticsService.getStatsByPlayer(playerId);
    }
}
