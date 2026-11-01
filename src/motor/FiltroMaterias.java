package motor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Alumno;
import model.Materia;
import model.OfertaAcademica;

/**
 * Decide cuáles de las materias deseadas por el alumno se pueden realmente cursar (actividades A6.1 y A6.3).
 *
 * <p>Una materia deseada se descarta, con su motivo, si: ya está aprobada, no existe en la oferta,
 * no tiene grupos, o al alumno le faltan prerrequisitos de seriación.</p>
 */
public final class FiltroMaterias {

    /**
     * @param validas     materias que sí pasan a la generación de horarios
     * @param descartadas clave de materia -> motivo por el que se descartó
     */
    public record Resultado(List<Materia> validas, Map<String, String> descartadas) {
        public Resultado {
            validas = List.copyOf(validas);
            descartadas = Map.copyOf(descartadas);
        }
    }

    private final TablaSeriaciones seriaciones;

    public FiltroMaterias(TablaSeriaciones seriaciones) {
        this.seriaciones = seriaciones;
    }

    public Resultado filtrar(OfertaAcademica oferta, Alumno alumno) {
        List<Materia> validas = new ArrayList<>();
        Map<String, String> descartadas = new LinkedHashMap<>();

        // TreeSet: el conjunto del alumno no tiene orden; así los resultados son siempre los mismos
        for (String clave : new TreeSet<>(alumno.deseadas())) {
            if (alumno.aprobadas().contains(clave)) {
                descartadas.put(clave, "ya está aprobada");
                continue;
            }
            Materia materia = oferta.getMateria(clave);
            if (materia == null) {
                descartadas.put(clave, "no existe en la oferta académica cargada");
                continue;
            }
            if (materia.grupos().isEmpty()) {
                descartadas.put(clave, "no tiene grupos abiertos");
                continue;
            }
            Set<String> faltan = seriaciones.faltantes(clave, alumno.aprobadas());
            if (!faltan.isEmpty()) {
                descartadas.put(clave, "seriación no cumplida, falta aprobar: " + String.join(", ", faltan));
                continue;
            }
            validas.add(materia);
        }
        return new Resultado(validas, descartadas);
    }
}
