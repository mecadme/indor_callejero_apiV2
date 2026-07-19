package com.indorcallejero.api.playerstatistics;

// Arranca con lo esencial para un resumen de partido. Sumar un tipo de
// stat nuevo es agregar una constante acá, no una clase de entidad nueva
// -- a diferencia del proyecto original, que tenía una subclase JPA
// (single-table inheritance) por cada uno de estos: Goal, Assist, Card,
// Aerials, Clearance, Foul, Pass, TotalShots, etc. Esa tabla quedaba
// enorme y llena de columnas nulas para la mayoría de las filas, y cada
// stat nueva era una migración de esquema. Acá es un enum.
public enum StatType {
    GOAL,
    ASSIST,
    YELLOW_CARD,
    RED_CARD,
    SHOT,
    FOUL
}
