package com.indorcallejero.api.information;

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
@RequestMapping("/api/information")
public class InformationController {

    private final InformationService informationService;

    public InformationController(InformationService informationService) {
        this.informationService = informationService;
    }

    @GetMapping
    public Page<InformationDTO> getInformation(Pageable pageable) {
        return informationService.getInformation(pageable);
    }

    @GetMapping("/{id}")
    public InformationDTO getInformationById(@PathVariable Long id) {
        return informationService.getInformationById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<InformationDTO> createInformation(@Valid @RequestBody CreateInformationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(informationService.createInformation(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public InformationDTO updateInformation(@PathVariable Long id, @Valid @RequestBody UpdateInformationRequest request) {
        return informationService.updateInformation(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InformationDTO updatePhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return informationService.updatePhoto(id, file);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInformation(@PathVariable Long id) {
        informationService.deleteInformation(id);
        return ResponseEntity.noContent().build();
    }
}
