package motor;

import java.time.LocalTime;

import model.Alumno;
import model.Grupo;
import model.Horario;
import model.Turno;

/**
 * Aplica las restricciones de horario del alumno a un grupo: día libre y turno preferido.
 *
 * <ul>
 *   <li><b>Día libre</b>: el grupo se rechaza si tiene alguna clase ese día.</li>
 *   <li><b>Turno</b>: MAÑANA = todas las clases empiezan antes de la hora de corte;
 *       TARDE = todas empiezan a la hora de corte o después.</li>
 * </ul>
 * Si el alumno no indicó preferencia (null), esa restricción no se aplica.
 */
public final class FiltroPreferencias {

    private final LocalTime horaCorte;

    public FiltroPreferencias(LocalTime horaCorte) {
        this.horaCorte = horaCorte;
    }

    /** true si el grupo respeta las restricciones del alumno. */
    public boolean admite(Grupo grupo, Alumno alumno) {
        for (Horario h : grupo.horarios()) {
            if (alumno.diaLibre() != null && h.dia() == alumno.diaLibre()) {
                return false;
            }
            Turno turno = alumno.turnoPreferido();
            if (turno == Turno.MANANA && !h.inicio().isBefore(horaCorte)) {
                return false;
            }
            if (turno == Turno.TARDE && h.inicio().isBefore(horaCorte)) {
                return false;
            }
        }
        return true;
    }
}
