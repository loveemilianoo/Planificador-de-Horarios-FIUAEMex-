package motor;

import java.util.List;
import java.util.Map;

import model.OpcionHorario;

/**
 * Lo que el motor le entrega a la capa web.
 *
 * @param opciones             horarios sin traslapes, del mejor al peor (vacía si no hubo ninguno viable)
 * @param materiasDescartadas  clave de materia -> motivo (ya aprobada, seriación, no existe...)
 * @param avisos               mensajes para mostrar al estudiante (escenario infactible, pocas opciones...)
 */
public record ResultadoGeneracion(
        List<OpcionHorario> opciones,
        Map<String, String> materiasDescartadas,
        List<String> avisos) {

    public ResultadoGeneracion {
        opciones = List.copyOf(opciones);
        materiasDescartadas = Map.copyOf(materiasDescartadas);
        avisos = List.copyOf(avisos);
    }

    public boolean hayOpciones() {
        return !opciones.isEmpty();
    }
}
