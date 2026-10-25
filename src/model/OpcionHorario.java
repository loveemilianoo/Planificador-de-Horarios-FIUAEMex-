import java.util.List;
import java.util.Objects;
 
public record OpcionHorario(List<Grupo> grupos) { 
    public OpcionHorario {
        Objects.requireNonNull(grupos, "grupos es obligatorio");
        grupos = List.copyOf(grupos);
    }
}
 