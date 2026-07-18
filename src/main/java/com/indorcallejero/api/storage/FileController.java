package com.indorcallejero.api.storage;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLConnection;

// Público (permitAll en SecurityConfig): logos e fotos de equipos/jugadores
// son contenido de marketing, no información sensible -- mismo criterio
// que swagger-ui en dev.
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{directory}/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String directory, @PathVariable String filename) {
        Resource resource = storageService.load(directory + "/" + filename);
        String contentType = URLConnection.guessContentTypeFromName(filename);
        return ResponseEntity.ok()
                .contentType(contentType != null
                        ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
