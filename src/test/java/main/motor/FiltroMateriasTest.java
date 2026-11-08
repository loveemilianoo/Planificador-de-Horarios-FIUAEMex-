package main.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static main.testutil.Datos.alumno;
import static main.testutil.Datos.g;
import static main.testutil.Datos.h;
import static main.testutil.Datos.m;
import static main.testutil.Datos.oferta;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.DiaSemana;
import main.model.Materia;
import main.model.OfertaAcademica;

class FiltroMateriasTest {

    private final OfertaAcademica oferta = oferta(
            m("MAT1", g("MAT1", "G1", h(DiaSemana.LUN, "09:00", "10:00"))),
            m("MAT2", g("MAT2", "G1", h(DiaSemana.MAR, "09:00", "10:00"))),
            m("VACIA"));

    private final FiltroMaterias filtro = new FiltroMaterias(
            new TablaSeriaciones(Map.of("MAT2", Set.of("MAT1"))));

    @Test
    @DisplayName("Una materia existente, no aprobada y sin seriación pendiente es válida")
    void materiaValida() {
        FiltroMaterias.Resultado r = filtro.filtrar(oferta, alumno(Set.of(), Set.of("MAT1")));
        assertEquals(1, r.validas().size());
        assertEquals("MAT1", r.validas().get(0).clave());
        assertTrue(r.descartadas().isEmpty());
    }

    @Test
    @DisplayName("Descarta las materias ya aprobadas")
    void descartaAprobadas() {
        FiltroMaterias.Resultado r = filtro.filtrar(oferta, alumno(Set.of("MAT1"), Set.of("MAT1")));
        assertTrue(r.validas().isEmpty());
        assertEquals("ya está aprobada", r.descartadas().get("MAT1"));
    }

    @Test
    @DisplayName("Descarta las materias que no existen en la oferta")
    void descartaInexistentes() {
        FiltroMaterias.Resultado r = filtro.filtrar(oferta, alumno(Set.of(), Set.of("XYZ")));
        assertTrue(r.validas().isEmpty());
        assertTrue(r.descartadas().get("XYZ").contains("no existe en la oferta"));
    }

    @Test
    @DisplayName("Descarta las materias sin grupos abiertos")
    void descartaSinGrupos() {
        FiltroMaterias.Resultado r = filtro.filtrar(oferta, alumno(Set.of(), Set.of("VACIA")));
        assertTrue(r.validas().isEmpty());
        assertEquals("no tiene grupos abiertos", r.descartadas().get("VACIA"));
    }

    @Test
    @DisplayName("Descarta por seriación no cumplida e indica qué falta aprobar")
    void descartaPorSeriacion() {
        FiltroMaterias.Resultado r = filtro.filtrar(oferta, alumno(Set.of(), Set.of("MAT2")));
        assertTrue(r.validas().isEmpty());
        String motivo = r.descartadas().get("MAT2");
        assertTrue(motivo.contains("seriación no cumplida"));
        assertTrue(motivo.contains("MAT1"));
    }

    @Test
    @DisplayName("Con el prerrequisito aprobado, la materia seriada sí es válida")
    void seriacionCumplida() {
        FiltroMaterias.Resultado r = filtro.filtrar(oferta, alumno(Set.of("MAT1"), Set.of("MAT2")));
        assertEquals(1, r.validas().size());
        assertEquals("MAT2", r.validas().get(0).clave());
    }

    @Test
    @DisplayName("Si se piden la materia y su prerrequisito sin tenerlo aprobado, la seriada se descarta")
    void prerrequisitoPedidoNoCuentaComoAprobado() {
        FiltroMaterias.Resultado r = filtro.filtrar(oferta, alumno(Set.of(), Set.of("MAT1", "MAT2")));
        List<String> claves = r.validas().stream().map(Materia::clave).toList();
        assertEquals(List.of("MAT1"), claves);
        assertTrue(r.descartadas().containsKey("MAT2"));
    }

    @Test
    @DisplayName("El orden de las materias válidas es siempre el mismo (alfabético)")
    void ordenDeterminista() {
        FiltroMaterias.Resultado r = filtro.filtrar(oferta,
                alumno(Set.of("MAT1"), Set.of("MAT2", "MAT1", "VACIA")));
        // MAT1 aprobada, VACIA sin grupos -> solo MAT2 válida
        assertEquals(List.of("MAT2"), r.validas().stream().map(Materia::clave).toList());
        assertEquals(2, r.descartadas().size());
    }

    @Test
    @DisplayName("Sin materias deseadas no hay válidas ni descartadas")
    void sinDeseadas() {
        FiltroMaterias.Resultado r = filtro.filtrar(oferta, alumno(Set.of(), Set.of()));
        assertTrue(r.validas().isEmpty());
        assertTrue(r.descartadas().isEmpty());
    }
}
