package main.model;

import java.util.List;
import java.util.Objects;
 
/** Representa una combinación posible de grupos para el horario de un alumno. */
public record OpcionHorario(List<Grupo> grupos) { 
    public OpcionHorario {
        Objects.requireNonNull(grupos, "grupos es obligatorio");
        grupos = List.copyOf(grupos);
    }
}
 