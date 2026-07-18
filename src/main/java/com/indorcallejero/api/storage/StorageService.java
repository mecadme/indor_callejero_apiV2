package com.indorcallejero.api.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * PERF-05 del audit: nada en el resto del código (TeamService, un futuro
 * SponsorService, etc.) sabe que hoy esto guarda en disco local. El día
 * que haga falta S3, se escribe una S3StorageService y se cambia el bean
 * que Spring inyecta -- ningún caller cambia una línea.
 */
public interface StorageService {

    /**
     * @param directory subcarpeta lógica (ej. "teams", "players") -- nunca
     *                   viene del cliente, la decide el caller
     * @return la clave para recuperar el archivo después (ver load)
     */
    String store(MultipartFile file, String directory);

    Resource load(String key);
}
