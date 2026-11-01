package motor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import model.DiaSemana;
import model.Grupo;
import model.Horario;
import model.OpcionHorario;

/**
 * Califica qué tan cómodo es un horario para ordenar las opciones (menor puntaje = mejor).
 *
 * <p>Puntaje = minutos totales de huecos entre clases del mismo día
 * + {@value #PESO_DIA_MINUTOS} minutos por cada día distinto en que hay que ir a la facultad.
 * Se asume que la opción ya no tiene traslapes.</p>
 */
public final class EvaluadorOpcion {

    /** Cada día extra de asistencia "cuesta" lo mismo que una hora de hueco. */
    public static final int PESO_DIA_MINUTOS = 60;

    public double puntaje(OpcionHorario opcion) {
        Map<DiaSemana, List<Horario>> porDia = new EnumMap<>(DiaSemana.class);
        for (Grupo g : opcion.grupos()) {
            for (Horario h : g.horarios()) {
                porDia.computeIfAbsent(h.dia(), d -> new ArrayList<>()).add(h);
            }
        }

        long minutosHuecos = 0;
        for (List<Horario> clases : porDia.values()) {
            clases.sort(Comparator.comparing(Horario::inicio));
            for (int i = 1; i < clases.size(); i++) {
                long hueco = Duration.between(clases.get(i - 1).fin(), clases.get(i).inicio()).toMinutes();
                if (hueco > 0) minutosHuecos += hueco;
            }
        }
        return minutosHuecos + (double) porDia.size() * PESO_DIA_MINUTOS;
    }
}
