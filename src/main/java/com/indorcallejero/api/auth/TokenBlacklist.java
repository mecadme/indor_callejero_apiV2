package com.indorcallejero.api.auth;

/**
 * Contrato de revocación de tokens. Hoy la única implementación es en
 * memoria (ver InMemoryTokenBlacklist) -- SEC-04 del audit. En la Etapa 10
 * agregamos una implementación con Redis, compartida entre instancias, sin
 * tocar ningún código que dependa de esta interfaz.
 */
public interface TokenBlacklist {

    void blacklist(String tokenId);

    boolean isBlacklisted(String tokenId);
}
