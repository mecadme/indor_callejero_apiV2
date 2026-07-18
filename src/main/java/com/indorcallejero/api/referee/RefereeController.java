package com.indorcallejero.api.referee;

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
@RequestMapping("/api/referees")
public class RefereeController {

    private final RefereeService refereeService;

    public RefereeController(RefereeService refereeService) {
        this.refereeService = refereeService;
    }

    @GetMapping
    public Page<RefereeDTO> getReferees(Pageable pageable) {
        return refereeService.getReferees(pageable);
    }

    @GetMapping("/{id}")
    public RefereeDTO getRefereeById(@PathVariable Long id) {
        return refereeService.getRefereeById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<RefereeDTO> createReferee(@Valid @RequestBody CreateRefereeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(refereeService.createReferee(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public RefereeDTO updateReferee(@PathVariable Long id, @Valid @RequestBody UpdateRefereeRequest request) {
        return refereeService.updateReferee(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RefereeDTO updatePhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return refereeService.updatePhoto(id, file);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReferee(@PathVariable Long id) {
        refereeService.deleteReferee(id);
        return ResponseEntity.noContent().build();
    }
}
