package com.indorcallejero.api.team;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public Page<TeamDTO> getTeams(Pageable pageable) {
        return teamService.getTeams(pageable);
    }

    @GetMapping("/{id}")
    public TeamDTO getTeamById(@PathVariable Long id) {
        return teamService.getTeamById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public TeamDTO updateTeam(@PathVariable Long id, @Valid @RequestBody UpdateTeamRequest request) {
        return teamService.updateTeam(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeamDTO updateLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return teamService.updateLogo(id, file);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }
}
