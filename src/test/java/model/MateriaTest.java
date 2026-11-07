package main.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MateriaTest {

    @Test
    @DisplayName("Conserva clave, nombre, semestre y grupos")
    void conservaDatos() {
        Grupo grupo = g("LINC11", "CO01", h(DiaSemana.LUN, "09:00", "10:00"));
        Materia m = new Materia("LINC11", "FISICA", 1, List.of(grupo));
        assertEquals("LINC11", m.clave());
        assertEquals("FISICA", m.nombre());
        assertEquals(1, m.semestre());
        assertEquals(List.of(grupo), m.grupos());
    }

    @Test
    @DisplayName("Grupos nulos se convierten en lista vacía")
    void gruposNulosQuedanVacios() {
        assertTrue(new Materia("LINC11", "FISICA", 1, null).grupos().isEmpty());
    }

    @Test
    @DisplayName("Rechaza clave o nombre nulos")
    void rechazaCamposNulos() {
        assertThrows(NullPointerException.class, () -> new Materia(null, "FISICA", 1, List.of()));
        assertThrows(NullPointerException.class, () -> new Materia("LINC11", null, 1, List.of()));
    }

    @Test
    @DisplayName("La lista de grupos es inmutable y es una copia defensiva")
    void gruposInmutables() {
        List<Grupo> original = new ArrayList<>();
        original.add(g("LINC11", "CO01", h(DiaSemana.LUN, "09:00", "10:00")));
        Materia m = new Materia("LINC11", "FISICA", 1, original);

        original.add(g("LINC11", "CO02", h(DiaSemana.MAR, "09:00", "10:00")));
        assertEquals(1, m.grupos().size());
        assertThrows(UnsupportedOperationException.class, () -> m.grupos().clear());
    }
}
