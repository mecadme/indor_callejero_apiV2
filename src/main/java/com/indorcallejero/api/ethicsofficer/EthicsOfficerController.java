package com.indorcallejero.api.ethicsofficer;

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
@RequestMapping("/api/ethics-officers")
public class EthicsOfficerController {

    private final EthicsOfficerService ethicsOfficerService;

    public EthicsOfficerController(EthicsOfficerService ethicsOfficerService) {
        this.ethicsOfficerService = ethicsOfficerService;
    }

    @GetMapping
    public Page<EthicsOfficerDTO> getEthicsOfficers(Pageable pageable) {
        return ethicsOfficerService.getEthicsOfficers(pageable);
    }

    @GetMapping("/by-team/{teamId}")
    public Page<EthicsOfficerDTO> getEthicsOfficersByTeam(@PathVariable Long teamId, Pageable pageable) {
        return ethicsOfficerService.getEthicsOfficersByTeam(teamId, pageable);
    }

    @GetMapping("/{id}")
    public EthicsOfficerDTO getEthicsOfficerById(@PathVariable Long id) {
        return ethicsOfficerService.getEthicsOfficerById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EthicsOfficerDTO> createEthicsOfficer(@Valid @RequestBody CreateEthicsOfficerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ethicsOfficerService.createEthicsOfficer(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public EthicsOfficerDTO updateEthicsOfficer(@PathVariable Long id, @Valid @RequestBody UpdateEthicsOfficerRequest request) {
        return ethicsOfficerService.updateEthicsOfficer(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EthicsOfficerDTO updatePhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ethicsOfficerService.updatePhoto(id, file);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/team")
    public EthicsOfficerDTO assignTeam(@PathVariable Long id, @Valid @RequestBody AssignTeamRequest request) {
        return ethicsOfficerService.assignTeam(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEthicsOfficer(@PathVariable Long id) {
        ethicsOfficerService.deleteEthicsOfficer(id);
        return ResponseEntity.noContent().build();
    }
}
