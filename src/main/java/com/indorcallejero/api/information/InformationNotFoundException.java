package com.indorcallejero.api.information;

import com.indorcallejero.api.error.NotFoundException;

public class InformationNotFoundException extends NotFoundException {

    public InformationNotFoundException(Long id) {
        super("Información no encontrada: id=" + id);
    }
}
