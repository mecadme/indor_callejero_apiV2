package com.indorcallejero.api.match;

// Una sola clase de este tipo por ahora -- no hay todavía un tercer caso
// de "operación rechazada por el estado actual del recurso" en el
// proyecto como para justificar una base compartida (ver NotFoundException
// en error/, que sí se extrajo recién a la tercera repetición, Etapa 6).
public class InvalidMatchStateException extends RuntimeException {

    public InvalidMatchStateException(String message) {
        super(message);
    }
}
