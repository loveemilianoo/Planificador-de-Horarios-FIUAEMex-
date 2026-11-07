package main.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.DiaSemana;
import main.model.OpcionHorario;

class ResultadoGeneracionTest {

    @Test
    @DisplayName("hayOpciones es false con lista vacía y true con al menos una opción")
    void hayOpciones() {
        assertFalse(new ResultadoGeneracion(List.of(), Map.of(), List.of()).hayOpciones());

        OpcionHorario o = new OpcionHorario(List.of(g("A", "G1", h(DiaSemana.LUN, "09:00", "10:00"))));
        assertTrue(new ResultadoGeneracion(List.of(o), Map.of(), List.of()).hayOpciones());
    }

    @Test
    @DisplayName("Las colecciones son inmutables y copia defensiva")
    void inmutable() {
        List<String> avisos = new ArrayList<>(List.of("a"));
        Map<String, String> descartadas = new HashMap<>(Map.of("X", "motivo"));
        ResultadoGeneracion r = new ResultadoGeneracion(new ArrayList<>(), descartadas, avisos);

        avisos.add("b");
        descartadas.put("Y", "otro");
        assertEquals(List.of("a"), r.avisos());
        assertEquals(Map.of("X", "motivo"), r.materiasDescartadas());
        assertThrows(UnsupportedOperationException.class, () -> r.avisos().add("c"));
        assertThrows(UnsupportedOperationException.class, () -> r.opciones().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.materiasDescartadas().clear());
    }
}
