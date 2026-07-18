package com.indorcallejero.api.match;

import com.indorcallejero.api.error.NotFoundException;

public class MatchNotFoundException extends NotFoundException {

    public MatchNotFoundException(Long id) {
        super("Partido no encontrado: id=" + id);
    }
}
