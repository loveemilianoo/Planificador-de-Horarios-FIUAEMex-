package main.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TablaSeriacionesTest {

    private final TablaSeriaciones tabla = new TablaSeriaciones(Map.of(
            "CALC2", Set.of("CALC1"),
            "CALC3", Set.of("CALC1", "CALC2")));

    @Test
    @DisplayName("Una materia sin seriación no tiene prerrequisitos y siempre se cumple")
    void materiaSinSeriacion() {
        assertTrue(tabla.prerrequisitosDe("FISICA").isEmpty());
        assertTrue(tabla.cumple("FISICA", Set.of()));
    }

    @Test
    @DisplayName("La tabla vacía no impone ninguna seriación")
    void tablaVacia() {
        assertTrue(TablaSeriaciones.vacia().cumple("CUALQUIERA", Set.of()));
    }

    @Test
    @DisplayName("prerrequisitosDe devuelve los prerrequisitos registrados")
    void prerrequisitosDe() {
        assertEquals(Set.of("CALC1", "CALC2"), tabla.prerrequisitosDe("CALC3"));
    }

    @Test
    @DisplayName("cumple es false si falta algún prerrequisito y true si están todos")
    void cumple() {
        assertFalse(tabla.cumple("CALC3", Set.of("CALC1")));
        assertFalse(tabla.cumple("CALC2", Set.of()));
        assertTrue(tabla.cumple("CALC3", Set.of("CALC1", "CALC2")));
        assertTrue(tabla.cumple("CALC2", Set.of("CALC1", "OTRA")));
    }

    @Test
    @DisplayName("faltantes devuelve solo lo que no se ha aprobado, en orden alfabético")
    void faltantes() {
        assertEquals(List.of("CALC1", "CALC2"), List.copyOf(tabla.faltantes("CALC3", Set.of())));
        assertEquals(List.of("CALC2"), List.copyOf(tabla.faltantes("CALC3", Set.of("CALC1"))));
        assertTrue(tabla.faltantes("CALC3", Set.of("CALC1", "CALC2")).isEmpty());
    }

    @Test
    @DisplayName("La tabla es inmutable: cambiar el mapa original no la afecta")
    void copiaDefensiva() {
        Set<String> reqs = new HashSet<>(Set.of("A"));
        Map<String, Set<String>> original = new HashMap<>();
        original.put("B", reqs);
        TablaSeriaciones t = new TablaSeriaciones(original);

        reqs.add("Z");
        original.put("C", Set.of("A"));
        assertEquals(Set.of("A"), t.prerrequisitosDe("B"));
        assertTrue(t.prerrequisitosDe("C").isEmpty());
    }

    @Test
    @DisplayName("Los prerrequisitos devueltos no se pueden modificar")
    void prerrequisitosInmutables() {
        assertThrows(UnsupportedOperationException.class,
                () -> tabla.prerrequisitosDe("CALC2").add("X"));
    }

    @Test
    @DisplayName("Rechaza un mapa nulo")
    void rechazaNulo() {
        assertThrows(NullPointerException.class, () -> new TablaSeriaciones(null));
    }
}
