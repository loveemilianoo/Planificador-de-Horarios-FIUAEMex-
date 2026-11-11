package main.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import main.model.Alumno;
import main.model.DiaSemana;
import main.model.Grupo;
import main.model.Horario;
import main.model.Materia;
import main.model.OfertaAcademica;
import main.model.OpcionHorario;
import main.model.Turno;
import main.motor.ResultadoGeneracion;

class ResultadoVistaTest {

    private static final Grupo GRUPO = new Grupo("CO01", "A1", "DR. X",
            List.of(new Horario(DiaSemana.MAR, java.time.LocalTime.of(9, 30), java.time.LocalTime.of(11, 30), "C-1")));
    private static final OfertaAcademica OFERTA = new OfertaAcademica(
            Map.of("A1", new Materia("A1", "Algebra", 2, List.of(GRUPO))));
    private static final Alumno ALUMNO = new Alumno(Set.of(), Set.of("A1"), Turno.MANANA, DiaSemana.VIE, 2);

    @Test
    void conOpcionesArmaEtiquetasCalendarioYCarga() {
        ResultadoGeneracion r = new ResultadoGeneracion(List.of(new OpcionHorario(List.of(GRUPO))), Map.of(), List.of());
        ResultadoVista v = ResultadoVista.de(r, OFERTA, ALUMNO);

        assertTrue(v.getHayViables());
        assertEquals("Opción A", v.getOpciones().get(0).getEtiqueta());
        assertEquals("1 materias · 2 h por semana · 1 días de asistencia", v.getOpciones().get(0).getResumen());
        assertFalse(v.getMensajeOpcionUnica().isEmpty());
        assertEquals(9, v.getHoraIni());
        assertEquals(11, v.getHoraFin());

        // martes = índice 1, empieza a las 9:30 -> 16 px de desplazamiento, 2 h = 62 px
        ResultadoVista.CeldaVista celda = v.celdas(0).get("1-9").get(0);
        assertEquals(16, celda.getTop());
        assertEquals(62, celda.getAlto());
        assertEquals("Algebra", celda.getBloque().getEtiqueta());
        assertEquals("Algebra", v.carga(0).get(0).getMateria());
        assertEquals("c0", v.carga(0).get(0).getColor());
    }

    @Test
    void sinOpcionesDaMensajeYSugerencias() {
        ResultadoGeneracion r = new ResultadoGeneracion(List.of(), Map.of("A1", "ya aprobada"), List.of());
        ResultadoVista v = ResultadoVista.de(r, OFERTA, ALUMNO);

        assertFalse(v.getHayViables());
        assertFalse(v.getMensajeSinResultados().isBlank());
        assertTrue(v.getSugerencias().contains("A1: ya aprobada"));
        assertTrue(v.getSugerencias().contains("Desmarca el día libre."));
        assertTrue(v.celdas(0).isEmpty());
        assertTrue(v.carga(0).isEmpty());
    }
}
