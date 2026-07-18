package com.indorcallejero.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ventana fija (no ventana deslizante): simple, sin locking manual gracias a
 * ConcurrentHashMap.compute, suficiente para frenar fuerza bruta básica.
 * Trade-off consciente: en el borde exacto entre dos ventanas se puede
 * colar hasta el doble del máximo. No es el algoritmo más preciso que
 * existe, es el más simple que resuelve el problema real de SEC-06.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private record Window(Instant start, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration windowDuration;

    public InMemoryRateLimiter(
            @Value("${security.rate-limit.max-attempts}") int maxAttempts,
            @Value("${security.rate-limit.window-seconds}") long windowSeconds
    ) {
        this.maxAttempts = maxAttempts;
        this.windowDuration = Duration.ofSeconds(windowSeconds);
    }

    @Override
    public void checkAllowed(String key) {
        Instant now = Instant.now();
        Window updated = windows.compute(key, (k, current) -> {
            if (current == null || current.start().plus(windowDuration).isBefore(now)) {
                return new Window(now, 1);
            }
            return new Window(current.start(), current.count() + 1);
        });
        if (updated.count() > maxAttempts) {
            throw new TooManyAttemptsException(key);
        }
    }
}
