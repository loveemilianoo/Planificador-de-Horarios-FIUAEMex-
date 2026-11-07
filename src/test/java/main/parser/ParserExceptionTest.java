package main.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParserExceptionTest {

    @Test
    @DisplayName("Conserva el mensaje y no tiene causa")
    void soloMensaje() {
        ParserException e = new ParserException("archivo inválido");
        assertEquals("archivo inválido", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    @DisplayName("Conserva el mensaje y la causa original")
    void mensajeYCausa() {
        Throwable causa = new IllegalStateException("original");
        ParserException e = new ParserException("falló la lectura", causa);
        assertEquals("falló la lectura", e.getMessage());
        assertSame(causa, e.getCause());
    }

    @Test
    @DisplayName("Es una excepción verificada (obliga a manejarla)")
    void esVerificada() {
        assertTrue(Exception.class.isAssignableFrom(ParserException.class));
        assertFalse(RuntimeException.class.isAssignableFrom(ParserException.class));
    }
}
