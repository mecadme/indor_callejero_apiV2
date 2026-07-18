package com.indorcallejero.api.referee;

import com.indorcallejero.api.error.NotFoundException;

public class RefereeNotFoundException extends NotFoundException {

    public RefereeNotFoundException(Long id) {
        super("Árbitro no encontrado: id=" + id);
    }
}
