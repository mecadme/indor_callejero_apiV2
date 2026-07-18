package com.indorcallejero.api.information;

public record InformationDTO(
        Long id,
        String title,
        String content,
        String photoUrl
) {
}
