package main.web;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import main.model.OfertaAcademica;
import main.motor.MotorHorarios;
import main.motor.TablaSeriaciones;
import main.parser.ExcelOfertaReader;
import main.parser.ParserException;
import main.parser.SeriacionesCsvReader;

/**
 * Al arrancar la aplicación carga una sola vez la oferta académica (Excel) y las
 * seriaciones (CSV), y deja en el {@link ServletContext} lo que usa el servlet.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    public static final String OFERTA = "oferta";
    public static final String SERIACIONES = "seriaciones";
    public static final String MOTOR = "motor";

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        ServletContext ctx = evento.getServletContext();
        String rutaOferta = ctx.getInitParameter("ofertaRecurso");
        String rutaSeriaciones = ctx.getInitParameter("seriacionesRecurso");
        try {
            ExcelOfertaReader lectorOferta = new ExcelOfertaReader();
            OfertaAcademica oferta;
            try (InputStream in = recurso(rutaOferta)) {
                oferta = lectorOferta.leer(in);
            }
            lectorOferta.getAdvertencias().forEach(a -> ctx.log("Oferta: " + a));

            SeriacionesCsvReader lectorSeriaciones = new SeriacionesCsvReader();
            TablaSeriaciones seriaciones;
            try (InputStream in = recurso(rutaSeriaciones)) {
                seriaciones = lectorSeriaciones.leer(in, oferta);
            }
            lectorSeriaciones.getAdvertencias().forEach(a -> ctx.log("Seriaciones: " + a));

            ctx.setAttribute(OFERTA, oferta);
            ctx.setAttribute(SERIACIONES, seriaciones);
            ctx.setAttribute(MOTOR, new MotorHorarios(seriaciones));
            ctx.log("Oferta cargada: " + oferta.materias().size() + " materias.");
        } catch (IOException | ParserException e) {
            throw new IllegalStateException("No se pudo cargar la oferta académica o las seriaciones", e);
        }
    }

    private static InputStream recurso(String ruta) throws IOException {
        InputStream in = AppContextListener.class.getResourceAsStream(ruta);
        if (in == null) {
            throw new IOException("No se encontró el recurso " + ruta + " en el classpath");
        }
        return in;
    }
}
