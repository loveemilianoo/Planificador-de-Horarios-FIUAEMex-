package main.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static main.testutil.Datos.alumno;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;
import static main.testutil.Datos.m;
import static main.testutil.Datos.oferta;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.DiaSemana;
import main.model.Grupo;
import main.model.OfertaAcademica;
import main.model.OpcionHorario;
import main.model.Turno;

/** Pruebas del flujo completo del motor: filtros + generación + avisos. */
class MotorHorariosTest {

    private final MotorHorarios motor = new MotorHorarios(TablaSeriaciones.vacia());

    private static boolean hayAviso(ResultadoGeneracion r, String fragmento) {
        return r.avisos().stream().anyMatch(a -> a.contains(fragmento));
    }

    private static void assertSinTraslapes(OpcionHorario opcion) {
        List<Grupo> grupos = opcion.grupos();
        for (int i = 0; i < grupos.size(); i++) {
            for (int j = i + 1; j < grupos.size(); j++) {
                assertFalse(DetectorTraslapes.traslapan(grupos.get(i), grupos.get(j)));
            }
        }
    }

    /** MAT1: G1 lun 9-10, G2 mar 9-10. MAT2: G1 lun 10-11, G2 lun 9-10. -> 3 combinaciones válidas. */
    private static OfertaAcademica ofertaBase() {
        return oferta(
                m("MAT1",
                        g("MAT1", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                        g("MAT1", "G2", h(DiaSemana.MAR, "09:00", "10:00"))),
                m("MAT2",
                        g("MAT2", "G1", h(DiaSemana.LUN, "10:00", "11:00")),
                        g("MAT2", "G2", h(DiaSemana.LUN, "09:00", "10:00"))));
    }

    @Test
    @DisplayName("Caso feliz: genera opciones sin traslapes y avisa que el cupo puede cambiar")
    void casoFeliz() {
        ResultadoGeneracion r = motor.generar(ofertaBase(), alumno(Set.of(), Set.of("MAT1", "MAT2")));

        assertTrue(r.hayOpciones());
        assertEquals(3, r.opciones().size());
        r.opciones().forEach(o -> {
            assertEquals(2, o.grupos().size());
            assertSinTraslapes(o);
        });
        assertTrue(r.materiasDescartadas().isEmpty());
        assertTrue(hayAviso(r, "cupo"));
    }

    @Test
    @DisplayName("Entrega a lo más 4 opciones aunque existan más combinaciones")
    void maximoCuatroOpciones() {
        OfertaAcademica o = oferta(m("MAT1",
                g("MAT1", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("MAT1", "G2", h(DiaSemana.MAR, "09:00", "10:00")),
                g("MAT1", "G3", h(DiaSemana.MIE, "09:00", "10:00")),
                g("MAT1", "G4", h(DiaSemana.JUE, "09:00", "10:00")),
                g("MAT1", "G5", h(DiaSemana.VIE, "09:00", "10:00")),
                g("MAT1", "G6", h(DiaSemana.SAB, "09:00", "10:00"))));
        ResultadoGeneracion r = motor.generar(o, alumno(Set.of(), Set.of("MAT1")));
        assertEquals(4, r.opciones().size());
    }

    @Test
    @DisplayName("La misma entrada produce siempre el mismo resultado")
    void deterministico() {
        var al = alumno(Set.of(), Set.of("MAT1", "MAT2"));
        ResultadoGeneracion r1 = motor.generar(ofertaBase(), al);
        ResultadoGeneracion r2 = motor.generar(ofertaBase(), al);
        assertEquals(r1.opciones(), r2.opciones());
        assertEquals(r1.avisos(), r2.avisos());
    }

    @Test
    @DisplayName("Una materia aprobada se descarta y las demás sí generan horario")
    void materiaAprobadaDescartada() {
        ResultadoGeneracion r = motor.generar(ofertaBase(), alumno(Set.of("MAT2"), Set.of("MAT1", "MAT2")));
        assertEquals("ya está aprobada", r.materiasDescartadas().get("MAT2"));
        assertEquals(2, r.opciones().size());
        r.opciones().forEach(o -> assertEquals(1, o.grupos().size()));
    }

    @Test
    @DisplayName("Si ninguna materia es cursable devuelve vacío con aviso y motivos")
    void ningunaMateriaValida() {
        ResultadoGeneracion r = motor.generar(ofertaBase(), alumno(Set.of(), Set.of("NOEXISTE")));
        assertFalse(r.hayOpciones());
        assertTrue(hayAviso(r, "Ninguna de las materias"));
        assertTrue(r.materiasDescartadas().containsKey("NOEXISTE"));
    }

    @Test
    @DisplayName("Sin materias deseadas tampoco hay opciones")
    void sinDeseadas() {
        ResultadoGeneracion r = motor.generar(ofertaBase(), alumno(Set.of(), Set.of()));
        assertFalse(r.hayOpciones());
        assertTrue(hayAviso(r, "Ninguna de las materias"));
    }

    @Test
    @DisplayName("La seriación bloquea la materia cuyo prerrequisito no está aprobado")
    void seriacionBloquea() {
        MotorHorarios conSeriacion = new MotorHorarios(
                new TablaSeriaciones(Map.of("MAT2", Set.of("MAT1"))));
        ResultadoGeneracion r = conSeriacion.generar(ofertaBase(), alumno(Set.of(), Set.of("MAT1", "MAT2")));

        assertTrue(r.materiasDescartadas().get("MAT2").contains("seriación no cumplida"));
        assertTrue(r.hayOpciones());
        r.opciones().forEach(o -> o.grupos().forEach(gr -> assertEquals("MAT1", gr.claveMateria())));
    }

    @Test
    @DisplayName("Con el prerrequisito aprobado la materia seriada sí se programa")
    void seriacionCumplida() {
        MotorHorarios conSeriacion = new MotorHorarios(
                new TablaSeriaciones(Map.of("MAT2", Set.of("MAT1"))));
        ResultadoGeneracion r = conSeriacion.generar(ofertaBase(), alumno(Set.of("MAT1"), Set.of("MAT2")));

        assertFalse(r.materiasDescartadas().containsKey("MAT2"));
        assertTrue(r.hayOpciones());
        r.opciones().forEach(o -> assertEquals("MAT2", o.grupos().get(0).claveMateria()));
    }

    @Test
    @DisplayName("Escenario infactible: avisa que no hay combinación sin traslapes")
    void escenarioInfactible() {
        OfertaAcademica o = oferta(
                m("MAT1", g("MAT1", "G1", h(DiaSemana.LUN, "09:00", "10:00"))),
                m("MAT2", g("MAT2", "G1", h(DiaSemana.LUN, "09:30", "10:30"))));
        ResultadoGeneracion r = motor.generar(o, alumno(Set.of(), Set.of("MAT1", "MAT2")));

        assertFalse(r.hayOpciones());
        assertTrue(hayAviso(r, "No existe ninguna combinación sin traslapes"));
        assertFalse(hayAviso(r, "cupo"));
    }

    @Test
    @DisplayName("Con una sola opción viable avisa que son menos de las esperadas")
    void pocasOpciones() {
        OfertaAcademica o = oferta(
                m("MAT1", g("MAT1", "G1", h(DiaSemana.LUN, "09:00", "10:00"))),
                m("MAT2", g("MAT2", "G1", h(DiaSemana.MAR, "09:00", "10:00"))));
        ResultadoGeneracion r = motor.generar(o, alumno(Set.of(), Set.of("MAT1", "MAT2")));

        assertEquals(1, r.opciones().size());
        assertTrue(hayAviso(r, "Solo se encontró 1"));
    }

    @Test
    @DisplayName("Turno sin grupos disponibles: avisa y no genera")
    void turnoSinGrupos() {
        OfertaAcademica o = oferta(m("MAT1", g("MAT1", "G1", h(DiaSemana.LUN, "14:00", "15:00"))));
        ResultadoGeneracion r = motor.generar(o,
                alumno(Set.of(), Set.of("MAT1"), Turno.MANANA, null));

        assertFalse(r.hayOpciones());
        assertTrue(hayAviso(r, "cumple tu turno o día libre"));
    }

    @Test
    @DisplayName("El día libre excluye los grupos con clase ese día")
    void diaLibreExcluyeGrupos() {
        OfertaAcademica o = oferta(m("MAT1",
                g("MAT1", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("MAT1", "G2", h(DiaSemana.VIE, "09:00", "10:00"))));
        ResultadoGeneracion r = motor.generar(o, alumno(Set.of(), Set.of("MAT1"), null, DiaSemana.VIE));

        assertEquals(1, r.opciones().size());
        assertEquals("G1", r.opciones().get(0).grupos().get(0).clave());
    }

    @Test
    @DisplayName("Los grupos sin horarios se ignoran")
    void ignoraGruposSinHorarios() {
        OfertaAcademica o = oferta(m("MAT1",
                g("MAT1", "SIN"),
                g("MAT1", "CON", h(DiaSemana.LUN, "09:00", "10:00"))));
        ResultadoGeneracion r = motor.generar(o, alumno(Set.of(), Set.of("MAT1")));

        assertEquals(1, r.opciones().size());
        assertEquals("CON", r.opciones().get(0).grupos().get(0).clave());
    }

    @Test
    @DisplayName("Si todos los grupos de una materia carecen de horario, avisa")
    void todosLosGruposSinHorarios() {
        OfertaAcademica o = oferta(m("MAT1", g("MAT1", "SIN")));
        ResultadoGeneracion r = motor.generar(o, alumno(Set.of(), Set.of("MAT1")));

        assertFalse(r.hayOpciones());
        assertTrue(hayAviso(r, "Ningún grupo de"));
    }

    @Test
    @DisplayName("Una configuración personalizada limita el número de opciones")
    void configuracionPersonalizada() {
        MotorHorarios limitado = new MotorHorarios(TablaSeriaciones.vacia(),
                new ConfiguracionMotor(1, 2, 100, LocalTime.of(13, 0)));
        ResultadoGeneracion r = limitado.generar(ofertaBase(), alumno(Set.of(), Set.of("MAT1", "MAT2")));
        assertEquals(2, r.opciones().size());
    }

    @Test
    @DisplayName("Con demasiadas combinaciones avisa que se muestran las mejores de las exploradas")
    void avisoDeTruncado() {
        OfertaAcademica o = oferta(
                m("MAT1",
                        g("MAT1", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                        g("MAT1", "G2", h(DiaSemana.MAR, "09:00", "10:00")),
                        g("MAT1", "G3", h(DiaSemana.MIE, "09:00", "10:00"))),
                m("MAT2", g("MAT2", "G1", h(DiaSemana.JUE, "09:00", "10:00"))));
        MotorHorarios limitado = new MotorHorarios(TablaSeriaciones.vacia(),
                new ConfiguracionMotor(1, 2, 2, LocalTime.of(13, 0)));
        ResultadoGeneracion r = limitado.generar(o, alumno(Set.of(), Set.of("MAT1", "MAT2")));

        assertTrue(hayAviso(r, "muchas combinaciones"));
    }

    @Test
    @DisplayName("Si las combinaciones caben justo en el tope, no avisa de truncado")
    void sinAvisoDeTruncadoEnElTope() {
        OfertaAcademica o = oferta(
                m("MAT1",
                        g("MAT1", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                        g("MAT1", "G2", h(DiaSemana.MAR, "09:00", "10:00"))),
                m("MAT2", g("MAT2", "G1", h(DiaSemana.JUE, "09:00", "10:00"))));
        MotorHorarios limitado = new MotorHorarios(TablaSeriaciones.vacia(),
                new ConfiguracionMotor(1, 2, 2, LocalTime.of(13, 0)));
        ResultadoGeneracion r = limitado.generar(o, alumno(Set.of(), Set.of("MAT1", "MAT2")));

        assertEquals(2, r.opciones().size());
        assertFalse(hayAviso(r, "muchas combinaciones"));
    }
}
