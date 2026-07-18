package com.indorcallejero.api.round;

import com.indorcallejero.api.error.NotFoundException;

public class RoundNotFoundException extends NotFoundException {

    public RoundNotFoundException(Long id) {
        super("Fecha no encontrada: id=" + id);
    }
}
