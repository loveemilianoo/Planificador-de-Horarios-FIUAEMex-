package motor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tabla de seriaciones reglamentarias: para cada materia, las claves de las materias que se
 * deben tener aprobadas antes de poder cursarla.
 *
 * Una materia que no aparece en la tabla no tiene prerrequisitos. La tabla es inmutable.
 * Los datos reales deben venir de la tabla oficial validada por la Coordinación (hito H1).
 */
public class TablaSeriaciones {

    private final Map<String, Set<String>> prerrequisitos;

    /** @param prerrequisitos clave de materia -> claves que debe tener aprobadas */
    public TablaSeriaciones(Map<String, Set<String>> prerrequisitos) {
        Objects.requireNonNull(prerrequisitos, "prerrequisitos es obligatorio");
        Map<String, Set<String>> copia = new HashMap<>();
        prerrequisitos.forEach((clave, reqs) -> copia.put(clave, Set.copyOf(reqs)));
        this.prerrequisitos = Map.copyOf(copia);
    }

    /** Tabla sin seriaciones (útil para pruebas). */
    public static TablaSeriaciones vacia() {
        return new TablaSeriaciones(Map.of());
    }

    /** Prerrequisitos de una materia; conjunto vacío si no tiene. */
    public Set<String> prerrequisitosDe(String claveMateria) {
        return prerrequisitos.getOrDefault(claveMateria, Set.of());
    }

    /** Prerrequisitos que al alumno le faltan por aprobar (ordenados para mensajes estables). */
    public Set<String> faltantes(String claveMateria, Set<String> aprobadas) {
        Set<String> faltan = new TreeSet<>(prerrequisitosDe(claveMateria));
        faltan.removeAll(aprobadas);
        return faltan;
    }

    /** true si el alumno tiene aprobados todos los prerrequisitos de la materia. */
    public boolean cumple(String claveMateria, Set<String> aprobadas) {
        return faltantes(claveMateria, aprobadas).isEmpty();
    }
}
