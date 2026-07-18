package com.indorcallejero.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 8, message = "debe tener al menos 8 caracteres") String newPassword
) {
}
