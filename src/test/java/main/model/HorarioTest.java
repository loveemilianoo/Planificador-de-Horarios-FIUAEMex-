package main.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HorarioTest {

    @Test
    @DisplayName("Crea un horario válido y conserva sus datos")
    void creaHorarioValido() {
        Horario h = new Horario(DiaSemana.LUN, LocalTime.of(9, 0), LocalTime.of(11, 0), "C-303");
        assertEquals(DiaSemana.LUN, h.dia());
        assertEquals(LocalTime.of(9, 0), h.inicio());
        assertEquals(LocalTime.of(11, 0), h.fin());
        assertEquals("C-303", h.aula());
    }

    @Test
    @DisplayName("Rechaza inicio igual o posterior al fin")
    void rechazaInicioNoAnteriorAFin() {
        assertThrows(IllegalArgumentException.class,
                () -> new Horario(DiaSemana.LUN, LocalTime.of(10, 0), LocalTime.of(10, 0), "A"));
        assertThrows(IllegalArgumentException.class,
                () -> new Horario(DiaSemana.LUN, LocalTime.of(11, 0), LocalTime.of(10, 0), "A"));
    }

    @Test
    @DisplayName("Rechaza día, inicio o fin nulos")
    void rechazaCamposNulos() {
        assertThrows(NullPointerException.class,
                () -> new Horario(null, LocalTime.of(9, 0), LocalTime.of(10, 0), "A"));
        assertThrows(NullPointerException.class,
                () -> new Horario(DiaSemana.LUN, null, LocalTime.of(10, 0), "A"));
        assertThrows(NullPointerException.class,
                () -> new Horario(DiaSemana.LUN, LocalTime.of(9, 0), null, "A"));
    }

    @Test
    @DisplayName("Dos horarios con los mismos datos son iguales")
    void igualdadPorValor() {
        Horario a = new Horario(DiaSemana.MAR, LocalTime.of(9, 0), LocalTime.of(10, 0), "A");
        Horario b = new Horario(DiaSemana.MAR, LocalTime.of(9, 0), LocalTime.of(10, 0), "A");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
