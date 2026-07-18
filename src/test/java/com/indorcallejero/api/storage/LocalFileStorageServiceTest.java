package com.indorcallejero.api.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService(Path base) {
        return new LocalFileStorageService(new FileContentValidator(), base.toString());
    }

    @Test
    void guardaYRecuperaUnArchivo_deVerdadEnDisco() throws IOException {
        LocalFileStorageService service = storageService(tempDir);
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG_SIGNATURE);

        String key = service.store(file, "teams");
        Resource loaded = service.load(key);

        assertThat(key).startsWith("teams/").endsWith(".png");
        assertThat(loaded.exists()).isTrue();
        assertThat(loaded.getContentAsByteArray()).isEqualTo(PNG_SIGNATURE);
    }

    @Test
    void generaUnNombreDistinto_cadaVez_aunqueElArchivoOriginalSeaElMismo() {
        LocalFileStorageService service = storageService(tempDir);
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG_SIGNATURE);

        String key1 = service.store(file, "teams");
        String key2 = service.store(file, "teams");

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void load_lanzaFileNotFoundInStorageException_cuandoElArchivoNoExiste() {
        LocalFileStorageService service = storageService(tempDir);

        assertThatThrownBy(() -> service.load("teams/no-existe.png"))
                .isInstanceOf(FileNotFoundInStorageException.class);
    }

    @Test
    void load_rechazaUnaClaveQueIntentaEscaparDelDirectorioBase() {
        LocalFileStorageService service = storageService(tempDir);

        assertThatThrownBy(() -> service.load("../../../../etc/passwd"))
                .isInstanceOf(InvalidFileException.class);
    }
}
