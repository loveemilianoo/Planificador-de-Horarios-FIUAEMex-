import java.util.Map;
import java.util.Objects;
 
public record OfertaAcademica(Map<String, Materia> materias) { 
    public OfertaAcademica {
        Objects.requireNonNull(materias, "materias es obligatorio");
        materias = Map.copyOf(materias);
    }
 
    public Materia getMateria(String clave) {
        return materias.get(clave);
    }
}
 