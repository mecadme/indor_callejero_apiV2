package com.indorcallejero.api.auth;

/**
 * Limita intentos por clave (ej. "login:mecadme" o "login:203.0.113.4") en
 * una ventana de tiempo fija. SEC-06 del audit -- sin esto, /login y
 * /register no tienen ningún freno ante fuerza bruta.
 */
public interface RateLimiter {

    /**
     * @throws TooManyAttemptsException si la clave superó el máximo de
     *                                   intentos permitidos en la ventana actual.
     */
    void checkAllowed(String key);
}
