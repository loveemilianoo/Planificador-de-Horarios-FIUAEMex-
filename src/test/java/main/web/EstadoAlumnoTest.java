package main.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import main.model.Materia;
import main.model.OfertaAcademica;
import main.model.Turno;
import main.motor.TablaSeriaciones;

class EstadoAlumnoTest {

    private EstadoAlumno estado;

    @BeforeEach
    void preparar() {
        OfertaAcademica oferta = new OfertaAcademica(Map.of(
                "A1", new Materia("A1", "Algebra", 1, List.of()),
                "B2", new Materia("B2", "Calculo", 2, List.of()),
                "C3", new Materia("C3", "Fisica", 3, List.of()),
                "O0", new Materia("O0", "Optativa", 0, List.of())));
        TablaSeriaciones seriaciones = new TablaSeriaciones(Map.of("C3", Set.of("B2")));
        estado = new EstadoAlumno(oferta, seriaciones);
    }

    @Test
    void semestreSeAcotaEntre2y4() {
        estado.setSemestre(9);
        assertEquals(4, estado.getSemestre());
        estado.setSemestre(0);
        assertEquals(2, estado.getSemestre());
    }

    @Test
    void anterioresSoloIncluyeSemestresPrevios() {
        estado.setSemestre(3);
        List<String> claves = estado.getAnteriores().stream().map(MateriaVista::getClave).toList();
        assertEquals(List.of("A1", "B2"), claves);
    }

    @Test
    void listaExcluyeAprobadasYOptativas() {
        estado.setSemestre(3);
        estado.setAprobadas(Set.of("A1"));
        List<String> claves = estado.getLista().stream().map(MateriaVista::getClave).toList();
        assertEquals(List.of("B2", "C3"), claves);
    }

    @Test
    void faltantesDevuelveNombreDelPrerrequisito() {
        MateriaVista fisica = new MateriaVista(new Materia("C3", "Fisica", 3, List.of()));
        assertEquals(List.of("Calculo"), estado.faltantes(fisica));
        estado.setAprobadas(Set.of("B2"));
        assertTrue(estado.faltantes(fisica).isEmpty());
    }

    @Test
    void turnoDeFormularioTraduceValores() {
        assertEquals(Turno.MANANA, EstadoAlumno.turnoDeFormulario("mat"));
        assertEquals(Turno.TARDE, EstadoAlumno.turnoDeFormulario("ves"));
        assertEquals(Turno.SIN_PREFERENCIA, EstadoAlumno.turnoDeFormulario("sin"));
        assertEquals(Turno.SIN_PREFERENCIA, EstadoAlumno.turnoDeFormulario(null));
    }
}
