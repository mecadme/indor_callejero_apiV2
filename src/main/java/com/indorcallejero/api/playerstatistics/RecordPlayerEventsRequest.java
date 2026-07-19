package com.indorcallejero.api.playerstatistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// Una lista, no un evento suelto: el mismo request sirve para cargar los
// 10 eventos de un partido de una sola vez (hoy) o para mandar 1 evento
// por vez en tiempo real (el día que exista un reloj de partido) -- ver
// el comentario de diseño en PlayerMatchEventEntity.
public record RecordPlayerEventsRequest(
        @NotEmpty @Valid List<RecordPlayerEventRequest> events
) {
    // Copia inmutable, no la referencia tal cual -- mismo motivo que
    // UserDTO.roles (Etapa 12, dominio Role): Jackson deserializa un
    // ArrayList mutable acá, y sin esto quedaría expuesto directo.
    public RecordPlayerEventsRequest {
        events = List.copyOf(events);
    }
}
