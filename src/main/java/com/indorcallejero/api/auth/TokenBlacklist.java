package com.indorcallejero.api.auth;

import java.time.Instant;

/**
 * Contrato de revocación de tokens. SEC-04 del audit. Antes de la Etapa 10
 * la única implementación era en memoria (HashSet, ni thread-safe ni
 * compartida entre instancias); ahora es Redis, y ningún caller de esta
 * interfaz cambió una línea para el swap.
 *
 * expiresAt sí es un agregado nuevo al contrato, no parte del swap en sí:
 * Redis puede expirar la entrada solo (TTL nativo) exactamente cuando el
 * JWT igual hubiera dejado de ser válido -- la blacklist en memoria de la
 * Etapa 2 no tenía forma de hacer eso, así que crecía para siempre. Achicar
 * el contrato para aprovechar algo que el backend nuevo permite es una
 * decisión distinta de "cambiar la implementación sin tocar el contrato".
 */
public interface TokenBlacklist {

    void blacklist(String tokenId, Instant expiresAt);

    boolean isBlacklisted(String tokenId);
}
