package main.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import main.model.Alumno;
import main.model.DiaSemana;
import main.model.Materia;
import main.model.OfertaAcademica;
import main.model.Turno;
import main.motor.TablaSeriaciones;

/**
 * Lo que el alumno va capturando en el asistente; vive en la sesión HTTP.
 * Expone getters para que las JSP lo lean con EL.
 */
public class EstadoAlumno {

    public static final int SEMESTRE_MIN = 2;
    public static final int SEMESTRE_MAX = 4;

    private final OfertaAcademica oferta;
    private final TablaSeriaciones seriaciones;

    private int semestre = SEMESTRE_MIN;
    private Set<String> aprobadas = new HashSet<>();
    private Set<String> deseadas = new HashSet<>();
    private Turno turno = Turno.SIN_PREFERENCIA;
    private DiaSemana diaLibre;
    private int opcionSeleccionada;

    public EstadoAlumno(OfertaAcademica oferta, TablaSeriaciones seriaciones) {
        this.oferta = oferta;
        this.seriaciones = seriaciones;
    }

    public int getSemestre() { return semestre; }

    public void setSemestre(int semestre) {
        this.semestre = Math.max(SEMESTRE_MIN, Math.min(SEMESTRE_MAX, semestre));
    }

    public Set<String> getAprobadas() { return aprobadas; }

    public void setAprobadas(Set<String> aprobadas) { this.aprobadas = new HashSet<>(aprobadas); }

    public Set<String> getDeseadas() { return deseadas; }

    public void setDeseadas(Set<String> deseadas) { this.deseadas = new HashSet<>(deseadas); }

    public Turno getTurno() { return turno; }

    public void setTurno(Turno turno) { this.turno = turno == null ? Turno.SIN_PREFERENCIA : turno; }

    public DiaSemana getDiaLibre() { return diaLibre; }

    public void setDiaLibre(DiaSemana diaLibre) { this.diaLibre = diaLibre; }

    public int getOpcionSeleccionada() { return opcionSeleccionada; }

    public void setOpcionSeleccionada(int opcionSeleccionada) { this.opcionSeleccionada = opcionSeleccionada; }

    /** Convierte el valor del formulario (mat / ves / sin) en un {@link Turno}. */
    public static Turno turnoDeFormulario(String valor) {
        if ("mat".equals(valor)) return Turno.MANANA;
        if ("ves".equals(valor)) return Turno.TARDE;
        return Turno.SIN_PREFERENCIA;
    }

    /** Materias de semestres anteriores al que se cursará (las que el alumno puede declarar aprobadas). */
    public List<MateriaVista> getAnteriores() {
        return materiasDe(m -> m.semestre() >= 1 && m.semestre() < semestre);
    }

    /** Materias que el alumno puede elegir: semestre actual o anteriores que no haya aprobado. */
    public List<MateriaVista> getLista() {
        return materiasDe(m -> m.semestre() >= 1 && m.semestre() <= semestre
                && !aprobadas.contains(m.clave()));
    }

    /** Nombres de los prerrequisitos que le faltan para cursar la materia (vacío si puede cursarla). */
    public List<String> faltantes(MateriaVista m) {
        List<String> nombres = new ArrayList<>();
        for (String clave : seriaciones.faltantes(m.getClave(), aprobadas)) {
            Materia previa = oferta.getMateria(clave);
            nombres.add(previa == null ? clave : previa.nombre());
        }
        nombres.sort(Comparator.naturalOrder());
        return nombres;
    }

    /** Arma el {@link Alumno} que consume el motor. */
    public Alumno aAlumno() {
        return new Alumno(aprobadas, deseadas, turno, diaLibre, semestre);
    }

    private List<MateriaVista> materiasDe(Predicate<Materia> filtro) {
        return oferta.materias().values().stream()
                .filter(filtro)
                .sorted(Comparator.comparingInt(Materia::semestre).thenComparing(Materia::nombre))
                .map(MateriaVista::new)
                .toList();
    }
}
