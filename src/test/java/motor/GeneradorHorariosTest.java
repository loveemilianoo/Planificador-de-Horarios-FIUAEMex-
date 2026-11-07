package main.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.DiaSemana;
import main.model.Grupo;
import main.model.OpcionHorario;

class GeneradorHorariosTest {

    private static final LocalTime CORTE = LocalTime.of(13, 0);

    private GeneradorHorarios generador(int min, int max, int candidatas) {
        return new GeneradorHorarios(new ConfiguracionMotor(min, max, candidatas, CORTE), new EvaluadorOpcion());
    }

    private GeneradorHorarios generadorPorDefecto() {
        return new GeneradorHorarios(ConfiguracionMotor.porDefecto(), new EvaluadorOpcion());
    }

    /** Verifica que una opción tenga un grupo por materia y que ningún par se traslape. */
    private static void assertOpcionValida(OpcionHorario opcion, int materiasEsperadas) {
        List<Grupo> grupos = opcion.grupos();
        assertEquals(materiasEsperadas, grupos.size());
        Set<String> materias = new HashSet<>();
        for (Grupo grupo : grupos) {
            assertTrue(materias.add(grupo.claveMateria()), "materia repetida: " + grupo.claveMateria());
        }
        for (int i = 0; i < grupos.size(); i++) {
            for (int j = i + 1; j < grupos.size(); j++) {
                assertFalse(DetectorTraslapes.traslapan(grupos.get(i), grupos.get(j)));
            }
        }
    }

    @Test
    @DisplayName("Sin materias no hay opciones")
    void sinMaterias() {
        GeneradorHorarios.ResultadoBusqueda r = generadorPorDefecto().generar(List.of());
        assertTrue(r.opciones().isEmpty());
        assertFalse(r.truncada());
    }

    @Test
    @DisplayName("Una sola materia: cada grupo es una opción")
    void unaMateria() {
        List<Grupo> a = List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("A", "G2", h(DiaSemana.MAR, "09:00", "10:00")));
        GeneradorHorarios.ResultadoBusqueda r = generadorPorDefecto().generar(List.of(a));
        assertEquals(2, r.opciones().size());
        r.opciones().forEach(o -> assertOpcionValida(o, 1));
    }

    @Test
    @DisplayName("Encuentra todas las combinaciones sin traslapes y descarta las que se empalman")
    void combinacionesSinTraslapes() {
        List<Grupo> a = List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("A", "G2", h(DiaSemana.MAR, "09:00", "10:00")));
        List<Grupo> b = List.of(
                g("B", "G1", h(DiaSemana.LUN, "10:00", "11:00")),
                g("B", "G2", h(DiaSemana.LUN, "09:00", "10:00")));
        // A1+B1 ok, A1+B2 traslape, A2+B1 ok, A2+B2 ok -> 3
        GeneradorHorarios.ResultadoBusqueda r = generadorPorDefecto().generar(List.of(a, b));
        assertEquals(3, r.opciones().size());
        r.opciones().forEach(o -> assertOpcionValida(o, 2));
    }

    @Test
    @DisplayName("Si todas las combinaciones se traslapan, devuelve lista vacía")
    void todasTraslapadas() {
        List<Grupo> a = List.of(g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")));
        List<Grupo> b = List.of(g("B", "G1", h(DiaSemana.LUN, "09:30", "10:30")));
        GeneradorHorarios.ResultadoBusqueda r = generadorPorDefecto().generar(List.of(a, b));
        assertTrue(r.opciones().isEmpty());
        assertFalse(r.truncada());
    }

    @Test
    @DisplayName("Respeta el máximo de opciones")
    void respetaMaxOpciones() {
        List<Grupo> a = List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("A", "G2", h(DiaSemana.MAR, "09:00", "10:00")),
                g("A", "G3", h(DiaSemana.MIE, "09:00", "10:00")));
        List<Grupo> b = List.of(g("B", "G1", h(DiaSemana.JUE, "09:00", "10:00")));
        GeneradorHorarios.ResultadoBusqueda r = generador(1, 2, 100).generar(List.of(a, b));
        assertEquals(2, r.opciones().size());
        assertFalse(r.truncada());
    }

    @Test
    @DisplayName("Marca la búsqueda como truncada al alcanzar el tope de candidatas")
    void truncada() {
        List<Grupo> a = List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("A", "G2", h(DiaSemana.MAR, "09:00", "10:00")),
                g("A", "G3", h(DiaSemana.MIE, "09:00", "10:00")));
        List<Grupo> b = List.of(g("B", "G1", h(DiaSemana.JUE, "09:00", "10:00")));
        GeneradorHorarios.ResultadoBusqueda r = generador(1, 2, 2).generar(List.of(a, b));
        assertTrue(r.truncada());
        assertEquals(2, r.opciones().size());
    }

    @Test
    @DisplayName("Las opciones salen ordenadas de la mejor (menos huecos) a la peor")
    void ordenadasPorCalidad() {
        List<Grupo> a = List.of(
                g("A", "A1", h(DiaSemana.LUN, "08:00", "09:00")),   // pegada a B
                g("A", "A2", h(DiaSemana.LUN, "12:00", "13:00")));  // con hueco de 2 h
        List<Grupo> b = List.of(g("B", "B1", h(DiaSemana.LUN, "09:00", "10:00")));
        GeneradorHorarios.ResultadoBusqueda r = generadorPorDefecto().generar(List.of(a, b));

        assertEquals(2, r.opciones().size());
        List<String> clavesMejor = r.opciones().get(0).grupos().stream().map(Grupo::clave).toList();
        assertTrue(clavesMejor.contains("A1"));
        EvaluadorOpcion ev = new EvaluadorOpcion();
        assertTrue(ev.puntaje(r.opciones().get(0)) <= ev.puntaje(r.opciones().get(1)));
    }

    @Test
    @DisplayName("No modifica la lista de entrada (funciona incluso con listas inmutables)")
    void noModificaEntrada() {
        List<Grupo> grande = List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("A", "G2", h(DiaSemana.MAR, "09:00", "10:00")));
        List<Grupo> chica = List.of(g("B", "G1", h(DiaSemana.MIE, "09:00", "10:00")));
        List<List<Grupo>> entrada = List.of(grande, chica); // inmutable: ordenarla in situ fallaría

        generadorPorDefecto().generar(entrada);
        assertEquals(List.of(grande, chica), entrada);
    }

    @Test
    @DisplayName("Con 4 materias y varios grupos, todas las opciones devueltas son válidas")
    void variasMateriasTodasValidas() {
        List<List<Grupo>> materias = new ArrayList<>();
        String[] claves = {"A", "B", "C", "D"};
        DiaSemana[] dias = {DiaSemana.LUN, DiaSemana.MAR, DiaSemana.MIE, DiaSemana.JUE};
        String[] horas = {"07:00", "09:00", "11:00", "15:00"};
        for (String clave : claves) {
            List<Grupo> grupos = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                grupos.add(g(clave, clave + i,
                        h(dias[i], horas[i], plusUnaHora(horas[i])),
                        h(dias[(i + 1) % 4], horas[(i + 2) % 4], plusUnaHora(horas[(i + 2) % 4]))));
            }
            materias.add(grupos);
        }
        GeneradorHorarios.ResultadoBusqueda r = generadorPorDefecto().generar(materias);
        assertFalse(r.opciones().isEmpty());
        assertTrue(r.opciones().size() <= 4);
        r.opciones().forEach(o -> assertOpcionValida(o, 4));
    }

    private static String plusUnaHora(String hora) {
        return LocalTime.parse(hora).plusHours(1).toString();
    }
}
