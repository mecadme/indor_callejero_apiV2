package com.indorcallejero.api.user;

import com.indorcallejero.api.auth.Role;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
        @NotNull Role role
) {
}
