package main.parser;

import static main.testutil.Datos.m;
import static main.testutil.Datos.oferta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.OfertaAcademica;
import main.motor.TablaSeriaciones;

class SeriacionesCsvReaderTest {

    private final SeriacionesCsvReader lector = new SeriacionesCsvReader();

    private static InputStream csv(String texto) {
        return new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Lee las seriaciones, omitiendo comentarios, líneas vacías y encabezado")
    void leeSeriaciones() throws Exception {
        OfertaAcademica oferta = oferta(m("A"), m("B"), m("C"));
        TablaSeriaciones tabla = lector.leer(csv("""
                # comentario
                clave,prerrequisito

                B,A
                C,A
                C,B
                """), oferta);

        assertEquals(Set.of("A"), tabla.prerrequisitosDe("B"));
        assertEquals(Set.of("A", "B"), tabla.prerrequisitosDe("C"));
        assertEquals(Set.of(), tabla.prerrequisitosDe("A"));
        assertEquals(List.of(), lector.getAdvertencias());
    }

    @Test
    @DisplayName("Tolera la marca BOM y espacios alrededor de las claves")
    void toleraBomYEspacios() throws Exception {
        OfertaAcademica oferta = oferta(m("A"), m("B"));
        TablaSeriaciones tabla = lector.leer(csv("﻿clave,prerrequisito\n B , A \n"), oferta);
        assertEquals(Set.of("A"), tabla.prerrequisitosDe("B"));
    }

    @Test
    @DisplayName("Ignora con advertencia las filas de materias que no están en la oferta")
    void ignoraMateriasFueraDeLaOferta() throws Exception {
        OfertaAcademica oferta = oferta(m("A"), m("B"));
        TablaSeriaciones tabla = lector.leer(csv("""
                clave,prerrequisito
                B,A
                X,A
                """), oferta);

        assertEquals(Set.of("A"), tabla.prerrequisitosDe("B"));
        assertEquals(Set.of(), tabla.prerrequisitosDe("X"));
        assertEquals(1, lector.getAdvertencias().size());
        assertTrue(lector.getAdvertencias().get(0).contains("línea 3"));
        assertTrue(lector.getAdvertencias().get(0).contains("X"));
    }

    @Test
    @DisplayName("Las advertencias se limpian en cada lectura")
    void advertenciasSeLimpian() throws Exception {
        OfertaAcademica oferta = oferta(m("A"));
        lector.leer(csv("clave,prerrequisito\nX,A\n"), oferta);
        assertEquals(1, lector.getAdvertencias().size());
        lector.leer(csv("clave,prerrequisito\n"), oferta);
        assertEquals(List.of(), lector.getAdvertencias());
    }

    @Test
    @DisplayName("Falla si el prerrequisito de una materia de la oferta no existe en la oferta")
    void prerrequisitoDesconocido() {
        OfertaAcademica oferta = oferta(m("B"));
        ParserException e = assertThrows(ParserException.class, () -> lector.leer(csv("""
                clave,prerrequisito
                B,A
                """), oferta));
        assertTrue(e.getMessage().contains("línea 2"), e.getMessage());
        assertTrue(e.getMessage().contains("A") && e.getMessage().contains("B"), e.getMessage());
    }

    @Test
    @DisplayName("Falla con el número de línea si la fila no tiene 2 columnas")
    void filaConColumnasIncorrectas() {
        OfertaAcademica oferta = oferta(m("A"), m("B"));
        ParserException e = assertThrows(ParserException.class,
                () -> lector.leer(csv("clave,prerrequisito\nB,A,extra\n"), oferta));
        assertTrue(e.getMessage().contains("línea 2") && e.getMessage().contains("2 columnas"), e.getMessage());
    }

    @Test
    @DisplayName("Falla si falta la clave o el prerrequisito")
    void filaIncompleta() {
        OfertaAcademica oferta = oferta(m("A"), m("B"));
        ParserException e = assertThrows(ParserException.class,
                () -> lector.leer(csv("clave,prerrequisito\nB,\n"), oferta));
        assertTrue(e.getMessage().contains("línea 2"), e.getMessage());
    }

    @Test
    @DisplayName("Falla con un mensaje claro si el InputStream es nulo")
    void streamNulo() {
        ParserException e = assertThrows(ParserException.class, () -> lector.leer(null, oferta(m("A"))));
        assertTrue(e.getMessage().contains("nulo"));
    }

    @Test
    @DisplayName("El seriaciones.csv del repositorio se valida contra la plantilla real 2026A")
    void csvRealContraPlantillaReal() throws Exception {
        OfertaAcademica real;
        try (InputStream in = Files.newInputStream(Path.of("Plantilla_ICO_2026A_Normalizada.xlsx"))) {
            real = new ExcelOfertaReader().leer(in);
        }
        TablaSeriaciones tabla;
        try (InputStream in = SeriacionesCsvReader.class.getResourceAsStream("/datos/seriaciones.csv")) {
            assertTrue(in != null, "falta src/main/resources/datos/seriaciones.csv");
            tabla = lector.leer(in, real);
        }

        // Las tres seriaciones cuyas dos materias están en la plantilla
        assertEquals(Set.of("LINC15"), tabla.prerrequisitosDe("LINC35"));
        assertEquals(Set.of("LINC32"), tabla.prerrequisitosDe("LINC33"));
        assertEquals(Set.of("LINC20"), tabla.prerrequisitosDe("LINC21"));
        // La plantilla solo trae 12 materias: el resto de las filas del CSV se ignora con advertencia
        assertEquals(Set.of(), tabla.prerrequisitosDe("LINC07"));
        assertEquals(7, lector.getAdvertencias().size(), lector.getAdvertencias().toString());
    }
}
