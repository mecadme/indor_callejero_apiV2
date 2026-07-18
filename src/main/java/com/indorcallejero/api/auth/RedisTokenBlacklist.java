package com.indorcallejero.api.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class RedisTokenBlacklist implements TokenBlacklist {

    private static final String KEY_PREFIX = "blacklist:jti:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklist(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            // Ya expiró solo -- blacklistearlo no cambia nada, y un TTL
            // negativo/cero en Redis se comporta de forma inconsistente
            // según el comando.
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "1", ttl);
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
    }
}
