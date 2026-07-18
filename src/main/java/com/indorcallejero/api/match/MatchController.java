package com.indorcallejero.api.match;

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

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public Page<MatchDTO> getMatches(Pageable pageable) {
        return matchService.getMatches(pageable);
    }

    @GetMapping("/{id}")
    public MatchDTO getMatchById(@PathVariable Long id) {
        return matchService.getMatchById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    public ResponseEntity<MatchDTO> createMatch(@Valid @RequestBody CreateMatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchService.createMatch(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/{id}/start")
    public MatchDTO startMatch(@PathVariable Long id) {
        return matchService.startMatch(id);
    }

    // El endpoint más caliente del sistema según el propio audit (§2, la
    // cadena de saturación bajo carga concurrente se traza justo sobre esta
    // ruta) -- responde apenas guarda el resultado, sin esperar el
    // recálculo de standings de ninguno de los dos equipos.
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/{id}/result")
    public MatchDTO recordResult(@PathVariable Long id, @Valid @RequestBody RecordResultRequest request) {
        return matchService.recordResult(id, request);
    }
}
