package main.motor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.DiaSemana;
import main.model.Grupo;

class DetectorTraslapesTest {

    @Test
    @DisplayName("Bloques que se cruzan el mismo día se traslapan")
    void bloquesCruzados() {
        assertTrue(DetectorTraslapes.traslapan(
                h(DiaSemana.LUN, "09:00", "11:00"), h(DiaSemana.LUN, "10:00", "12:00")));
        assertTrue(DetectorTraslapes.traslapan(
                h(DiaSemana.LUN, "10:00", "12:00"), h(DiaSemana.LUN, "09:00", "11:00")));
    }

    @Test
    @DisplayName("Bloques contiguos (uno termina cuando empieza el otro) NO se traslapan")
    void bloquesContiguos() {
        assertFalse(DetectorTraslapes.traslapan(
                h(DiaSemana.LUN, "09:00", "10:00"), h(DiaSemana.LUN, "10:00", "11:00")));
        assertFalse(DetectorTraslapes.traslapan(
                h(DiaSemana.LUN, "10:00", "11:00"), h(DiaSemana.LUN, "09:00", "10:00")));
    }

    @Test
    @DisplayName("La misma hora en días distintos no se traslapa")
    void diasDistintos() {
        assertFalse(DetectorTraslapes.traslapan(
                h(DiaSemana.LUN, "09:00", "11:00"), h(DiaSemana.MAR, "09:00", "11:00")));
    }

    @Test
    @DisplayName("Un bloque contenido dentro de otro se traslapa")
    void bloqueContenido() {
        assertTrue(DetectorTraslapes.traslapan(
                h(DiaSemana.VIE, "08:00", "12:00"), h(DiaSemana.VIE, "09:00", "10:00")));
    }

    @Test
    @DisplayName("Bloques idénticos se traslapan")
    void bloquesIdenticos() {
        assertTrue(DetectorTraslapes.traslapan(
                h(DiaSemana.MIE, "09:00", "10:00"), h(DiaSemana.MIE, "09:00", "10:00")));
    }

    @Test
    @DisplayName("Dos grupos se traslapan si algún bloque de uno cruza algún bloque del otro")
    void gruposConBloqueCruzado() {
        Grupo a = g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00"), h(DiaSemana.MIE, "09:00", "10:00"));
        Grupo b = g("B", "G1", h(DiaSemana.MAR, "09:00", "10:00"), h(DiaSemana.MIE, "09:30", "10:30"));
        assertTrue(DetectorTraslapes.traslapan(a, b));
    }

    @Test
    @DisplayName("Dos grupos sin bloques cruzados no se traslapan")
    void gruposSinCruce() {
        Grupo a = g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00"), h(DiaSemana.MIE, "09:00", "10:00"));
        Grupo b = g("B", "G1", h(DiaSemana.MAR, "09:00", "10:00"), h(DiaSemana.MIE, "10:00", "11:00"));
        assertFalse(DetectorTraslapes.traslapan(a, b));
    }

    @Test
    @DisplayName("Un grupo sin horarios nunca se traslapa")
    void grupoSinHorarios() {
        Grupo vacio = g("A", "G1");
        Grupo otro = g("B", "G1", h(DiaSemana.LUN, "09:00", "10:00"));
        assertFalse(DetectorTraslapes.traslapan(vacio, otro));
        assertFalse(DetectorTraslapes.traslapan(otro, vacio));
    }

    @Test
    @DisplayName("traslapaConAlguno: lista vacía no traslapa; detecta cruce con cualquiera de la lista")
    void traslapaConAlguno() {
        Grupo candidato = g("C", "G1", h(DiaSemana.LUN, "10:00", "11:00"));
        Grupo libre = g("A", "G1", h(DiaSemana.MAR, "10:00", "11:00"));
        Grupo choca = g("B", "G1", h(DiaSemana.LUN, "10:30", "11:30"));

        assertFalse(DetectorTraslapes.traslapaConAlguno(candidato, List.of()));
        assertFalse(DetectorTraslapes.traslapaConAlguno(candidato, List.of(libre)));
        assertTrue(DetectorTraslapes.traslapaConAlguno(candidato, List.of(libre, choca)));
    }
}
