package com.indorcallejero.api.player;

import com.indorcallejero.api.error.NotFoundException;

public class PlayerNotFoundException extends NotFoundException {

    public PlayerNotFoundException(Long id) {
        super("Jugador no encontrado: id=" + id);
    }
}
