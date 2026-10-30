package parser;

import java.io.InputStream;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.DiaSemana;
import model.Grupo;
import model.Horario;
import model.Materia;
import model.OfertaAcademica;

/**
 * Lee la oferta académica desde un archivo JSON y la convierte en una {@link OfertaAcademica}.
 *
 * <h3>Formato esperado del JSON</h3>
 * <pre>
 * {
 *   "materias": [
 *     { "clave": "LINC11", "nombre": "FISICA", "semestre": 1,
 *       "grupos": [
 *         { "clave": "CO01", "docente": "DRA. QUINTANA GUERRA MARIA ROSA",
 *           "horarios": [ { "dia": "LUN", "inicio": "09:00", "fin": "11:00", "aula": "C-303" } ] }
 *       ] }
 *   ]
 * }
 * </pre>
 *
 * <ul>
 *   <li>Obligatorios: materia (clave, nombre), grupo (clave) y horario (dia, inicio, fin).</li>
 *   <li>Opcionales: semestre (0 si falta), docente y aula (se usa "N/D" si falta el aula).</li>
 *   <li>Días válidos: LUN, MAR, MIE, JUE, VIE y SAB (el sistema solo trabaja de lunes a sábado; también se acepta MIER como alias de MIE).</li>
 *   <li>Horas en formato HH:mm, por ejemplo "09:00".</li>
 * </ul>
 *
 * <h3>Decisión de diseño</h3>
 * <p>El modelo se construye "a mano" recorriendo el árbol JSON, en lugar de dejar que Jackson llene los
 * records automáticamente. Así los mensajes de error son claros (por ejemplo "Falta el campo 'clave' en
 * el grupo LINC11/CO01") y no hace falta agregar más librerías para manejar horas.</p>
 */
public class JsonOfertaReader implements OfertaReader {

    /** Jackson: convierte el texto JSON en un árbol de nodos que se puede recorrer. */
    private final ObjectMapper mapper;

    /** Formato de las horas del JSON ("9:00" o "09:00"). */
    private final DateTimeFormatter formatoHora;

    public JsonOfertaReader() {
        this.mapper = new ObjectMapper();
        this.formatoHora = DateTimeFormatter.ofPattern("H:mm");
    }

    /**
     * Lee el JSON y construye la oferta académica.
     *
     * @param in flujo del archivo .json (lo cierra quien lo abrió, no este método)
     * @return la oferta con todas las materias y grupos, en el orden del archivo
     * @throws ParserException si el JSON es inválido, falta algún campo obligatorio o hay datos incorrectos
     */
    @Override
    public OfertaAcademica leer(InputStream in) throws ParserException {
        if (in == null) {
            throw new ParserException("El InputStream del archivo JSON es nulo.");
        }
        try {
            JsonNode raiz = mapper.readTree(in);
            if (raiz == null || !raiz.path("materias").isArray()) {
                throw new ParserException("El JSON debe tener un arreglo llamado 'materias'.");
            }

            // LinkedHashMap conserva el orden en que vienen las materias en el archivo
            Map<String, Materia> materias = new LinkedHashMap<>();
            for (JsonNode nodoMateria : raiz.get("materias")) {
                String clave = texto(nodoMateria, "clave", true, "una materia");
                String donde = "la materia " + clave; // se usa en los mensajes de error
                String nombre = texto(nodoMateria, "nombre", true, donde);
                int semestre = nodoMateria.path("semestre").asInt(0); // 0 si falta (optativa)

                // Cada materia trae su lista de grupos
                List<Grupo> grupos = new ArrayList<>();
                for (JsonNode nodoGrupo : nodoMateria.path("grupos")) {
                    grupos.add(leerGrupo(nodoGrupo, clave));
                }

                if (materias.containsKey(clave)) {
                    throw new ParserException("La materia " + clave + " aparece repetida en el JSON.");
                }
                materias.put(clave, new Materia(clave, nombre, semestre, grupos));
            }
            return new OfertaAcademica(materias);

        } catch (ParserException e) {
            throw e; // ya trae un mensaje claro: no se envuelve de nuevo
        } catch (Exception e) {
            // JSON mal escrito, error de lectura, etc.
            throw new ParserException("Error al procesar el archivo JSON de oferta académica", e);
        }
    }

    /** Construye un {@link Grupo} (con sus horarios) a partir de su nodo JSON. */
    private Grupo leerGrupo(JsonNode nodo, String claveMateria) throws ParserException {
        String claveGrupo = texto(nodo, "clave", true, "un grupo de " + claveMateria);
        String donde = "el grupo " + claveMateria + "/" + claveGrupo;
        String docente = texto(nodo, "docente", false, donde); // opcional

        List<Horario> horarios = new ArrayList<>();
        for (JsonNode h : nodo.path("horarios")) {
            horarios.add(leerHorario(h, donde));
        }
        return new Grupo(claveGrupo, claveMateria, docente, horarios);
    }

    /** Construye un {@link Horario} (día, hora de inicio, hora de fin y aula) a partir de su nodo JSON. */
    private Horario leerHorario(JsonNode nodo, String donde) throws ParserException {
        try {
            DiaSemana dia = leerDia(texto(nodo, "dia", true, "un horario de " + donde));
            LocalTime inicio = LocalTime.parse(texto(nodo, "inicio", true, "un horario de " + donde), formatoHora);
            LocalTime fin = LocalTime.parse(texto(nodo, "fin", true, "un horario de " + donde), formatoHora);
            String aula = texto(nodo, "aula", false, donde);
            return new Horario(dia, inicio, fin, aula.isEmpty() ? "N/D" : aula);
        } catch (DateTimeException | IllegalArgumentException e) {
            // DateTimeException: hora mal escrita. IllegalArgumentException: el inicio no es antes del fin.
            throw new ParserException("Horario invalido en " + donde + ": " + e.getMessage(), e);
        }
    }

    /** Convierte el texto del día ("LUN", "mie"...) en el valor del enum {@link DiaSemana}. */
    private DiaSemana leerDia(String valor) throws ParserException {
        String v = valor.trim().toUpperCase();
        if (v.equals("MIER")) v = "MIE"; // alias: se acepta MIER por comodidad
        try {
            return DiaSemana.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new ParserException("Dia no valido: '" + valor + "' (use LUN, MAR, MIE, JUE, VIE o SAB).");
        }
    }

    /**
     * Lee un campo de texto de un nodo JSON.
     *
     * @param obligatorio si es true y el campo falta o está vacío, lanza un error que indica dónde falta
     * @param donde       descripción del lugar (para el mensaje de error)
     * @return el texto sin espacios sobrantes; "" si el campo no existe y no era obligatorio
     */
    private String texto(JsonNode nodo, String campo, boolean obligatorio, String donde) throws ParserException {
        JsonNode valor = nodo.get(campo);
        String t = (valor == null || valor.isNull()) ? "" : valor.asText().trim();
        if (t.isEmpty() && obligatorio) {
            throw new ParserException("Falta el campo '" + campo + "' en " + donde + ".");
        }
        return t;
    }
}