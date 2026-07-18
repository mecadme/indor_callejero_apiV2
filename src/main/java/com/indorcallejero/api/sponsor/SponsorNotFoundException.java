package com.indorcallejero.api.sponsor;

import com.indorcallejero.api.error.NotFoundException;

public class SponsorNotFoundException extends NotFoundException {

    public SponsorNotFoundException(Long id) {
        super("Sponsor no encontrado: id=" + id);
    }
}
