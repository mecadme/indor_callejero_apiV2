package com.indorcallejero.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Mismo algoritmo de ventana fija que InMemoryRateLimiter (Etapa 2), pero
 * ahora las dos instancias detrás del balanceador (Etapa 11) cuentan sobre
 * el mismo contador -- antes, alguien podía agotar el límite contra la
 * instancia A y seguir intentando tranquilo contra la B. INCR de Redis es
 * atómico entre procesos, no hace falta ningún lock de nuestro lado.
 */
@Component
public class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final int maxAttempts;
    private final Duration windowDuration;

    public RedisRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${security.rate-limit.max-attempts}") int maxAttempts,
            @Value("${security.rate-limit.window-seconds}") long windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.windowDuration = Duration.ofSeconds(windowSeconds);
    }

    @Override
    public void checkAllowed(String key) {
        String redisKey = KEY_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        // El TTL se pone solo en el primer hit de la ventana (count == 1) --
        // ponerlo en cada llamada iría corriendo la ventana para adelante
        // cada vez que alguien reintenta, dejando de ser "fija".
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, windowDuration);
        }
        if (count != null && count > maxAttempts) {
            throw new TooManyAttemptsException(key);
        }
    }
}
