package com.indorcallejero.api.storage;

import com.indorcallejero.api.error.NotFoundException;

public class FileNotFoundInStorageException extends NotFoundException {

    public FileNotFoundInStorageException(String key) {
        super("Archivo no encontrado: " + key);
    }
}
