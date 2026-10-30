package parser;

/**
 * Error que se lanza cuando un archivo de oferta académica no se puede leer.
 *
 * <p>Ejemplos: el archivo no es un Excel válido, falta la fila de encabezados, el JSON no tiene
 * el campo "materias", etc. Es una excepción verificada ({@code extends Exception}), por lo que
 * quien llame a {@link OfertaReader#leer} está obligado a manejarla; así la capa web puede
 * mostrarle al usuario un mensaje claro en lugar de fallar sin explicación.</p>
 */
public class ParserException extends Exception {

    /** Error con un mensaje descriptivo (cuando nosotros detectamos el problema). */
    public ParserException(String message) {
        super(message);
    }

    /**
     * Error con mensaje y causa original (cuando se envuelve otra excepción, por ejemplo
     * un error de la librería Apache POI o de Jackson). La causa se conserva para depurar.
     */
    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}