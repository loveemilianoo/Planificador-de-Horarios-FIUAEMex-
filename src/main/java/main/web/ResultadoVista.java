package main.web;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.model.Alumno;
import main.model.DiaSemana;
import main.model.Grupo;
import main.model.Horario;
import main.model.Materia;
import main.model.OfertaAcademica;
import main.model.OpcionHorario;
import main.model.Turno;
import main.motor.ResultadoGeneracion;

/** Adapta el {@link ResultadoGeneracion} del motor a lo que necesitan las JSP. */
public final class ResultadoVista {

    /** Píxeles que mide una hora en el calendario (coincide con la altura de fila del CSS). */
    static final int PX_POR_HORA = 32;
    static final int COLORES = 6;
    private static final String[] LETRAS = {"A", "B", "C", "D", "E", "F"};

    /** Pestaña de una opción de horario. */
    public static final class OpcionVista {
        private final String etiqueta;
        private final String resumen;

        OpcionVista(String etiqueta, String resumen) {
            this.etiqueta = etiqueta;
            this.resumen = resumen;
        }

        public String getEtiqueta() { return etiqueta; }
        public String getResumen() { return resumen; }
    }

    /** Un grupo dentro de la lista "Tu carga". */
    public static final class CargaVista {
        private final String materia;
        private final String grupo;
        private final String docente;
        private final String color;

        CargaVista(String materia, String grupo, String docente, String color) {
            this.materia = materia;
            this.grupo = grupo;
            this.docente = docente;
            this.color = color;
        }

        public String getMateria() { return materia; }
        public String getGrupo() { return grupo; }
        public String getDocente() { return docente; }
        public String getColor() { return color; }
    }

    /** Datos de texto de un bloque del calendario. */
    public static final class BloqueVista {
        private final String etiqueta;
        private final String grupo;
        private final String docente;
        private final String color;

        BloqueVista(String etiqueta, String grupo, String docente, String color) {
            this.etiqueta = etiqueta;
            this.grupo = grupo;
            this.docente = docente;
            this.color = color;
        }

        public String getEtiqueta() { return etiqueta; }
        public String getGrupo() { return grupo; }
        public String getDocente() { return docente; }
        public String getColor() { return color; }
    }

    /** Bloque ubicado dentro de la celda de su hora de inicio. */
    public static final class CeldaVista {
        private final int top;
        private final int alto;
        private final BloqueVista bloque;

        CeldaVista(int top, int alto, BloqueVista bloque) {
            this.top = top;
            this.alto = alto;
            this.bloque = bloque;
        }

        public int getTop() { return top; }
        public int getAlto() { return alto; }
        public BloqueVista getBloque() { return bloque; }
    }

    private final ResultadoGeneracion resultado;
    private final OfertaAcademica oferta;
    private final List<OpcionVista> opciones;
    private final String mensajeSinResultados;
    private final List<String> sugerencias;
    private final String mensajeOpcionUnica;
    private final int horaIni;
    private final int horaFin;

    private ResultadoVista(ResultadoGeneracion resultado, OfertaAcademica oferta, Alumno alumno) {
        this.resultado = resultado;
        this.oferta = oferta;

        List<OpcionVista> ops = new ArrayList<>();
        for (int i = 0; i < resultado.opciones().size(); i++) {
            String letra = i < LETRAS.length ? LETRAS[i] : String.valueOf(i + 1);
            ops.add(new OpcionVista("Opción " + letra, resumen(resultado.opciones().get(i))));
        }
        this.opciones = List.copyOf(ops);

        String avisos = String.join(" ", resultado.avisos());
        this.mensajeSinResultados = avisos.isBlank()
                ? "Con las materias y preferencias elegidas no se encontró ninguna combinación sin traslapes."
                : avisos;
        this.sugerencias = sugerencias(resultado, alumno);
        this.mensajeOpcionUnica = resultado.opciones().size() == 1
                ? "Solo se encontró una opción viable con tu selección." : "";

        int min = 24;
        int max = 0;
        for (OpcionHorario o : resultado.opciones()) {
            for (Grupo g : o.grupos()) {
                for (Horario h : g.horarios()) {
                    min = Math.min(min, h.inicio().getHour());
                    int fin = h.fin().getHour() + (h.fin().getMinute() > 0 ? 1 : 0);
                    max = Math.max(max, fin - 1);
                }
            }
        }
        if (min > max) { // sin opciones: rango por defecto
            min = 7;
            max = 20;
        }
        this.horaIni = min;
        this.horaFin = max;
    }

    public static ResultadoVista de(ResultadoGeneracion resultado, OfertaAcademica oferta, Alumno alumno) {
        return new ResultadoVista(resultado, oferta, alumno);
    }

    public boolean getHayViables() { return resultado.hayOpciones(); }
    public List<OpcionVista> getOpciones() { return opciones; }
    public List<String> getAvisos() { return resultado.avisos(); }
    public String getMensajeSinResultados() { return mensajeSinResultados; }
    public List<String> getSugerencias() { return sugerencias; }
    public String getMensajeOpcionUnica() { return mensajeOpcionUnica; }
    public int getHoraIni() { return horaIni; }
    public int getHoraFin() { return horaFin; }

    /** Lista "Tu carga" de la opción indicada (un índice fuera de rango se ajusta). */
    public List<CargaVista> carga(int indice) {
        List<CargaVista> carga = new ArrayList<>();
        OpcionHorario opcion = opcion(indice);
        if (opcion == null) return carga;
        for (int i = 0; i < opcion.grupos().size(); i++) {
            Grupo g = opcion.grupos().get(i);
            carga.add(new CargaVista(nombreMateria(g), g.clave(), docente(g), color(i)));
        }
        return carga;
    }

    /** Celdas del calendario de la opción, con llave "díaIndex-hora" (0 = lunes). */
    public Map<String, List<CeldaVista>> celdas(int indice) {
        Map<String, List<CeldaVista>> celdas = new LinkedHashMap<>();
        OpcionHorario opcion = opcion(indice);
        if (opcion == null) return celdas;
        for (int i = 0; i < opcion.grupos().size(); i++) {
            Grupo g = opcion.grupos().get(i);
            BloqueVista bloque = new BloqueVista(nombreMateria(g), g.clave(), docente(g), color(i));
            for (Horario h : g.horarios()) {
                int top = h.inicio().getMinute() * PX_POR_HORA / 60;
                long duracion = Duration.between(h.inicio(), h.fin()).toMinutes();
                int alto = Math.max(14, (int) (duracion * PX_POR_HORA / 60) - 2);
                String llave = h.dia().ordinal() + "-" + h.inicio().getHour();
                celdas.computeIfAbsent(llave, k -> new ArrayList<>()).add(new CeldaVista(top, alto, bloque));
            }
        }
        return celdas;
    }

    private OpcionHorario opcion(int indice) {
        if (resultado.opciones().isEmpty()) return null;
        int i = Math.max(0, Math.min(indice, resultado.opciones().size() - 1));
        return resultado.opciones().get(i);
    }

    private String nombreMateria(Grupo g) {
        Materia m = oferta.getMateria(g.claveMateria());
        return m == null ? g.claveMateria() : m.nombre();
    }

    private static String docente(Grupo g) {
        return g.docente() == null || g.docente().isBlank() ? "Docente por asignar" : g.docente();
    }

    private static String color(int indice) {
        return "c" + (indice % COLORES);
    }

    private String resumen(OpcionHorario opcion) {
        Set<DiaSemana> dias = EnumSet.noneOf(DiaSemana.class);
        long minutos = 0;
        for (Grupo g : opcion.grupos()) {
            for (Horario h : g.horarios()) {
                dias.add(h.dia());
                minutos += Duration.between(h.inicio(), h.fin()).toMinutes();
            }
        }
        double horas = minutos / 60.0;
        String textoHoras = horas == Math.rint(horas) ? String.valueOf((int) horas) : String.format("%.1f", horas);
        return opcion.grupos().size() + " materias · " + textoHoras + " h por semana · "
                + dias.size() + " días de asistencia";
    }

    private static List<String> sugerencias(ResultadoGeneracion r, Alumno alumno) {
        List<String> s = new ArrayList<>();
        r.materiasDescartadas().forEach((clave, motivo) -> s.add(clave + ": " + motivo));
        if (alumno.turnoPreferido() != null && alumno.turnoPreferido() != Turno.SIN_PREFERENCIA) {
            s.add("Elige \"Sin preferencia\" en el turno.");
        }
        if (alumno.diaLibre() != null) {
            s.add("Desmarca el día libre.");
        }
        s.add("Quita una materia de tu selección e inténtalo de nuevo.");
        return s;
    }
}
