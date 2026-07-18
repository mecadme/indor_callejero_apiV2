package com.indorcallejero.api.match;

// Simplificado a propósito respecto del original (que sumaba PAUSED): esa
// etapa era del reloj de partido en tiempo real, fuera de alcance acá --
// esta etapa es sobre el resultado y las standings, no sobre el cronómetro.
public enum MatchStatus {
    SCHEDULED, IN_PROGRESS, FINISHED
}
