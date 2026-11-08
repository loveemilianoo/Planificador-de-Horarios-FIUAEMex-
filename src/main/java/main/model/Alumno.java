package main.model;

import java.util.Set;

/** Representa al alumno y sus materias, preferencias y semestre actual. */
public record Alumno(
    Set<String> aprobadas,
    Set<String> deseadas,
    Turno turnoPreferido,
    DiaSemana diaLibre,
    int semestre
) {

    public Alumno{
        aprobadas = aprobadas == null ? Set.of() : Set.copyOf(aprobadas);
        deseadas = deseadas == null ? Set.of() : Set.copyOf(deseadas);
    }

}
