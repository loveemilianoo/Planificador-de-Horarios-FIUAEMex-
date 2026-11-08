package main.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;
import static main.testutil.Datos.m;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OfertaAcademicaTest {

    @Test
    @DisplayName("getMateria devuelve la materia por clave y null si no existe")
    void getMateria() {
        Materia fisica = m("LINC11", g("LINC11", "CO01", h(DiaSemana.LUN, "09:00", "10:00")));
        OfertaAcademica oferta = new OfertaAcademica(Map.of("LINC11", fisica));

        assertSame(fisica, oferta.getMateria("LINC11"));
        assertNull(oferta.getMateria("NOEXISTE"));
    }

    @Test
    @DisplayName("Rechaza un mapa de materias nulo")
    void rechazaMapaNulo() {
        assertThrows(NullPointerException.class, () -> new OfertaAcademica(null));
    }

    @Test
    @DisplayName("El mapa es inmutable y es copia defensiva")
    void mapaInmutable() {
        Map<String, Materia> original = new HashMap<>();
        original.put("A", m("A"));
        OfertaAcademica oferta = new OfertaAcademica(original);

        original.put("B", m("B")); // no debe afectar a la oferta
        assertEquals(1, oferta.materias().size());
        assertThrows(UnsupportedOperationException.class, () -> oferta.materias().put("C", m("C")));
    }
}
