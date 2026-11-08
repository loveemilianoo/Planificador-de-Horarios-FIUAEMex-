package main.motor;

import java.util.ArrayList;
import java.util.List;

import main.model.Alumno;
import main.model.Grupo;
import main.model.Materia;
import main.model.OfertaAcademica;

/**
 * Punto de entrada del motor. El servlet solo necesita llamar a {@link #generar}.
 *
 * Flujo:
 * 1. Filtra materias deseadas (aprobadas, existencia y seriación).
 * 2. Filtra grupos por día libre y turno.
 * 3. Busca combinaciones sin traslapes.
 * 4. Ordena y entrega de 2 a 4 opciones, con avisos si el escenario es infactible.
 *
 * No guarda estado entre llamadas, por lo que una sola instancia sirve para varios usuarios a la vez.
 */
public class MotorHorarios {

    private final TablaSeriaciones seriaciones;
    private final ConfiguracionMotor config;

    public MotorHorarios(TablaSeriaciones seriaciones, ConfiguracionMotor config) {
        this.seriaciones = seriaciones;
        this.config = config;
    }

    public MotorHorarios(TablaSeriaciones seriaciones) {
        this(seriaciones, ConfiguracionMotor.porDefecto());
    }

    public ResultadoGeneracion generar(OfertaAcademica oferta, Alumno alumno) {
        List<String> avisos = new ArrayList<>();

        // 1. Materias que sí se pueden cursar
        FiltroMaterias.Resultado filtro = new FiltroMaterias(seriaciones).filtrar(oferta, alumno);
        if (filtro.validas().isEmpty()) {
            avisos.add("Ninguna de las materias seleccionadas se puede cursar; revisa los motivos indicados.");
            return new ResultadoGeneracion(List.of(), filtro.descartadas(), avisos);
        }

        // 2. Grupos que respetan las restricciones del alumno
        FiltroPreferencias prefs = new FiltroPreferencias(config.horaCorteTurno());
        List<List<Grupo>> candidatos = new ArrayList<>();
        for (Materia m : filtro.validas()) {
            List<Grupo> admitidos = m.grupos().stream()
                    .filter(g -> !g.horarios().isEmpty())
                    .filter(g -> prefs.admite(g, alumno))
                    .toList();
            if (admitidos.isEmpty()) {
                avisos.add("Ningún grupo de " + m.nombre() + " (" + m.clave()
                        + ") cumple tu turno o día libre; prueba con restricciones más flexibles.");
                return new ResultadoGeneracion(List.of(), filtro.descartadas(), avisos);
            }
            candidatos.add(admitidos);
        }

        // 3. Combinaciones sin traslapes
        GeneradorHorarios.ResultadoBusqueda busqueda =
                new GeneradorHorarios(config, new EvaluadorOpcion()).generar(candidatos);

        // 4. Avisos según lo encontrado
        if (busqueda.opciones().isEmpty()) {
            avisos.add("No existe ninguna combinación sin traslapes con esta selección y restricciones.");
        } else if (busqueda.opciones().size() < config.minOpciones()) {
            avisos.add("Solo se encontró " + busqueda.opciones().size()
                    + " opción(es) viable(s) con tu selección.");
        }
        if (busqueda.truncada()) {
            avisos.add("Había muchas combinaciones posibles; se muestran las mejores de las exploradas.");
        }
        if (!busqueda.opciones().isEmpty()) {
            avisos.add("El cupo de los grupos puede cambiar en tiempo real; ten lista más de una opción.");
        }
        return new ResultadoGeneracion(busqueda.opciones(), filtro.descartadas(), avisos);
    }
}
