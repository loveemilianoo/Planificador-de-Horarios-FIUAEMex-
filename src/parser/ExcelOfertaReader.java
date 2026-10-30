package parser;

import java.io.InputStream;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import model.DiaSemana;
import model.Grupo;
import model.Horario;
import model.Materia;
import model.OfertaAcademica;

/**
 * Lee la plantilla de horarios de la UAEMex (.xlsx) y la convierte en una {@link OfertaAcademica}.
 *
 * <h3>Cómo funciona</h3>
 * <ol>
 *   <li>Busca la hoja y la fila de encabezados (la que dice "UNIDAD DE APRENDIZAJE").</li>
 *   <li>Detecta en qué columna está cada dato (Sem, CLAVE, UNIDAD DE APRENDIZAJE, GPO, DOCENTE)
 *       y dónde empieza el bloque de cada día (LUNES..SABADO, cada uno con INI / FIN / AULA).
 *       Se hace por el NOMBRE del encabezado y no por número de columna, porque las plantillas
 *       2025B y 2026A no tienen las mismas columnas.</li>
 *   <li>Recorre las filas: cada fila es un GRUPO de una materia. Las filas con la misma clave se
 *       agrupan dentro de una misma {@link Materia}.</li>
 * </ol>
 *
 * <h3>Notas importantes</h3>
 * <ul>
 *   <li>Si no se indica nombre de hoja, usa la primera hoja que contenga los encabezados.</li>
 *   <li>Los problemas en filas individuales (hora inválida, grupo vacío...) NO detienen la lectura:
 *       se anotan y se pueden consultar con {@link #getAdvertencias()}.</li>
 *   <li>Las materias optativas (semestre "OPT1-8", etc.) se guardan con semestre
 *       {@link #SEMESTRE_OPTATIVA} (0).</li>
 *   <li>Si una clave aparece con nombres o semestres distintos, o un mismo grupo aparece repetido, o hay
 *       filas con clave pero sin nombre (o al revés), NO se detiene la lectura: se avisa en
 *       {@link #getAdvertencias()}. Con claves repetidas se conserva el primer nombre y el primer semestre.</li>
 *   <li>NO es seguro para uso simultáneo desde varios hilos (guarda las advertencias en un campo).
 *       En el backend web conviene crear un {@code new ExcelOfertaReader()} por cada archivo subido.</li>
 * </ul>
 *
 * <h3>Ejemplo de uso</h3>
 * <pre>
 * ExcelOfertaReader lector = new ExcelOfertaReader();
 * OfertaAcademica oferta = lector.leer(inputStreamDelArchivo);
 * List&lt;String&gt; avisos = lector.getAdvertencias();
 * </pre>
 */
public class ExcelOfertaReader implements OfertaReader {

    /** Semestre que se asigna a las optativas (OPT1-8, OPT2-9, ...). Acordado con el equipo: 0. */
    public static final int SEMESTRE_OPTATIVA = 0;

    /** Cuántas filas desde arriba se revisan buscando los encabezados (la tabla empieza en la fila ~11). */
    private static final int FILAS_BUSQUEDA_ENCABEZADO = 25;

    /** Formato de las horas escritas como texto: "9:00", "09:00" o "09:00:00". */
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm[:ss]");

    /** Nombre de la hoja a leer; null significa "buscarla automáticamente". */
    private final String nombreHoja;

    /** Apache POI: convierte cualquier celda (número, fecha, texto) a texto tal como se ve en Excel. */
    private final DataFormatter formatter = new DataFormatter();

    /** Avisos de la última lectura (filas omitidas, horas inválidas, etc.). Se limpia en cada lectura. */
    private final List<String> advertencias = new ArrayList<>();

    /** Lector que busca automáticamente la hoja que contiene la tabla de horarios. */
    public ExcelOfertaReader() {
        this(null);
    }

    /**
     * Lector que usa una hoja concreta del libro.
     *
     * @param nombreHoja nombre exacto de la hoja (por ejemplo "Horarios 2"); null = automático
     */
    public ExcelOfertaReader(String nombreHoja) {
        this.nombreHoja = nombreHoja;
    }

    /**
     * Avisos generados durante la última llamada a {@link #leer(InputStream)}.
     *
     * @return lista de solo lectura; vacía si todo se leyó sin problemas
     */
    public List<String> getAdvertencias() {
        return Collections.unmodifiableList(advertencias);
    }

    /**
     * Lee el Excel y construye la oferta académica.
     *
     * @param in flujo del archivo .xlsx (lo cierra quien lo abrió, no este método)
     * @return la oferta con todas las materias y grupos encontrados, en el orden de la hoja
     * @throws ParserException si el archivo no es válido o no se encuentran las columnas necesarias
     */
    @Override
    public OfertaAcademica leer(InputStream in) throws ParserException {
        advertencias.clear(); // cada lectura empieza sin avisos de la anterior
        if (in == null) {
            throw new ParserException("El InputStream del archivo Excel es nulo.");
        }

        // try-with-resources: el libro de Excel se cierra solo al terminar
        try (Workbook libro = WorkbookFactory.create(in)) {

            // ---- 1. Ubicar la hoja, la fila de encabezados y las columnas ----
            Sheet hoja = elegirHoja(libro);
            int filaEncabezado = buscarFilaEncabezado(hoja);
            Columnas cols = mapearColumnas(hoja.getRow(filaEncabezado));

            // Debajo del encabezado hay una fila de subtítulos (INI / FIN / AULA) que no son datos: se salta.
            int primeraFila = filaEncabezado + 1;
            Row subEncabezado = hoja.getRow(primeraFila);
            if (subEncabezado != null
                    && normalizar(texto(subEncabezado.getCell(cols.primerColumnaDia()))).equals("INI")) {
                primeraFila++;
            }

            // LinkedHashMap conserva el orden en que aparecen las materias en la hoja.
            Map<String, String> nombres = new LinkedHashMap<>();             // clave -> nombre de la materia
            Map<String, Integer> semestres = new LinkedHashMap<>();          // clave -> semestre
            Map<String, List<Grupo>> gruposPorMateria = new LinkedHashMap<>(); // clave -> sus grupos

            // ---- 2. Recorrer las filas: cada fila es un grupo de una materia ----
            for (int i = primeraFila; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) continue;

                int numFila = i + 1; // número de fila como lo ve el usuario en Excel (POI cuenta desde 0)
                String clave = texto(fila.getCell(cols.clave));
                String nombre = texto(fila.getCell(cols.nombre));
                // Filas sin clave y sin nombre no son materias (vacías, notas, catálogo de aulas del final)
                if (clave.isEmpty() && nombre.isEmpty()) continue;
                // Fila "a medias": tiene clave o nombre pero no ambos. Se omite, pero se avisa para no perder datos en silencio.
                if (clave.isEmpty() || nombre.isEmpty()) {
                    advertir(numFila, "fila incompleta (clave='" + clave + "', nombre='" + nombre + "'); fila omitida");
                    continue;
                }

                String claveGrupo = texto(fila.getCell(cols.grupo));
                if (claveGrupo.isEmpty()) {
                    advertir(numFila, "la materia " + clave + " no tiene grupo (GPO); fila omitida");
                    continue;
                }

                // El docente es opcional: si la plantilla no tiene esa columna se deja vacío
                String docente = cols.docente >= 0 ? texto(fila.getCell(cols.docente)) : "";
                List<Horario> horarios = leerHorarios(fila, cols, numFila, clave, claveGrupo);
                if (horarios.isEmpty()) {
                    advertir(numFila, "el grupo " + clave + "/" + claveGrupo + " no tiene ningun horario");
                }

                // El nombre y el semestre son de la MATERIA: se guardan la primera vez que aparece su clave.
                // Si la misma clave vuelve a aparecer con otro nombre u otro semestre se avisa (no se detiene la
                // lectura): puede ser un error de captura o dos materias distintas que comparten clave.
                String nombrePrevio = nombres.putIfAbsent(clave, nombre);
                if (nombrePrevio != null && !normalizar(nombrePrevio).equals(normalizar(nombre))) {
                    advertir(numFila, "la clave " + clave + " ya se usa para '" + nombrePrevio + "' y aquí aparece como '"
                            + nombre + "'; se conserva el primer nombre y este grupo se agrega a esa materia");
                }
                String semestreTexto = cols.semestre >= 0 ? texto(fila.getCell(cols.semestre)) : "";
                int semestreFila = leerSemestre(semestreTexto, numFila);
                Integer semestrePrevio = semestres.putIfAbsent(clave, semestreFila);
                if (semestrePrevio != null && semestrePrevio.intValue() != semestreFila) {
                    advertir(numFila, "la materia " + clave + " aparece con semestre " + semestreFila
                            + " pero antes tenía semestre " + semestrePrevio + "; se conserva el primero");
                }

                // Se agrega el grupo a la lista de su materia (creando la lista si es el primero)
                List<Grupo> gruposDeLaMateria = gruposPorMateria.computeIfAbsent(clave, k -> new ArrayList<>());
                for (Grupo existente : gruposDeLaMateria) {
                    if (existente.clave().equals(claveGrupo)) {
                        advertir(numFila, "el grupo " + clave + "/" + claveGrupo
                                + " aparece repetido; se conservan ambos");
                        break;
                    }
                }
                gruposDeLaMateria.add(new Grupo(claveGrupo, clave, docente, horarios));
            }

            // ---- 3. Armar los objetos Materia (una por clave, con todos sus grupos) ----
            Map<String, Materia> materias = new LinkedHashMap<>();
            for (Map.Entry<String, List<Grupo>> e : gruposPorMateria.entrySet()) {
                String clave = e.getKey();
                materias.put(clave, new Materia(clave, nombres.get(clave), semestres.get(clave), e.getValue()));
            }
            return new OfertaAcademica(materias);

        } catch (ParserException e) {
            throw e; // ya trae un mensaje claro: no se envuelve de nuevo
        } catch (Exception e) {
            // Cualquier otro error (archivo corrupto, no es Excel, etc.) se reporta como ParserException
            throw new ParserException("Error al leer la plantilla Excel de oferta académica", e);
        }
    }

    // ------------------------------------------------------------------ hoja y encabezados

    /**
     * Elige la hoja a leer: la indicada por nombre, o la primera que tenga la fila de encabezados.
     * (Los libros de la UAEMex pueden traer varias hojas; no siempre la primera es la buena.)
     */
    private Sheet elegirHoja(Workbook libro) throws ParserException {
        if (nombreHoja != null) {
            Sheet hoja = libro.getSheet(nombreHoja);
            if (hoja == null) {
                throw new ParserException("No existe la hoja '" + nombreHoja + "' en el archivo Excel.");
            }
            if (buscarFilaEncabezadoOMenosUno(hoja) < 0) {
                throw new ParserException("La hoja '" + nombreHoja + "' no tiene la fila de encabezados.");
            }
            return hoja;
        }
        for (int i = 0; i < libro.getNumberOfSheets(); i++) {
            Sheet hoja = libro.getSheetAt(i);
            if (buscarFilaEncabezadoOMenosUno(hoja) >= 0) {
                return hoja;
            }
        }
        throw new ParserException(
                "Ninguna hoja del Excel contiene la fila de encabezados ('UNIDAD DE APRENDIZAJE').");
    }

    /** Igual que la versión "OMenosUno", pero lanza error si no encuentra la fila. */
    private int buscarFilaEncabezado(Sheet hoja) throws ParserException {
        int fila = buscarFilaEncabezadoOMenosUno(hoja);
        if (fila < 0) {
            throw new ParserException("No se encontró la fila de encabezados en el archivo Excel.");
        }
        return fila;
    }

    /**
     * Busca en las primeras filas la que contiene "UNIDAD DE APRENDIZAJE" (el encabezado de la tabla).
     *
     * @return el índice de la fila (desde 0), o -1 si esta hoja no tiene la tabla
     */
    private int buscarFilaEncabezadoOMenosUno(Sheet hoja) {
        int limite = Math.min(FILAS_BUSQUEDA_ENCABEZADO, hoja.getLastRowNum());
        for (int r = 0; r <= limite; r++) {
            Row fila = hoja.getRow(r);
            if (fila == null) continue;
            for (Cell celda : fila) {
                if (normalizar(texto(celda)).contains("UNIDAD DE APRENDIZAJE")) {
                    return r;
                }
            }
        }
        return -1;
    }

    /**
     * Recorre la fila de encabezados y anota en qué columna está cada dato.
     *
     * <p>Usa comparación exacta ({@code equals}) con "GPO" a propósito: la plantilla también tiene la
     * columna "GRUPOS EQUIVALENTES ENTRE PLANES", y si se buscara "GRUPO" se confundiría con esa.</p>
     */
    private Columnas mapearColumnas(Row encabezado) throws ParserException {
        Columnas cols = new Columnas();
        for (int c = 0; c < encabezado.getLastCellNum(); c++) {
            String v = normalizar(texto(encabezado.getCell(c)));
            if (v.isEmpty()) continue;

            if (v.equals("SEM") && cols.semestre < 0) cols.semestre = c;
            else if (v.equals("CLAVE") && cols.clave < 0) cols.clave = c; // la 2a "CLAVE" (plan 2004) se ignora
            else if (v.contains("UNIDAD DE APRENDIZAJE") && cols.nombre < 0) cols.nombre = c;
            else if (v.equals("GPO") && cols.grupo < 0) cols.grupo = c;   // NO "GRUPOS EQUIVALENTES..."
            else if (v.equals("DOCENTE") && cols.docente < 0) cols.docente = c;
            // Cada día ocupa 3 columnas: INI, FIN, AULA. Aquí solo se guarda donde empieza (INI).
            else if (v.equals("LUNES")) cols.dias.put(c, DiaSemana.LUN);
            else if (v.equals("MARTES")) cols.dias.put(c, DiaSemana.MAR);
            else if (v.equals("MIERCOLES")) cols.dias.put(c, DiaSemana.MIE);
            else if (v.equals("JUEVES")) cols.dias.put(c, DiaSemana.JUE);
            else if (v.equals("VIERNES")) cols.dias.put(c, DiaSemana.VIE);
            else if (v.equals("SABADO")) cols.dias.put(c, DiaSemana.SAB);
        }

        // Estas columnas son indispensables; sin ellas no se puede construir la oferta
        List<String> faltan = new ArrayList<>();
        if (cols.clave < 0) faltan.add("CLAVE");
        if (cols.nombre < 0) faltan.add("UNIDAD DE APRENDIZAJE");
        if (cols.grupo < 0) faltan.add("GPO");
        if (cols.dias.isEmpty()) faltan.add("LUNES..SABADO");
        if (!faltan.isEmpty()) {
            throw new ParserException("No se pudieron identificar las columnas requeridas: " + faltan);
        }
        return cols;
    }

    // ------------------------------------------------------------------ lectura de filas

    /**
     * Lee los horarios de una fila (un grupo): revisa cada día y, si tiene hora de inicio y de fin,
     * crea un {@link Horario}. Un horario con problemas se omite y se anota como advertencia.
     */
    private List<Horario> leerHorarios(Row fila, Columnas cols, int numFila, String clave, String grupo) {
        List<Horario> horarios = new ArrayList<>();
        for (Map.Entry<Integer, DiaSemana> e : cols.dias.entrySet()) {
            int c = e.getKey(); // columna INI; FIN = c+1; AULA = c+2
            try {
                LocalTime inicio = leerHora(fila.getCell(c));
                LocalTime fin = leerHora(fila.getCell(c + 1));
                if (inicio == null && fin == null) continue; // ese día el grupo no tiene clase

                if (inicio == null || fin == null) {
                    advertir(numFila, clave + "/" + grupo + " " + e.getValue()
                            + ": falta la hora de inicio o de fin; horario omitido");
                    continue;
                }
                String aula = texto(fila.getCell(c + 2));
                // Si no hay aula se pone "N/D" (no disponible)
                horarios.add(new Horario(e.getValue(), inicio, fin, aula.isEmpty() ? "N/D" : aula));
            } catch (DateTimeException | IllegalArgumentException ex) {
                // DateTimeException: hora con formato raro. IllegalArgumentException: inicio no es antes de fin.
                advertir(numFila, clave + "/" + grupo + " " + e.getValue()
                        + ": horario invalido (" + ex.getMessage() + "); horario omitido");
            }
        }
        return horarios;
    }

    /**
     * Convierte una celda en una hora.
     *
     * <p>Excel guarda las horas como fracción del día (9:00 = 0.375). Esta función soporta tanto
     * celdas de hora reales como horas escritas como texto ("9:00" o "09:00").</p>
     *
     * @return la hora, o null si la celda está vacía
     */
    private LocalTime leerHora(Cell celda) {
        if (celda == null) return null;

        if (celda.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
            double fraccionDelDia = celda.getNumericCellValue();
            fraccionDelDia = fraccionDelDia - Math.floor(fraccionDelDia); // se descarta la parte de fecha
            long minutos = Math.round(fraccionDelDia * 24 * 60);          // se redondea al minuto más cercano
            return LocalTime.of((int) (minutos / 60) % 24, (int) (minutos % 60));
        }

        String t = texto(celda);
        if (t.isEmpty()) return null;
        return LocalTime.parse(t, FORMATO_HORA); // lanza DateTimeParseException si el texto no es una hora
    }

    /**
     * Interpreta el semestre: "3" -> 3; "OPT1-8" (optativa) -> {@link #SEMESTRE_OPTATIVA}.
     * Si el valor no se reconoce se usa también 0 y se anota una advertencia.
     */
    private int leerSemestre(String valor, int numFila) {
        String v = normalizar(valor);
        if (v.matches("\\d+")) {
            return Integer.parseInt(v);
        }
        if (v.startsWith("OPT")) {
            return SEMESTRE_OPTATIVA;
        }
        advertir(numFila, "semestre no reconocido ('" + valor + "'); se usa " + SEMESTRE_OPTATIVA);
        return SEMESTRE_OPTATIVA;
    }

    // ------------------------------------------------------------------ utilidades

    /** Texto de una celda tal como se ve en Excel, sin espacios (incluye espacios "duros" \u00A0). */
    private String texto(Cell celda) {
        if (celda == null) return "";
        return formatter.formatCellValue(celda).replace('\u00A0', ' ').trim();
    }

    /**
     * Deja un texto en mayúsculas, sin acentos y con espacios simples, para compararlo sin errores:
     * "Miércoles " -> "MIERCOLES".
     */
    private static String normalizar(String s) {
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toUpperCase().replaceAll("\\s+", " ").trim();
    }

    /** Guarda un aviso indicando la fila de Excel donde ocurrió. */
    private void advertir(int numFila, String mensaje) {
        advertencias.add("Fila " + numFila + ": " + mensaje);
    }

    /** Posiciones (índices de columna) detectadas en el encabezado. -1 significa "no encontrada". */
    private static class Columnas {
        int semestre = -1, clave = -1, nombre = -1, grupo = -1, docente = -1;

        /** Columna donde empieza el bloque de cada día (la de INI) y a qué día corresponde. */
        final Map<Integer, DiaSemana> dias = new LinkedHashMap<>();

        /** Columna INI del primer día; sirve para reconocer la fila de subtítulos INI/FIN/AULA. */
        int primerColumnaDia() {
            return dias.keySet().iterator().next();
        }
    }
}