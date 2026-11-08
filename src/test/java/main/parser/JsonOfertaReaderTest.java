package main.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import main.model.DiaSemana;
import main.model.Grupo;
import main.model.Horario;
import main.model.Materia;
import main.model.OfertaAcademica;

class JsonOfertaReaderTest {

    private final JsonOfertaReader lector = new JsonOfertaReader();

    private static InputStream json(String texto) {
        return new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8));
    }

    private ParserException error(String texto) {
        return assertThrows(ParserException.class, () -> lector.leer(json(texto)));
    }

    /** JSON con una materia, un grupo y un horario; el campo "horario" se reemplaza en cada prueba. */
    private static String conHorario(String horarioJson) {
        return """
                {"materias":[{"clave":"LINC11","nombre":"FISICA","semestre":1,"grupos":[
                  {"clave":"CO01","docente":"DOC","horarios":[%s]}]}]}
                """.formatted(horarioJson);
    }

    private Horario unicoHorario(String horarioJson) throws Exception {
        OfertaAcademica oferta = lector.leer(json(conHorario(horarioJson)));
        return oferta.getMateria("LINC11").grupos().get(0).horarios().get(0);
    }

    // ------------------------------------------------------------------ lectura correcta

    @Test
    @DisplayName("Lee materias, grupos y horarios completos")
    void lecturaCompleta() throws Exception {
        OfertaAcademica oferta = lector.leer(json("""
                {"materias":[
                  {"clave":"LINC11","nombre":"FISICA","semestre":1,"grupos":[
                    {"clave":"CO01","docente":"DRA. QUINTANA","horarios":[
                      {"dia":"LUN","inicio":"09:00","fin":"11:00","aula":"C-303"},
                      {"dia":"MIE","inicio":"09:00","fin":"11:00","aula":"C-304"}]},
                    {"clave":"CO02","docente":"DR. PEREZ","horarios":[
                      {"dia":"MAR","inicio":"13:00","fin":"15:00","aula":"C-303"}]}]},
                  {"clave":"LINC12","nombre":"CALCULO","semestre":2,"grupos":[]}
                ]}
                """));

        assertEquals(2, oferta.materias().size());

        Materia fisica = oferta.getMateria("LINC11");
        assertEquals("FISICA", fisica.nombre());
        assertEquals(1, fisica.semestre());
        assertEquals(2, fisica.grupos().size());

        Grupo co01 = fisica.grupos().get(0);
        assertEquals("CO01", co01.clave());
        assertEquals("LINC11", co01.claveMateria());
        assertEquals("DRA. QUINTANA", co01.docente());
        assertEquals(2, co01.horarios().size());
        assertEquals(new Horario(DiaSemana.LUN, LocalTime.of(9, 0), LocalTime.of(11, 0), "C-303"),
                co01.horarios().get(0));

        assertTrue(oferta.getMateria("LINC12").grupos().isEmpty());
    }

    @Test
    @DisplayName("Una materia sin el campo 'grupos' queda con lista vacía")
    void materiaSinGrupos() throws Exception {
        OfertaAcademica oferta = lector.leer(json("""
                {"materias":[{"clave":"LINC11","nombre":"FISICA"}]}"""));
        assertTrue(oferta.getMateria("LINC11").grupos().isEmpty());
    }

    @Test
    @DisplayName("Un arreglo de materias vacío da una oferta vacía")
    void ofertaVacia() throws Exception {
        assertTrue(lector.leer(json("{\"materias\":[]}")).materias().isEmpty());
    }

    @Test
    @DisplayName("Semestre, docente y aula son opcionales: 0, vacío y \"N/D\"")
    void camposOpcionales() throws Exception {
        OfertaAcademica oferta = lector.leer(json("""
                {"materias":[{"clave":"OPT1","nombre":"OPTATIVA","grupos":[
                  {"clave":"CO01","horarios":[{"dia":"LUN","inicio":"09:00","fin":"10:00"}]}]}]}"""));
        Materia optativa = oferta.getMateria("OPT1");
        assertEquals(0, optativa.semestre());
        assertEquals("", optativa.grupos().get(0).docente());
        assertEquals("N/D", optativa.grupos().get(0).horarios().get(0).aula());
    }

    @Test
    @DisplayName("Un aula vacía también se convierte en \"N/D\"")
    void aulaVacia() throws Exception {
        Horario h = unicoHorario("{\"dia\":\"LUN\",\"inicio\":\"09:00\",\"fin\":\"10:00\",\"aula\":\"  \"}");
        assertEquals("N/D", h.aula());
    }

    @Test
    @DisplayName("Acepta el alias MIER, minúsculas y espacios en el día")
    void aliasDeDia() throws Exception {
        assertEquals(DiaSemana.MIE,
                unicoHorario("{\"dia\":\"MIER\",\"inicio\":\"09:00\",\"fin\":\"10:00\"}").dia());
        assertEquals(DiaSemana.LUN,
                unicoHorario("{\"dia\":\" lun \",\"inicio\":\"09:00\",\"fin\":\"10:00\"}").dia());
        assertEquals(DiaSemana.SAB,
                unicoHorario("{\"dia\":\"sab\",\"inicio\":\"09:00\",\"fin\":\"10:00\"}").dia());
    }

    @Test
    @DisplayName("Acepta horas con uno o dos dígitos en la hora (9:00 y 09:00)")
    void formatosDeHora() throws Exception {
        Horario h = unicoHorario("{\"dia\":\"LUN\",\"inicio\":\"9:00\",\"fin\":\"10:30\"}");
        assertEquals(LocalTime.of(9, 0), h.inicio());
        assertEquals(LocalTime.of(10, 30), h.fin());
    }

    // ------------------------------------------------------------------ errores

    @Test
    @DisplayName("InputStream nulo lanza ParserException")
    void streamNulo() {
        ParserException e = assertThrows(ParserException.class, () -> lector.leer(null));
        assertTrue(e.getMessage().contains("nulo"));
    }

    @Test
    @DisplayName("JSON mal formado lanza ParserException con la causa original")
    void jsonMalFormado() {
        ParserException e = error("{ esto no es json");
        assertNotNull(e.getCause());
    }

    @Test
    @DisplayName("Archivo vacío o sin arreglo 'materias' lanza ParserException")
    void sinArregloMaterias() {
        assertTrue(error("{}").getMessage().contains("materias"));
        assertTrue(error("{\"materias\":\"no es arreglo\"}").getMessage().contains("materias"));
        assertTrue(error("").getMessage().contains("materias"));
    }

    @Test
    @DisplayName("Materia sin clave o sin nombre indica el campo que falta")
    void materiaIncompleta() {
        assertTrue(error("{\"materias\":[{\"nombre\":\"X\"}]}").getMessage().contains("'clave'"));
        assertTrue(error("{\"materias\":[{\"clave\":\"A\"}]}").getMessage().contains("'nombre'"));
        assertTrue(error("{\"materias\":[{\"clave\":\"  \",\"nombre\":\"X\"}]}").getMessage().contains("'clave'"));
    }

    @Test
    @DisplayName("Grupo sin clave indica en qué materia falta")
    void grupoSinClave() {
        ParserException e = error("""
                {"materias":[{"clave":"LINC11","nombre":"FISICA","grupos":[{"docente":"X"}]}]}""");
        assertTrue(e.getMessage().contains("'clave'"));
        assertTrue(e.getMessage().contains("LINC11"));
    }

    @Test
    @DisplayName("Materia repetida lanza ParserException")
    void materiaRepetida() {
        ParserException e = error("""
                {"materias":[{"clave":"A","nombre":"X"},{"clave":"A","nombre":"Y"}]}""");
        assertTrue(e.getMessage().contains("repetida"));
    }

    @Test
    @DisplayName("Día inválido lanza ParserException")
    void diaInvalido() {
        ParserException e = error(conHorario("{\"dia\":\"DOM\",\"inicio\":\"09:00\",\"fin\":\"10:00\"}"));
        assertTrue(e.getMessage().contains("Dia no valido"));
    }

    @Test
    @DisplayName("Horario sin día, inicio o fin indica el campo que falta")
    void horarioIncompleto() {
        assertTrue(error(conHorario("{\"inicio\":\"09:00\",\"fin\":\"10:00\"}")).getMessage().contains("'dia'"));
        assertTrue(error(conHorario("{\"dia\":\"LUN\",\"fin\":\"10:00\"}")).getMessage().contains("'inicio'"));
        assertTrue(error(conHorario("{\"dia\":\"LUN\",\"inicio\":\"09:00\"}")).getMessage().contains("'fin'"));
    }

    @Test
    @DisplayName("Hora mal escrita o inexistente lanza ParserException de horario inválido")
    void horaInvalida() {
        assertTrue(error(conHorario("{\"dia\":\"LUN\",\"inicio\":\"abc\",\"fin\":\"10:00\"}"))
                .getMessage().contains("Horario invalido"));
        assertTrue(error(conHorario("{\"dia\":\"LUN\",\"inicio\":\"25:00\",\"fin\":\"26:00\"}"))
                .getMessage().contains("Horario invalido"));
    }

    @Test
    @DisplayName("Inicio igual o posterior al fin lanza ParserException")
    void inicioNoAnteriorAFin() {
        assertTrue(error(conHorario("{\"dia\":\"LUN\",\"inicio\":\"10:00\",\"fin\":\"10:00\"}"))
                .getMessage().contains("Horario invalido"));
        assertTrue(error(conHorario("{\"dia\":\"LUN\",\"inicio\":\"11:00\",\"fin\":\"09:00\"}"))
                .getMessage().contains("Horario invalido"));
    }
}
