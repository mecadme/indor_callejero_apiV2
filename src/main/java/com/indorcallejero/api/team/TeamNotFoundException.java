package com.indorcallejero.api.team;

import com.indorcallejero.api.error.NotFoundException;

public class TeamNotFoundException extends NotFoundException {

    public TeamNotFoundException(Long id) {
        super("Equipo no encontrado: id=" + id);
    }
}
