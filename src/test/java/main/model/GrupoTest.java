package main.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static main.testutil.Datos.h;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GrupoTest {

    @Test
    @DisplayName("Conserva clave, materia, docente y horarios")
    void conservaDatos() {
        Horario horario = h(DiaSemana.LUN, "09:00", "10:00");
        Grupo g = new Grupo("CO01", "LINC11", "DOCENTE", List.of(horario));
        assertEquals("CO01", g.clave());
        assertEquals("LINC11", g.claveMateria());
        assertEquals("DOCENTE", g.docente());
        assertEquals(List.of(horario), g.horarios());
    }

    @Test
    @DisplayName("Horarios nulos se convierten en lista vacía")
    void horariosNulosQuedanVacios() {
        Grupo g = new Grupo("CO01", "LINC11", "DOCENTE", null);
        assertTrue(g.horarios().isEmpty());
    }

    @Test
    @DisplayName("Rechaza clave o claveMateria nulas")
    void rechazaClavesNulas() {
        assertThrows(NullPointerException.class, () -> new Grupo(null, "LINC11", "D", List.of()));
        assertThrows(NullPointerException.class, () -> new Grupo("CO01", null, "D", List.of()));
    }

    @Test
    @DisplayName("La lista de horarios es inmutable y es una copia defensiva")
    void horariosInmutables() {
        List<Horario> original = new ArrayList<>();
        original.add(h(DiaSemana.LUN, "09:00", "10:00"));
        Grupo g = new Grupo("CO01", "LINC11", "D", original);

        original.add(h(DiaSemana.MAR, "09:00", "10:00")); // no debe afectar al grupo
        assertEquals(1, g.horarios().size());
        assertThrows(UnsupportedOperationException.class,
                () -> g.horarios().add(h(DiaSemana.MIE, "09:00", "10:00")));
    }
}
