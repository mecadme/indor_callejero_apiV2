package com.indorcallejero.api.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class LocalFileStorageService implements StorageService {

    private final FileContentValidator validator;
    private final Path basePath;

    public LocalFileStorageService(
            FileContentValidator validator,
            @Value("${storage.local.base-path}") String basePath
    ) {
        this.validator = validator;
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String directory) {
        // El nombre del archivo en disco nunca sale del nombre que manda el
        // cliente -- ni el nombre (evita path traversal / colisiones) ni la
        // extensión (evita disfrazar un tipo no permitido como otro). UUID +
        // la extensión que corresponde al tipo REAL detectado.
        String extension = validator.validateAndGetExtension(file);
        String filename = UUID.randomUUID() + extension;

        Path targetDir = basePath.resolve(directory).normalize();
        Path target = targetDir.resolve(filename);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }

        return directory + "/" + filename;
    }

    @Override
    public Resource load(String key) {
        // La clave puede venir de un @PathVariable (FileController) -- a
        // diferencia del filename que generamos nosotros en store(), acá SÍ
        // hay que asumir que puede ser hostil ("../../.env"). normalize() +
        // startsWith es el chequeo que evita escapar de basePath.
        Path resolved = basePath.resolve(key).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new InvalidFileException("Clave de archivo inválida");
        }

        Resource resource = new FileSystemResource(resolved);
        if (!resource.exists()) {
            throw new FileNotFoundInStorageException(key);
        }
        return resource;
    }
}
