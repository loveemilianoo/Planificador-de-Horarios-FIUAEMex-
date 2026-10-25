import java.util.Objects;
import java.util.List;

public record Grupo(String clave, String claveMateria, String docente, List<Horario> horarios) {
    public Grupo {
        Objects.requireNonNull(clave, "clave es obligatorio");
        Objects.requireNonNull(claveMateria, "claveMateria es obligatorio");
        horarios = horarios == null ? List.of() : List.copyOf(horarios);
    }   
}
