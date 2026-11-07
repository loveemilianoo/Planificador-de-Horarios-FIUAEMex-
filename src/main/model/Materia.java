package main.model;

import java.util.List;
import java.util.Objects;
 
/** Representa una materia académica y los grupos disponibles para cursarla. */
public record Materia(String clave, String nombre, int semestre, List<Grupo> grupos) {
 
    public Materia {
        Objects.requireNonNull(clave, "clave es obligatoria");
        Objects.requireNonNull(nombre, "nombre es obligatorio");
        grupos = grupos == null ? List.of() : List.copyOf(grupos);
    }
}
 
