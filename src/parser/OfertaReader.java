package parser;

import java.io.InputStream;

import model.OfertaAcademica;

/**
 * Contrato común de todos los lectores de oferta académica.
 *
 * <p>Un "lector" toma un archivo (Excel, JSON, etc.) y lo convierte en un objeto
 * {@link OfertaAcademica}, que es lo que usa el resto del sistema (filtros, motor combinatorio...).
 * Así, el resto del sistema no necesita saber de qué tipo de archivo vino la información.</p>
 *
 * <p>Implementaciones actuales: {@link ExcelOfertaReader} y {@link JsonOfertaReader}.</p>
 *
 * <p>Decisión de diseño: el método recibe un {@link InputStream} y no una ruta de archivo.
 * Gracias a eso el mismo lector sirve para un archivo del disco (en pruebas) y para un archivo
 * que el usuario sube desde la página web, sin cambiar ni una línea del lector.</p>
 */
public interface OfertaReader {

    /**
     * Lee el archivo y construye la oferta académica.
     *
     * @param in flujo de bytes del archivo. El lector NO lo cierra: lo cierra quien lo abrió.
     * @return la oferta académica con todas las materias y sus grupos
     * @throws ParserException si el archivo no se puede leer o no tiene el formato esperado
     */
    OfertaAcademica leer(InputStream in) throws ParserException;
}