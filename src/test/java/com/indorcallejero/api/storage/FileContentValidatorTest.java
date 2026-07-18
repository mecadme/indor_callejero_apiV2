package com.indorcallejero.api.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bytes reales, no un Tika mockeado -- la prueba que importa acá es que
 * Tika lee el contenido de verdad, no que "algo" devuelva lo que le
 * dijimos que devuelva.
 */
class FileContentValidatorTest {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final FileContentValidator validator = new FileContentValidator();

    @Test
    void aceptaUnPngDeVerdad_porSuContenido_sinImportarElNombre() {
        // El nombre de archivo dice "documento.txt" a propósito: la
        // validación tiene que ignorarlo por completo.
        MockMultipartFile file = new MockMultipartFile("file", "documento.txt", "text/plain", PNG_SIGNATURE);

        String extension = validator.validateAndGetExtension(file);

        assertThat(extension).isEqualTo(".png");
    }

    @Test
    void rechazaUnArchivoDeTextoDisfrazadoDeImagen_porSuContenidoReal() {
        // Justo el caso que SEC-09 del audit describe: un archivo cuyo
        // nombre y Content-Type dicen "imagen", pero el contenido real es
        // texto plano.
        byte[] textContent = "esto no es una imagen, es texto plano".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", textContent);

        assertThatThrownBy(() -> validator.validateAndGetExtension(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("no permitido");
    }

    @Test
    void rechazaUnArchivoVacio() {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> validator.validateAndGetExtension(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void rechazaUnArchivoQueSuperaElTamañoMaximo() {
        byte[] tooLarge = new byte[6 * 1024 * 1024];
        System.arraycopy(PNG_SIGNATURE, 0, tooLarge, 0, PNG_SIGNATURE.length);
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", tooLarge);

        assertThatThrownBy(() -> validator.validateAndGetExtension(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("tamaño máximo");
    }
}
