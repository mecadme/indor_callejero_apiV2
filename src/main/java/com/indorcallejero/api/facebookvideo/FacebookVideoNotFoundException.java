package com.indorcallejero.api.facebookvideo;

import com.indorcallejero.api.error.NotFoundException;

public class FacebookVideoNotFoundException extends NotFoundException {

    public FacebookVideoNotFoundException(Long id) {
        super("Video no encontrado: id=" + id);
    }
}
