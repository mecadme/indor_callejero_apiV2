package com.indorcallejero.api.storage;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;

/**
 * SEC-09 del audit: PhotoUtils del original tomaba la extensión del
 * nombre de archivo que manda el cliente, verbatim, sin mirar el
 * contenido -- subir "logo.jpg" que en realidad son los primeros bytes
 * de un ejecutable pasaba sin problema. Acá se valida el tipo REAL,
 * leyendo los magic bytes del archivo con Tika, sin importar qué
 * extensión o Content-Type haya declarado el cliente.
 */
@Component
public class FileContentValidator {

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final Tika tika = new Tika();

    /**
     * @return la extensión de archivo correcta para el tipo REAL detectado
     * (no la que mandó el cliente).
     */
    public String validateAndGetExtension(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("El archivo está vacío");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidFileException("El archivo supera el tamaño máximo permitido (5MB)");
        }

        String detectedType;
        try {
            detectedType = tika.detect(file.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo para validar su contenido", e);
        }

        String extension = ALLOWED_TYPES.get(detectedType);
        if (extension == null) {
            throw new InvalidFileException(
                    "Tipo de archivo no permitido: " + detectedType
                            + " (detectado por el contenido real, no por el nombre). Permitidos: "
                            + Set.of("JPEG", "PNG", "WEBP"));
        }
        return extension;
    }
}
