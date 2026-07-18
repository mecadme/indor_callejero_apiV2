package com.indorcallejero.api.auth;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Set concurrente, no el HashSet sin sincronizar de la versión original
 * (SEC-04). Sigue siendo por-instancia -- dos instancias de la app no se
 * enteran una a la otra de un logout -- y sin expiración propia (un token
 * revocado queda acá para siempre, aunque el JWT ya haya expirado solo).
 * Ambas limitaciones se resuelven con la misma pieza: Redis en la Etapa 10,
 * que da TTL nativo y estado compartido gratis.
 */
@Component
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final Set<String> blacklistedTokenIds = ConcurrentHashMap.newKeySet();

    @Override
    public void blacklist(String tokenId) {
        blacklistedTokenIds.add(tokenId);
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        return blacklistedTokenIds.contains(tokenId);
    }
}
