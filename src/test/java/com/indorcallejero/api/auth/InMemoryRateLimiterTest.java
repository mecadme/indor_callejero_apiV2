package com.indorcallejero.api.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryRateLimiterTest {

    @Test
    void permiteHastaElMaximoYLuegoExplota() {
        RateLimiter limiter = new InMemoryRateLimiter(3, 60);

        assertThatCode(() -> {
            limiter.checkAllowed("clave");
            limiter.checkAllowed("clave");
            limiter.checkAllowed("clave");
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> limiter.checkAllowed("clave"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void cuentaCadaClavePorSeparado() {
        RateLimiter limiter = new InMemoryRateLimiter(1, 60);

        assertThatCode(() -> {
            limiter.checkAllowed("usuario-a");
            limiter.checkAllowed("usuario-b");
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> limiter.checkAllowed("usuario-a"))
                .isInstanceOf(TooManyAttemptsException.class);
    }
}
