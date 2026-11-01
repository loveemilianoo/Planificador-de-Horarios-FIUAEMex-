package motor;

import java.util.List;

import model.Grupo;
import model.Horario;

/**
 * Detecta empalmes de horario. Es la base de la poda del backtracking.
 *
 * <p>Dos clases se traslapan si son el mismo día y sus intervalos se cruzan. Si una termina
 * justo cuando empieza la otra (10:00 y 10:00) NO se consideran traslapadas.</p>
 */
public final class DetectorTraslapes {

    private DetectorTraslapes() {
    }

    /** Dos bloques horarios se traslapan: mismo día e intervalos que se cruzan. */
    public static boolean traslapan(Horario a, Horario b) {
        return a.dia() == b.dia()
                && a.inicio().isBefore(b.fin())
                && b.inicio().isBefore(a.fin());
    }

    /** Dos grupos se traslapan si algún bloque de uno se cruza con algún bloque del otro. */
    public static boolean traslapan(Grupo a, Grupo b) {
        for (Horario ha : a.horarios()) {
            for (Horario hb : b.horarios()) {
                if (traslapan(ha, hb)) return true;
            }
        }
        return false;
    }

    /** true si el candidato se traslapa con al menos uno de los grupos ya elegidos. */
    public static boolean traslapaConAlguno(Grupo candidato, List<Grupo> elegidos) {
        for (Grupo g : elegidos) {
            if (traslapan(candidato, g)) return true;
        }
        return false;
    }
}
