package com.indorcallejero.api.ethicsofficer;

import com.indorcallejero.api.error.NotFoundException;

public class EthicsOfficerNotFoundException extends NotFoundException {

    public EthicsOfficerNotFoundException(Long id) {
        super("Oficial de ética no encontrado: id=" + id);
    }
}
