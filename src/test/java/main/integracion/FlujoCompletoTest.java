package main.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.Alumno;
import main.model.DiaSemana;
import main.model.Grupo;
import main.model.OfertaAcademica;
import main.motor.DetectorTraslapes;
import main.motor.MotorHorarios;
import main.motor.ResultadoGeneracion;
import main.motor.TablaSeriaciones;
import main.parser.JsonOfertaReader;

/** Prueba de punta a punta del backend: JSON de oferta -> parser -> motor -> opciones de horario. */
class FlujoCompletoTest {

    private static final String OFERTA_JSON = """
            {"materias":[
              {"clave":"LINC11","nombre":"FISICA","semestre":2,"grupos":[
                {"clave":"CO01","docente":"DOC A","horarios":[{"dia":"LUN","inicio":"09:00","fin":"11:00","aula":"C-303"}]},
                {"clave":"CO02","docente":"DOC B","horarios":[{"dia":"MAR","inicio":"09:00","fin":"11:00","aula":"C-303"}]}]},
              {"clave":"LINC12","nombre":"CALCULO","semestre":2,"grupos":[
                {"clave":"CO01","docente":"DOC C","horarios":[{"dia":"LUN","inicio":"10:00","fin":"12:00"}]},
                {"clave":"CO02","docente":"DOC D","horarios":[{"dia":"MIE","inicio":"10:00","fin":"12:00"}]}]},
              {"clave":"LINC13","nombre":"ALGEBRA","semestre":3,"grupos":[
                {"clave":"CO01","docente":"DOC E","horarios":[{"dia":"JUE","inicio":"09:00","fin":"11:00"}]}]}
            ]}
            """;

    private OfertaAcademica cargarOferta() throws Exception {
        return new JsonOfertaReader().leer(
                new ByteArrayInputStream(OFERTA_JSON.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("JSON -> parser -> motor produce opciones válidas sin traslapes")
    void flujoFeliz() throws Exception {
        OfertaAcademica oferta = cargarOferta();
        Alumno alumno = new Alumno(Set.of(), Set.of("LINC11", "LINC12"), null, null, 2);

        ResultadoGeneracion r = new MotorHorarios(TablaSeriaciones.vacia()).generar(oferta, alumno);

        // FISICA CO01 (lun 9-11) choca con CALCULO CO01 (lun 10-12): quedan 3 de las 4 combinaciones
        assertEquals(3, r.opciones().size());
        r.opciones().forEach(o -> {
            assertEquals(2, o.grupos().size());
            assertFalse(DetectorTraslapes.traslapan(o.grupos().get(0), o.grupos().get(1)));
        });
    }

    @Test
    @DisplayName("La seriación y las preferencias se aplican sobre los datos leídos del JSON")
    void seriacionYPreferencias() throws Exception {
        OfertaAcademica oferta = cargarOferta();
        TablaSeriaciones seriaciones = new TablaSeriaciones(Map.of("LINC13", Set.of("LINC12")));
        // Quiere ALGEBRA (seriada con CALCULO, no aprobado) y FISICA sin clases el lunes
        Alumno alumno = new Alumno(Set.of(), Set.of("LINC11", "LINC13"), null, DiaSemana.LUN, 3);

        ResultadoGeneracion r = new MotorHorarios(seriaciones).generar(oferta, alumno);

        assertTrue(r.materiasDescartadas().get("LINC13").contains("seriación no cumplida"));
        assertEquals(1, r.opciones().size());
        List<Grupo> grupos = r.opciones().get(0).grupos();
        assertEquals(1, grupos.size());
        assertEquals("CO02", grupos.get(0).clave()); // el CO01 tiene clase el lunes
    }
}
