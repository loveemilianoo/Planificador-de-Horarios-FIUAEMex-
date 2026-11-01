package motor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import model.Grupo;
import model.OpcionHorario;

/**
 * Núcleo combinatorio (actividad A6.2): elige exactamente un grupo por materia sin traslapes.
 *
 * <h3>Estrategia (riesgo R08)</h3>
 * <ul>
 *   <li><b>Backtracking con poda</b>: en cuanto un grupo se traslapa con los ya elegidos se descarta
 *       esa rama completa, sin explorar sus combinaciones.</li>
 *   <li><b>Materias con menos grupos primero</b>: las más restrictivas fallan antes y podan más.</li>
 *   <li><b>Tope de candidatas</b>: se detiene al juntar {@link ConfiguracionMotor#maxCandidatas()}
 *       combinaciones válidas; luego se ordenan con {@link EvaluadorOpcion} y se entregan las mejores.</li>
 * </ul>
 */
public class GeneradorHorarios {

    /**
     * @param opciones las mejores opciones, ordenadas de mejor a peor (máx. {@code maxOpciones})
     * @param truncada true si la búsqueda se detuvo por el tope de candidatas (puede haber más combinaciones)
     */
    public record ResultadoBusqueda(List<OpcionHorario> opciones, boolean truncada) {
        public ResultadoBusqueda {
            opciones = List.copyOf(opciones);
        }
    }

    private final ConfiguracionMotor config;
    private final EvaluadorOpcion evaluador;

    public GeneradorHorarios(ConfiguracionMotor config, EvaluadorOpcion evaluador) {
        this.config = config;
        this.evaluador = evaluador;
    }

    /**
     * @param gruposPorMateria una lista por materia con los grupos entre los que se puede elegir
     * @return las mejores opciones sin traslapes; lista vacía si no existe ninguna combinación válida
     */
    public ResultadoBusqueda generar(List<List<Grupo>> gruposPorMateria) {
        if (gruposPorMateria.isEmpty()) {
            return new ResultadoBusqueda(List.of(), false);
        }

        // Copia ordenada: no se modifica la lista del llamador
        List<List<Grupo>> ordenadas = new ArrayList<>(gruposPorMateria);
        ordenadas.sort(Comparator.comparingInt(List::size));

        List<OpcionHorario> candidatas = new ArrayList<>();
        buscar(0, ordenadas, new ArrayList<>(), candidatas);

        boolean truncada = candidatas.size() >= config.maxCandidatas();
        List<OpcionHorario> mejores = candidatas.stream()
                .sorted(Comparator.comparingDouble(evaluador::puntaje))
                .limit(config.maxOpciones())
                .toList();
        return new ResultadoBusqueda(mejores, truncada);
    }

    /** Recursión: en el nivel {@code idx} se prueba cada grupo de la materia idx. */
    private void buscar(int idx, List<List<Grupo>> materias, List<Grupo> actual, List<OpcionHorario> candidatas) {
        if (idx == materias.size()) {
            candidatas.add(new OpcionHorario(actual)); // OpcionHorario copia la lista
            return;
        }
        for (Grupo g : materias.get(idx)) {
            if (DetectorTraslapes.traslapaConAlguno(g, actual)) {
                continue; // poda
            }
            actual.add(g);
            buscar(idx + 1, materias, actual, candidatas);
            actual.remove(actual.size() - 1); // backtrack
            if (candidatas.size() >= config.maxCandidatas()) {
                return;
            }
        }
    }
}
