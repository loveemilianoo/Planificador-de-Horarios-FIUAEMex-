package main.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpcionHorarioTest {

    @Test
    @DisplayName("Rechaza una lista de grupos nula")
    void rechazaNulo() {
        assertThrows(NullPointerException.class, () -> new OpcionHorario(null));
    }

    @Test
    @DisplayName("Conserva los grupos y hace copia defensiva inmutable")
    void copiaDefensiva() {
        List<Grupo> original = new ArrayList<>();
        original.add(g("MAT1", "G1", h(DiaSemana.LUN, "09:00", "10:00")));
        OpcionHorario opcion = new OpcionHorario(original);

        original.clear(); // no debe afectar a la opción
        assertEquals(1, opcion.grupos().size());
        assertThrows(UnsupportedOperationException.class, () -> opcion.grupos().clear());
    }
}
