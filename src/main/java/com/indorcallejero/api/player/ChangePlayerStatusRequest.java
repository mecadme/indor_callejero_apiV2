package com.indorcallejero.api.player;

import jakarta.validation.constraints.NotNull;

// Un endpoint, no los tres PUT /active, /suspended, /injured casi
// idénticos que tenía el proyecto original -- mismo cuerpo de método,
// misma validación, la única diferencia real era el valor del enum.
public record ChangePlayerStatusRequest(@NotNull PlayerStatus status) {
}
