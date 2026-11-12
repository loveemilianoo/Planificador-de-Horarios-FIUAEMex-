package main.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import main.model.DiaSemana;
import main.model.OfertaAcademica;
import main.motor.MotorHorarios;
import main.motor.ResultadoGeneracion;
import main.motor.TablaSeriaciones;

/**
 * Controlador del asistente de 3 pasos:
 * 0 inicio, 1 avance, 2 materias y preferencias, 3 resultados.
 * El estado del alumno vive en la sesión; los POST redirigen (patrón PRG).
 */
@WebServlet(name = "planificador", urlPatterns = "/planificador")
public class PlanificadorServlet extends HttpServlet {

    private static final String ESTADO = "estado";
    private static final String RESULTADO = "resultado";
    private static final String VISTAS = "/WEB-INF/vistas/";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if ("1".equals(req.getParameter("nuevo"))) {
            HttpSession s = req.getSession(false);
            if (s != null) {
                s.removeAttribute(ESTADO);
                s.removeAttribute(RESULTADO);
            }
        }
        EstadoAlumno estado = estado(req);
        int paso = entero(req.getParameter("paso"), 0);

        switch (paso) {
            case 1 -> mostrar(req, resp, estado, 1, "avance.jsp");
            case 2 -> mostrar(req, resp, estado, 2, "prefs.jsp");
            case 3 -> {
                ResultadoVista rv = (ResultadoVista) req.getSession().getAttribute(RESULTADO);
                if (rv == null) {
                    resp.sendRedirect(url(req, 2));
                    return;
                }
                if (req.getParameter("opcion") != null) {
                    int max = Math.max(0, rv.getOpciones().size() - 1);
                    estado.setOpcionSeleccionada(Math.max(0, Math.min(entero(req.getParameter("opcion"), 0), max)));
                }
                int sel = estado.getOpcionSeleccionada();
                req.setAttribute("resultado", rv);
                req.setAttribute("carga", rv.carga(sel));
                req.setAttribute("celdas", rv.celdas(sel));
                req.setAttribute("horaIni", rv.getHoraIni());
                req.setAttribute("horaFin", rv.getHoraFin());
                mostrar(req, resp, estado, 3, "resultados.jsp");
            }
            default -> mostrar(req, resp, estado, 0, "inicio.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        EstadoAlumno estado = estado(req);
        String accion = req.getParameter("accion");
        if (accion == null) {
            accion = "";
        }

        switch (accion) {
            case "semestre" -> {
                guardarAvance(req, estado);
                resp.sendRedirect(url(req, 1));
            }
            case "avance" -> {
                guardarAvance(req, estado);
                resp.sendRedirect(url(req, 2));
            }
            case "generar" -> generar(req, resp, estado);
            default -> resp.sendRedirect(url(req, 0));
        }
    }

    private void guardarAvance(HttpServletRequest req, EstadoAlumno estado) {
        estado.setSemestre(entero(req.getParameter("semestre"), estado.getSemestre()));
        estado.setAprobadas(valores(req, "apr"));
        // Si cambia el semestre o lo aprobado, lo ya elegido puede dejar de ser válido
        Set<String> disponibles = estado.getLista().stream()
                .map(MateriaVista::getClave)
                .collect(Collectors.toSet());
        estado.getDeseadas().retainAll(disponibles);
    }

    private void generar(HttpServletRequest req, HttpServletResponse resp, EstadoAlumno estado)
            throws ServletException, IOException {
        estado.setDeseadas(valores(req, "sel"));
        estado.setTurno(EstadoAlumno.turnoDeFormulario(req.getParameter("turno")));
        estado.setDiaLibre(req.getParameter("viernes") != null ? DiaSemana.VIE : null);
        estado.setOpcionSeleccionada(0);

        if (estado.getDeseadas().isEmpty()) {
            req.setAttribute("error", "Selecciona al menos una materia para generar tu horario.");
            mostrar(req, resp, estado, 2, "prefs.jsp");
            return;
        }

        ServletContext ctx = getServletContext();
        MotorHorarios motor = (MotorHorarios) ctx.getAttribute(AppContextListener.MOTOR);
        OfertaAcademica oferta = (OfertaAcademica) ctx.getAttribute(AppContextListener.OFERTA);
        ResultadoGeneracion resultado = motor.generar(oferta, estado.aAlumno());
        req.getSession().setAttribute(RESULTADO, ResultadoVista.de(resultado, oferta, estado.aAlumno()));
        resp.sendRedirect(url(req, 3));
    }

    private void mostrar(HttpServletRequest req, HttpServletResponse resp, EstadoAlumno estado, int paso, String vista)
            throws ServletException, IOException {
        req.setAttribute("paso", paso);
        req.setAttribute("estado", estado);
        if (paso == 1) {
            req.setAttribute("anteriores", estado.getAnteriores());
        } else if (paso == 2) {
            req.setAttribute("lista", estado.getLista());
        }
        resp.setContentType("text/html;charset=UTF-8");
        req.getRequestDispatcher(VISTAS + vista).forward(req, resp);
    }

    private EstadoAlumno estado(HttpServletRequest req) {
        HttpSession sesion = req.getSession();
        EstadoAlumno estado = (EstadoAlumno) sesion.getAttribute(ESTADO);
        if (estado == null) {
            ServletContext ctx = getServletContext();
            estado = new EstadoAlumno(
                    (OfertaAcademica) ctx.getAttribute(AppContextListener.OFERTA),
                    (TablaSeriaciones) ctx.getAttribute(AppContextListener.SERIACIONES));
            sesion.setAttribute(ESTADO, estado);
        }
        return estado;
    }

    private static Set<String> valores(HttpServletRequest req, String nombre) {
        String[] v = req.getParameterValues(nombre);
        return v == null ? new HashSet<>() : new HashSet<>(Arrays.asList(v));
    }

    private static int entero(String texto, int porDefecto) {
        try {
            return texto == null ? porDefecto : Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    private static String url(HttpServletRequest req, int paso) {
        return req.getContextPath() + "/planificador?paso=" + paso;
    }
}
