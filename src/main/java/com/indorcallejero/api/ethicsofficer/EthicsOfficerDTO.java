package com.indorcallejero.api.ethicsofficer;

public record EthicsOfficerDTO(
        Long id,
        String firstName,
        String lastName,
        String photoUrl,
        Long teamId,
        String teamName
) {
}
