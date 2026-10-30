package model;

import java.util.Set;

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
