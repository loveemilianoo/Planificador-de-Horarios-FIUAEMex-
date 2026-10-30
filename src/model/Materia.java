package model;

import java.util.List;
import java.util.Objects;
 
public record Materia(String clave, String nombre, int semestre, List<Grupo> grupos) {
 
    public Materia {
        Objects.requireNonNull(clave, "clave es obligatoria");
        Objects.requireNonNull(nombre, "nombre es obligatorio");
        grupos = grupos == null ? List.of() : List.copyOf(grupos);
    }
}
 
