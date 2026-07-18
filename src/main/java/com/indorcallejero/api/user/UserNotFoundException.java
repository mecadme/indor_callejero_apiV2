package com.indorcallejero.api.user;

import com.indorcallejero.api.error.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Long id) {
        super("Usuario no encontrado: id=" + id);
    }
}
