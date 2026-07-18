package com.indorcallejero.api.error;

// UserNotFoundException, TeamNotFoundException y ahora PlayerNotFoundException
// eran tres clases idénticas salvo el nombre -- tercera repetición, momento
// de extraer la base compartida en vez de seguir copiando el patrón.
// GlobalExceptionHandler mapea este tipo una sola vez a 404.
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
