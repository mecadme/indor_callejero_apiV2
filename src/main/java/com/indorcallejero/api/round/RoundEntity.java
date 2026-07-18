package com.indorcallejero.api.round;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// "number", no "round" a secas para el nombre de columna del orden:
// ROUND ya es un nombre de función SQL en MySQL, y aunque no es palabra
// reservada de verdad (a diferencia de GROUP, que sí rompió la Etapa 6),
// no vale la pena arriesgarse dos veces con el mismo tipo de bug.
@Entity
@Table(name = "rounds")
public class RoundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // El orden real de las fechas/fases (1, 2, 3... o el número que sea
    // para instancias eliminatorias) -- separado del id autogenerado a
    // propósito, porque el id no tiene por qué coincidir con el orden si
    // algún día se crea una fecha fuera de secuencia.
    @Column(name = "round_number", nullable = false)
    private Integer number;

    protected RoundEntity() {
    }

    public RoundEntity(String name, Integer number) {
        this.name = name;
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
