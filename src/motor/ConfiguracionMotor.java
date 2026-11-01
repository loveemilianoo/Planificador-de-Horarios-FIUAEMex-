package motor;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Parámetros ajustables del motor combinatorio.
 *
 * @param minOpciones     mínimo de opciones que se espera entregar (el Acta pide 2); si hay menos se avisa
 * @param maxOpciones     máximo de opciones que se entregan al estudiante (el Acta pide 4)
 * @param maxCandidatas   tope de combinaciones válidas que explora el backtracking antes de detenerse
 *                        (evita la explosión combinatoria, riesgo R08)
 * @param horaCorteTurno  hora que separa MAÑANA de TARDE: un grupo es de mañana si todas sus clases
 *                        empiezan antes de esta hora, y de tarde si todas empiezan a esta hora o después
 */
public record ConfiguracionMotor(int minOpciones, int maxOpciones, int maxCandidatas, LocalTime horaCorteTurno) {

    public ConfiguracionMotor {
        if (minOpciones < 1 || maxOpciones < minOpciones) {
            throw new IllegalArgumentException("Se requiere 1 <= minOpciones <= maxOpciones");
        }
        if (maxCandidatas < maxOpciones) {
            throw new IllegalArgumentException("maxCandidatas no puede ser menor que maxOpciones");
        }
        Objects.requireNonNull(horaCorteTurno, "horaCorteTurno es obligatoria");
    }

    /** Valores del Acta Constitutiva: de 2 a 4 opciones, corte de turno a las 13:00. */
    public static ConfiguracionMotor porDefecto() {
        return new ConfiguracionMotor(2, 4, 5000, LocalTime.of(13, 0));
    }
}
