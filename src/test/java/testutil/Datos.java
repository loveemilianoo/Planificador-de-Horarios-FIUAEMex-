package main.testutil;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.model.Alumno;
import main.model.DiaSemana;
import main.model.Grupo;
import main.model.Horario;
import main.model.Materia;
import main.model.OfertaAcademica;
import main.model.Turno;

/** Constructores cortos para armar datos de prueba sin repetir código. */
public final class Datos {

    private Datos() {
    }

    /** Horario: h(LUN, "09:00", "11:00"). Las horas deben escribirse con dos dígitos. */
    public static Horario h(DiaSemana dia, String inicio, String fin) {
        return new Horario(dia, LocalTime.parse(inicio), LocalTime.parse(fin), "A-1");
    }

    /** Grupo de una materia: g("MAT1", "G1", h(...), h(...)). */
    public static Grupo g(String claveMateria, String claveGrupo, Horario... horarios) {
        return new Grupo(claveGrupo, claveMateria, "DOCENTE PRUEBA", List.of(horarios));
    }

    /** Materia con nombre "Materia <clave>" y semestre 1. */
    public static Materia m(String clave, Grupo... grupos) {
        return new Materia(clave, "Materia " + clave, 1, List.of(grupos));
    }

    public static OfertaAcademica oferta(Materia... materias) {
        Map<String, Materia> mapa = new LinkedHashMap<>();
        for (Materia materia : materias) {
            mapa.put(materia.clave(), materia);
        }
        return new OfertaAcademica(mapa);
    }

    /** Alumno sin restricciones de turno ni día libre. */
    public static Alumno alumno(Set<String> aprobadas, Set<String> deseadas) {
        return new Alumno(aprobadas, deseadas, null, null, 2);
    }

    public static Alumno alumno(Set<String> aprobadas, Set<String> deseadas, Turno turno, DiaSemana diaLibre) {
        return new Alumno(aprobadas, deseadas, turno, diaLibre, 2);
    }
}
