package main.motor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static main.testutil.Datos.alumno;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;

import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.DiaSemana;
import main.model.Grupo;
import main.model.Turno;

class FiltroPreferenciasTest {

    private final FiltroPreferencias filtro = new FiltroPreferencias(LocalTime.of(13, 0));

    @Test
    @DisplayName("Sin preferencias se admite cualquier grupo")
    void sinPreferencias() {
        Grupo grupo = g("A", "G1", h(DiaSemana.LUN, "07:00", "09:00"), h(DiaSemana.VIE, "18:00", "20:00"));
        assertTrue(filtro.admite(grupo, alumno(Set.of(), Set.of())));
    }

    @Test
    @DisplayName("Día libre: rechaza grupos con clase ese día y admite los demás")
    void diaLibre() {
        Grupo conViernes = g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00"), h(DiaSemana.VIE, "09:00", "10:00"));
        Grupo sinViernes = g("A", "G2", h(DiaSemana.LUN, "09:00", "10:00"), h(DiaSemana.MAR, "09:00", "10:00"));
        var alumno = alumno(Set.of(), Set.of(), null, DiaSemana.VIE);

        assertFalse(filtro.admite(conViernes, alumno));
        assertTrue(filtro.admite(sinViernes, alumno));
    }

    @Test
    @DisplayName("Turno mañana: admite clases que empiezan antes del corte")
    void turnoMananaAdmite() {
        Grupo grupo = g("A", "G1", h(DiaSemana.LUN, "09:00", "11:00"), h(DiaSemana.MAR, "12:59", "14:00"));
        assertTrue(filtro.admite(grupo, alumno(Set.of(), Set.of(), Turno.MANANA, null)));
    }

    @Test
    @DisplayName("Turno mañana: rechaza una clase que empieza justo a la hora de corte")
    void turnoMananaRechazaEnElCorte() {
        Grupo grupo = g("A", "G1", h(DiaSemana.LUN, "13:00", "14:00"));
        assertFalse(filtro.admite(grupo, alumno(Set.of(), Set.of(), Turno.MANANA, null)));
    }

    @Test
    @DisplayName("Turno tarde: admite clases que empiezan a la hora de corte o después")
    void turnoTardeAdmite() {
        Grupo grupo = g("A", "G1", h(DiaSemana.LUN, "13:00", "15:00"), h(DiaSemana.MAR, "17:00", "19:00"));
        assertTrue(filtro.admite(grupo, alumno(Set.of(), Set.of(), Turno.TARDE, null)));
    }

    @Test
    @DisplayName("Turno tarde: rechaza una clase que empieza antes del corte")
    void turnoTardeRechaza() {
        Grupo grupo = g("A", "G1", h(DiaSemana.LUN, "12:59", "14:00"));
        assertFalse(filtro.admite(grupo, alumno(Set.of(), Set.of(), Turno.TARDE, null)));
    }

    @Test
    @DisplayName("Un grupo con una clase de mañana y otra de tarde no cumple ningún turno estricto")
    void grupoMixto() {
        Grupo grupo = g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00"), h(DiaSemana.MAR, "15:00", "16:00"));
        assertFalse(filtro.admite(grupo, alumno(Set.of(), Set.of(), Turno.MANANA, null)));
        assertFalse(filtro.admite(grupo, alumno(Set.of(), Set.of(), Turno.TARDE, null)));
    }

    @Test
    @DisplayName("Día libre y turno se combinan: ambos deben cumplirse")
    void combinaDiaYTurno() {
        Grupo grupo = g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00"), h(DiaSemana.VIE, "09:00", "10:00"));
        assertFalse(filtro.admite(grupo, alumno(Set.of(), Set.of(), Turno.MANANA, DiaSemana.VIE)));
        assertTrue(filtro.admite(grupo, alumno(Set.of(), Set.of(), Turno.MANANA, DiaSemana.SAB)));
    }
}
