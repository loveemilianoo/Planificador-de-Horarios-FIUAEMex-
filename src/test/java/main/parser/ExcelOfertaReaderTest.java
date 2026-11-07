package main.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import main.model.DiaSemana;
import main.model.Grupo;
import main.model.Horario;
import main.model.Materia;
import main.model.OfertaAcademica;

/**
 * Las pruebas arman en memoria un Excel con la misma estructura que la plantilla de la UAEMex:
 * título (fila 1), encabezados (fila 2), subtítulos INI/FIN/AULA (fila 3) y datos desde la fila 4.
 */
class ExcelOfertaReaderTest {

    // Posiciones de columna del libro de prueba
    private static final int SEM = 0, CLAVE = 1, NOMBRE = 2, EQUIV = 3, GPO = 4, DOCENTE = 5;
    private static final int LUN = 6, MAR = 9, MIE = 12, JUE = 15, VIE = 18, SAB = 21, CLAVE_VIEJA = 24;
    private static final String HOJA = "Horarios";

    private final ExcelOfertaReader lector = new ExcelOfertaReader();

    // ------------------------------------------------------------------ utilidades para armar libros

    private static XSSFWorkbook nuevoLibro(boolean conDocente) {
        XSSFWorkbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet(HOJA);
        hoja.createRow(0).createCell(0).setCellValue("UNIVERSIDAD AUTONOMA DEL ESTADO DE MEXICO");

        Row enc = hoja.createRow(1);
        enc.createCell(SEM).setCellValue("Sem");
        enc.createCell(CLAVE).setCellValue("CLAVE");
        enc.createCell(NOMBRE).setCellValue("UNIDAD DE APRENDIZAJE");
        enc.createCell(EQUIV).setCellValue("GRUPOS EQUIVALENTES ENTRE PLANES"); // no debe confundirse con GPO
        enc.createCell(GPO).setCellValue("GPO");
        if (conDocente) enc.createCell(DOCENTE).setCellValue("DOCENTE");
        enc.createCell(LUN).setCellValue("LUNES");
        enc.createCell(MAR).setCellValue("MARTES");
        enc.createCell(MIE).setCellValue("MIÉRCOLES");
        enc.createCell(JUE).setCellValue("JUEVES");
        enc.createCell(VIE).setCellValue("VIERNES");
        enc.createCell(SAB).setCellValue("SÁBADO");
        enc.createCell(CLAVE_VIEJA).setCellValue("CLAVE"); // segunda CLAVE (plan 2004): se ignora

        Row sub = hoja.createRow(2);
        for (int dia : new int[] {LUN, MAR, MIE, JUE, VIE, SAB}) {
            sub.createCell(dia).setCellValue("INI");
            sub.createCell(dia + 1).setCellValue("FIN");
            sub.createCell(dia + 2).setCellValue("AULA");
        }
        return libro;
    }

    private static void poner(Row fila, int col, String valor) {
        if (valor != null) fila.createCell(col).setCellValue(valor);
    }

    /** Agrega una fila de datos al final de la hoja (la primera queda en la fila 4 de Excel). */
    private static Row fila(XSSFWorkbook libro, String sem, String clave, String nombre, String gpo, String docente) {
        Sheet hoja = libro.getSheet(HOJA);
        Row fila = hoja.createRow(hoja.getLastRowNum() + 1);
        poner(fila, SEM, sem);
        poner(fila, CLAVE, clave);
        poner(fila, NOMBRE, nombre);
        poner(fila, EQUIV, "EQ-99");
        poner(fila, GPO, gpo);
        poner(fila, DOCENTE, docente);
        poner(fila, CLAVE_VIEJA, "VIEJA");
        return fila;
    }

    private static void clase(Row fila, int colDia, String inicio, String fin, String aula) {
        poner(fila, colDia, inicio);
        poner(fila, colDia + 1, fin);
        poner(fila, colDia + 2, aula);
    }

    /** Serializa el libro a bytes .xlsx y lo cierra. */
    private static byte[] bytes(XSSFWorkbook libro) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        libro.write(salida);
        libro.close();
        return salida.toByteArray();
    }

    private static InputStream aStream(XSSFWorkbook libro) throws IOException {
        return new ByteArrayInputStream(bytes(libro));
    }

    private boolean hayAdvertencia(String fragmento) {
        return lector.getAdvertencias().stream().anyMatch(a -> a.contains(fragmento));
    }

    // ------------------------------------------------------------------ lectura correcta

    @Test
    @DisplayName("Lee clave, nombre, semestre, grupo, docente y horarios")
    void lecturaCompleta() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        Row f = fila(libro, "2", "LINC11", "FISICA", "CO01", "DRA. QUINTANA");
        clase(f, LUN, "09:00", "11:00", "C-303");
        clase(f, MIE, "09:00", "11:00", "C-304");

        OfertaAcademica oferta = lector.leer(aStream(libro));

        Materia fisica = oferta.getMateria("LINC11");
        assertNotNull(fisica);
        assertEquals("FISICA", fisica.nombre());
        assertEquals(2, fisica.semestre());

        Grupo grupo = fisica.grupos().get(0);
        assertEquals("CO01", grupo.clave());          // viene de GPO, no de "grupos equivalentes"
        assertEquals("LINC11", grupo.claveMateria()); // viene de la primera CLAVE, no de la del plan 2004
        assertEquals("DRA. QUINTANA", grupo.docente());
        assertEquals(2, grupo.horarios().size());
        assertEquals(new Horario(DiaSemana.LUN, LocalTime.of(9, 0), LocalTime.of(11, 0), "C-303"),
                grupo.horarios().get(0));
        assertEquals(new Horario(DiaSemana.MIE, LocalTime.of(9, 0), LocalTime.of(11, 0), "C-304"),
                grupo.horarios().get(1));
        assertTrue(lector.getAdvertencias().isEmpty());
    }

    @Test
    @DisplayName("Las filas con la misma clave se agrupan en una sola materia, en orden")
    void agrupaFilasPorClave() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");
        clase(fila(libro, "2", "LINC11", "FISICA", "CO02", "B"), MAR, "09:00", "10:00", "X");
        clase(fila(libro, "2", "LINC12", "CALCULO", "CO01", "C"), MIE, "09:00", "10:00", "X");

        OfertaAcademica oferta = lector.leer(aStream(libro));

        assertEquals(2, oferta.materias().size());
        List<Grupo> grupos = oferta.getMateria("LINC11").grupos();
        assertEquals(2, grupos.size());
        assertEquals("CO01", grupos.get(0).clave());
        assertEquals("CO02", grupos.get(1).clave());
        assertEquals(1, oferta.getMateria("LINC12").grupos().size());
    }

    @Test
    @DisplayName("Acepta horas escritas como texto: 9:00, 09:00 y 09:00:00")
    void horasComoTexto() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        Row f = fila(libro, "2", "LINC11", "FISICA", "CO01", "A");
        clase(f, LUN, "9:00", "10:00", "X");
        clase(f, MAR, "09:00:00", "10:30:00", "X");

        Grupo grupo = lector.leer(aStream(libro)).getMateria("LINC11").grupos().get(0);
        assertEquals(LocalTime.of(9, 0), grupo.horarios().get(0).inicio());
        assertEquals(LocalTime.of(9, 0), grupo.horarios().get(1).inicio());
        assertEquals(LocalTime.of(10, 30), grupo.horarios().get(1).fin());
        assertTrue(lector.getAdvertencias().isEmpty());
    }

    @Test
    @DisplayName("Acepta celdas de hora reales de Excel (fracción del día)")
    void horasNumericas() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("h:mm"));
        Row f = fila(libro, "2", "LINC11", "FISICA", "CO01", "A");
        Cell inicio = f.createCell(LUN);
        inicio.setCellValue(9.0 / 24);
        inicio.setCellStyle(estilo);
        Cell fin = f.createCell(LUN + 1);
        fin.setCellValue(11.0 / 24);
        fin.setCellStyle(estilo);

        Horario h = lector.leer(aStream(libro)).getMateria("LINC11").grupos().get(0).horarios().get(0);
        assertEquals(LocalTime.of(9, 0), h.inicio());
        assertEquals(LocalTime.of(11, 0), h.fin());
        assertEquals("N/D", h.aula());
    }

    @Test
    @DisplayName("Reconoce las columnas de jueves, viernes y sábado (con acento)")
    void juevesViernesSabado() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        Row f = fila(libro, "2", "LINC11", "FISICA", "CO01", "A");
        clase(f, JUE, "07:00", "09:00", "J-1");
        clase(f, VIE, "11:00", "13:00", "V-1");
        clase(f, SAB, "08:00", "10:00", "S-1");

        List<Horario> horarios = lector.leer(aStream(libro)).getMateria("LINC11").grupos().get(0).horarios();
        assertEquals(List.of(
                new Horario(DiaSemana.JUE, LocalTime.of(7, 0), LocalTime.of(9, 0), "J-1"),
                new Horario(DiaSemana.VIE, LocalTime.of(11, 0), LocalTime.of(13, 0), "V-1"),
                new Horario(DiaSemana.SAB, LocalTime.of(8, 0), LocalTime.of(10, 0), "S-1")),
                horarios);
        assertTrue(lector.getAdvertencias().isEmpty());
    }

    @Test
    @DisplayName("Aula vacía se guarda como \"N/D\"")
    void aulaVacia() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", null);

        Horario h = lector.leer(aStream(libro)).getMateria("LINC11").grupos().get(0).horarios().get(0);
        assertEquals("N/D", h.aula());
    }

    @Test
    @DisplayName("Si la plantilla no tiene columna DOCENTE, el docente queda vacío")
    void sinColumnaDocente() throws Exception {
        XSSFWorkbook libro = nuevoLibro(false);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "IGNORADO"), LUN, "09:00", "10:00", "X");

        Grupo grupo = lector.leer(aStream(libro)).getMateria("LINC11").grupos().get(0);
        assertEquals("", grupo.docente());
    }

    @Test
    @DisplayName("Las optativas (OPT1-8) reciben semestre 0 sin advertencias")
    void optativas() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "OPT1-8", "OPT01", "OPTATIVA", "CO01", "A"), LUN, "09:00", "10:00", "X");

        OfertaAcademica oferta = lector.leer(aStream(libro));
        assertEquals(ExcelOfertaReader.SEMESTRE_OPTATIVA, oferta.getMateria("OPT01").semestre());
        assertEquals(0, oferta.getMateria("OPT01").semestre());
        assertTrue(lector.getAdvertencias().isEmpty());
    }

    @Test
    @DisplayName("Ignora filas vacías, notas sueltas y filas sin clave ni nombre")
    void ignoraFilasSinMateria() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        Sheet hoja = libro.getSheet(HOJA);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");
        hoja.createRow(hoja.getLastRowNum() + 1);                                          // fila vacía
        hoja.createRow(hoja.getLastRowNum() + 1).createCell(SEM).setCellValue("Nota al pie"); // sin clave ni nombre
        clase(fila(libro, "2", "LINC12", "CALCULO", "CO01", "B"), MAR, "09:00", "10:00", "X");

        OfertaAcademica oferta = lector.leer(aStream(libro));
        assertEquals(2, oferta.materias().size());
        assertTrue(lector.getAdvertencias().isEmpty());
    }

    @Test
    @DisplayName("Elige automáticamente la hoja que tiene los encabezados aunque no sea la primera")
    void hojaAutomatica() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");
        Sheet portada = libro.createSheet("Portada");
        portada.createRow(0).createCell(0).setCellValue("Solo una portada");
        libro.setSheetOrder("Portada", 0);

        OfertaAcademica oferta = lector.leer(aStream(libro));
        assertNotNull(oferta.getMateria("LINC11"));
    }

    @Test
    @DisplayName("Puede leer una hoja concreta por nombre")
    void hojaPorNombre() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");

        OfertaAcademica oferta = new ExcelOfertaReader(HOJA).leer(aStream(libro));
        assertNotNull(oferta.getMateria("LINC11"));
    }

    // ------------------------------------------------------------------ advertencias (no detienen la lectura)

    @Test
    @DisplayName("Fila incompleta (clave sin nombre) se omite con advertencia y número de fila")
    void filaIncompleta() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", null, "CO01", "A"), LUN, "09:00", "10:00", "X");

        OfertaAcademica oferta = lector.leer(aStream(libro));
        assertTrue(oferta.materias().isEmpty());
        assertEquals(1, lector.getAdvertencias().size());
        assertTrue(lector.getAdvertencias().get(0).startsWith("Fila 4:"));
        assertTrue(hayAdvertencia("fila incompleta"));
    }

    @Test
    @DisplayName("Fila sin grupo (GPO) se omite con advertencia")
    void filaSinGrupo() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", null, "A"), LUN, "09:00", "10:00", "X");

        OfertaAcademica oferta = lector.leer(aStream(libro));
        assertTrue(oferta.materias().isEmpty());
        assertTrue(hayAdvertencia("no tiene grupo"));
    }

    @Test
    @DisplayName("Falta la hora de fin: se omite el horario y se avisa; el grupo queda sin horarios")
    void faltaHoraFin() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", null, "X");

        OfertaAcademica oferta = lector.leer(aStream(libro));
        assertTrue(oferta.getMateria("LINC11").grupos().get(0).horarios().isEmpty());
        assertTrue(hayAdvertencia("falta la hora de inicio o de fin"));
        assertTrue(hayAdvertencia("no tiene ningun horario"));
    }

    @Test
    @DisplayName("Hora ilegible o inicio posterior al fin: horario omitido con advertencia")
    void horarioInvalido() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        Row f = fila(libro, "2", "LINC11", "FISICA", "CO01", "A");
        clase(f, LUN, "abc", "10:00", "X");
        clase(f, MAR, "11:00", "09:00", "X");
        clase(f, MIE, "09:00", "10:00", "X"); // este sí es válido

        Grupo grupo = lector.leer(aStream(libro)).getMateria("LINC11").grupos().get(0);
        assertEquals(1, grupo.horarios().size());
        assertEquals(DiaSemana.MIE, grupo.horarios().get(0).dia());
        assertEquals(2, lector.getAdvertencias().stream().filter(a -> a.contains("horario invalido")).count());
    }

    @Test
    @DisplayName("Un grupo sin ningún horario se conserva pero se avisa")
    void grupoSinHorarios() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        fila(libro, "2", "LINC11", "FISICA", "CO01", "A");

        OfertaAcademica oferta = lector.leer(aStream(libro));
        assertEquals(1, oferta.getMateria("LINC11").grupos().size());
        assertTrue(hayAdvertencia("no tiene ningun horario"));
    }

    @Test
    @DisplayName("Misma clave con otro nombre: conserva el primero y avisa")
    void claveConNombreDistinto() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");
        clase(fila(libro, "2", "LINC11", "FISICA II", "CO02", "B"), MAR, "09:00", "10:00", "X");

        Materia materia = lector.leer(aStream(libro)).getMateria("LINC11");
        assertEquals("FISICA", materia.nombre());
        assertEquals(2, materia.grupos().size());
        assertTrue(hayAdvertencia("ya se usa para"));
    }

    @Test
    @DisplayName("El mismo nombre con distinta capitalización o acentos NO genera advertencia")
    void nombreEquivalente() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");
        clase(fila(libro, "2", "LINC11", "Física", "CO02", "B"), MAR, "09:00", "10:00", "X");

        lector.leer(aStream(libro));
        assertFalse(hayAdvertencia("ya se usa para"));
    }

    @Test
    @DisplayName("Misma clave con otro semestre: conserva el primero y avisa")
    void claveConSemestreDistinto() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");
        clase(fila(libro, "3", "LINC11", "FISICA", "CO02", "B"), MAR, "09:00", "10:00", "X");

        Materia materia = lector.leer(aStream(libro)).getMateria("LINC11");
        assertEquals(2, materia.semestre());
        assertTrue(hayAdvertencia("pero antes tenía semestre"));
    }

    @Test
    @DisplayName("Semestre no reconocido: se usa 0 y se avisa")
    void semestreDesconocido() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "X", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");

        Materia materia = lector.leer(aStream(libro)).getMateria("LINC11");
        assertEquals(0, materia.semestre());
        assertTrue(hayAdvertencia("semestre no reconocido"));
    }

    @Test
    @DisplayName("Grupo repetido: se conservan ambos y se avisa")
    void grupoRepetido() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");
        clase(fila(libro, "2", "LINC11", "FISICA", "CO01", "B"), MAR, "09:00", "10:00", "X");

        Materia materia = lector.leer(aStream(libro)).getMateria("LINC11");
        assertEquals(2, materia.grupos().size());
        assertTrue(hayAdvertencia("aparece repetido"));
    }

    @Test
    @DisplayName("Cada lectura empieza sin las advertencias de la anterior")
    void advertenciasSeLimpian() throws Exception {
        XSSFWorkbook conProblema = nuevoLibro(true);
        fila(conProblema, "2", "LINC11", "FISICA", "CO01", "A"); // grupo sin horarios -> advertencia
        lector.leer(aStream(conProblema));
        assertFalse(lector.getAdvertencias().isEmpty());

        XSSFWorkbook limpio = nuevoLibro(true);
        clase(fila(limpio, "2", "LINC11", "FISICA", "CO01", "A"), LUN, "09:00", "10:00", "X");
        lector.leer(aStream(limpio));
        assertTrue(lector.getAdvertencias().isEmpty());
    }

    @Test
    @DisplayName("La lista de advertencias es de solo lectura")
    void advertenciasInmutables() {
        assertThrows(UnsupportedOperationException.class, () -> lector.getAdvertencias().add("x"));
    }

    // ------------------------------------------------------------------ errores (detienen la lectura)

    @Test
    @DisplayName("InputStream nulo lanza ParserException")
    void streamNulo() {
        ParserException e = assertThrows(ParserException.class, () -> lector.leer(null));
        assertTrue(e.getMessage().contains("nulo"));
    }

    @Test
    @DisplayName("Un archivo que no es Excel lanza ParserException con la causa original")
    void noEsExcel() {
        InputStream basura = new ByteArrayInputStream("esto no es un excel".getBytes(StandardCharsets.UTF_8));
        ParserException e = assertThrows(ParserException.class, () -> lector.leer(basura));
        assertNotNull(e.getCause());
    }

    @Test
    @DisplayName("Si ninguna hoja tiene la fila de encabezados lanza ParserException")
    void sinEncabezados() throws Exception {
        XSSFWorkbook libro = new XSSFWorkbook();
        libro.createSheet("Otra").createRow(0).createCell(0).setCellValue("nada útil");
        InputStream entrada = aStream(libro);

        ParserException e = assertThrows(ParserException.class, () -> lector.leer(entrada));
        assertTrue(e.getMessage().contains("Ninguna hoja"));
    }

    @Test
    @DisplayName("Hoja por nombre inexistente o sin encabezados lanza ParserException")
    void hojaPorNombreInvalida() throws Exception {
        XSSFWorkbook libro = nuevoLibro(true);
        libro.createSheet("Vacia").createRow(0).createCell(0).setCellValue("sin tabla");
        byte[] bytes = bytes(libro);

        ParserException noExiste = assertThrows(ParserException.class,
                () -> new ExcelOfertaReader("NoExiste").leer(new ByteArrayInputStream(bytes)));
        assertTrue(noExiste.getMessage().contains("No existe la hoja"));

        ParserException sinTabla = assertThrows(ParserException.class,
                () -> new ExcelOfertaReader("Vacia").leer(new ByteArrayInputStream(bytes)));
        assertTrue(sinTabla.getMessage().contains("no tiene la fila de encabezados"));
    }

    @Test
    @DisplayName("Si faltan columnas indispensables lanza ParserException que las nombra")
    void columnasFaltantes() throws Exception {
        XSSFWorkbook libro = new XSSFWorkbook();
        Row enc = libro.createSheet(HOJA).createRow(0);
        enc.createCell(0).setCellValue("UNIDAD DE APRENDIZAJE");
        enc.createCell(1).setCellValue("LUNES");
        InputStream entrada = aStream(libro);

        ParserException e = assertThrows(ParserException.class, () -> lector.leer(entrada));
        assertTrue(e.getMessage().contains("columnas requeridas"));
        assertTrue(e.getMessage().contains("CLAVE"));
        assertTrue(e.getMessage().contains("GPO"));
    }

    // ------------------------------------------------------------------ plantillas reales del repositorio

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Plantilla_ICO_2025B__Normalizada.xlsx, 12, 38",
            "Plantilla_ICO_2026A_Normalizada.xlsx,  12, 35"})
    @DisplayName("Lee las plantillas reales de la UAEMex sin advertencias")
    void plantillasReales(String archivo, int materiasEsperadas, int gruposEsperados) throws Exception {
        OfertaAcademica oferta;
        try (InputStream in = Files.newInputStream(Path.of(archivo))) {
            oferta = lector.leer(in);
        }

        assertEquals(materiasEsperadas, oferta.materias().size());
        assertEquals(gruposEsperados,
                oferta.materias().values().stream().mapToInt(m -> m.grupos().size()).sum());
        assertEquals(List.of(), lector.getAdvertencias());

        Set<DiaSemana> dias = EnumSet.noneOf(DiaSemana.class);
        oferta.materias().forEach((clave, materia) -> {
            assertFalse(materia.nombre().isBlank(), "materia sin nombre: " + clave);
            for (Grupo grupo : materia.grupos()) {
                assertEquals(clave, grupo.claveMateria());
                assertFalse(grupo.horarios().isEmpty(), "grupo sin horarios: " + clave + "/" + grupo.clave());
                grupo.horarios().forEach(h -> dias.add(h.dia()));
            }
        });
        assertEquals(EnumSet.allOf(DiaSemana.class), dias, "deben aparecer los seis días de la semana");

        Grupo paradigmas = oferta.getMateria("LINC33").grupos().get(0);
        assertEquals("CO01", paradigmas.clave());
        assertEquals("ING. GUTIERREZ OCAMPO ELFEGO", paradigmas.docente());
        Horario primera = paradigmas.horarios().get(0);
        assertEquals(DiaSemana.MAR, primera.dia());
        assertEquals(LocalTime.of(7, 0), primera.inicio());
        assertEquals(LocalTime.of(9, 0), primera.fin());
    }
}
