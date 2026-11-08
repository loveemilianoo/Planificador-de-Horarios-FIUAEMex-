package main.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.DiaSemana;
import main.model.OpcionHorario;

class EvaluadorOpcionTest {

    private final EvaluadorOpcion evaluador = new EvaluadorOpcion();

    @Test
    @DisplayName("Una opción vacía tiene puntaje 0")
    void opcionVacia() {
        assertEquals(0.0, evaluador.puntaje(new OpcionHorario(List.of())), 0.0001);
    }

    @Test
    @DisplayName("Clases contiguas el mismo día: solo cuenta el peso del día")
    void clasesContiguas() {
        OpcionHorario o = new OpcionHorario(List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("B", "G1", h(DiaSemana.LUN, "10:00", "11:00"))));
        assertEquals(EvaluadorOpcion.PESO_DIA_MINUTOS, evaluador.puntaje(o), 0.0001);
    }

    @Test
    @DisplayName("Un hueco de 2 horas suma 120 minutos al puntaje")
    void huecoMismoDia() {
        OpcionHorario o = new OpcionHorario(List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("B", "G1", h(DiaSemana.LUN, "12:00", "13:00"))));
        assertEquals(120 + EvaluadorOpcion.PESO_DIA_MINUTOS, evaluador.puntaje(o), 0.0001);
    }

    @Test
    @DisplayName("Cada día distinto de asistencia suma el peso del día")
    void diasDistintos() {
        OpcionHorario o = new OpcionHorario(List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("B", "G1", h(DiaSemana.MAR, "09:00", "10:00"))));
        assertEquals(2 * EvaluadorOpcion.PESO_DIA_MINUTOS, evaluador.puntaje(o), 0.0001);
    }

    @Test
    @DisplayName("El orden de los grupos o de los bloques no cambia el puntaje")
    void ordenIrrelevante() {
        OpcionHorario a = new OpcionHorario(List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("B", "G1", h(DiaSemana.LUN, "12:00", "13:00"))));
        OpcionHorario b = new OpcionHorario(List.of(
                g("B", "G1", h(DiaSemana.LUN, "12:00", "13:00")),
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00"))));
        assertEquals(evaluador.puntaje(a), evaluador.puntaje(b), 0.0001);
    }

    @Test
    @DisplayName("Un horario compacto puntúa mejor (menos) que uno con huecos y más días")
    void compactoMejorQueDisperso() {
        OpcionHorario compacto = new OpcionHorario(List.of(
                g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00")),
                g("B", "G1", h(DiaSemana.LUN, "10:00", "11:00"))));
        OpcionHorario disperso = new OpcionHorario(List.of(
                g("A", "G2", h(DiaSemana.LUN, "09:00", "10:00")),
                g("B", "G2", h(DiaSemana.MIE, "16:00", "17:00"))));
        assertTrue(evaluador.puntaje(compacto) < evaluador.puntaje(disperso));
    }
}
