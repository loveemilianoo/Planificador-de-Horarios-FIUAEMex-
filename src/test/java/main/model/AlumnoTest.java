package main.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlumnoTest {

    @Test
    @DisplayName("Conserva materias, preferencias y semestre")
    void conservaDatos() {
        Alumno a = new Alumno(Set.of("A"), Set.of("B", "C"), Turno.MANANA, DiaSemana.VIE, 3);
        assertEquals(Set.of("A"), a.aprobadas());
        assertEquals(Set.of("B", "C"), a.deseadas());
        assertEquals(Turno.MANANA, a.turnoPreferido());
        assertEquals(DiaSemana.VIE, a.diaLibre());
        assertEquals(3, a.semestre());
    }

    @Test
    @DisplayName("Conjuntos nulos se convierten en conjuntos vacíos")
    void conjuntosNulosQuedanVacios() {
        Alumno a = new Alumno(null, null, null, null, 2);
        assertTrue(a.aprobadas().isEmpty());
        assertTrue(a.deseadas().isEmpty());
    }

    @Test
    @DisplayName("Las preferencias pueden ser nulas (sin restricción)")
    void preferenciasOpcionales() {
        Alumno a = new Alumno(Set.of(), Set.of(), null, null, 2);
        assertNull(a.turnoPreferido());
        assertNull(a.diaLibre());
    }

    @Test
    @DisplayName("Los conjuntos son inmutables y son copia defensiva")
    void conjuntosInmutables() {
        Set<String> original = new HashSet<>(Set.of("A"));
        Alumno a = new Alumno(original, Set.of(), null, null, 2);

        original.add("Z"); // no debe afectar al alumno
        assertEquals(Set.of("A"), a.aprobadas());
        assertThrows(UnsupportedOperationException.class, () -> a.aprobadas().add("Y"));
        assertThrows(UnsupportedOperationException.class, () -> a.deseadas().add("Y"));
    }
}
