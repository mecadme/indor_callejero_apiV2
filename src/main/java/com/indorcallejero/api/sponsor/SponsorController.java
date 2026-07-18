package com.indorcallejero.api.sponsor;

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
@RequestMapping("/api/sponsors")
public class SponsorController {

    private final SponsorService sponsorService;

    public SponsorController(SponsorService sponsorService) {
        this.sponsorService = sponsorService;
    }

    @GetMapping
    public Page<SponsorDTO> getSponsors(Pageable pageable) {
        return sponsorService.getSponsors(pageable);
    }

    @GetMapping("/{id}")
    public SponsorDTO getSponsorById(@PathVariable Long id) {
        return sponsorService.getSponsorById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SponsorDTO> createSponsor(@Valid @RequestBody CreateSponsorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sponsorService.createSponsor(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public SponsorDTO updateSponsor(@PathVariable Long id, @Valid @RequestBody UpdateSponsorRequest request) {
        return sponsorService.updateSponsor(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SponsorDTO updatePhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return sponsorService.updatePhoto(id, file);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSponsor(@PathVariable Long id) {
        sponsorService.deleteSponsor(id);
        return ResponseEntity.noContent().build();
    }
}
