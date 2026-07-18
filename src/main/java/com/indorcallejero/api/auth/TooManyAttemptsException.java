package com.indorcallejero.api.auth;

public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException(String key) {
        super("Demasiados intentos para: " + key);
    }
}
