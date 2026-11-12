package main.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import main.model.OfertaAcademica;
import main.motor.TablaSeriaciones;

/**
 * Lee las seriaciones (prerrequisitos) desde un CSV y las valida contra la oferta académica cargada.
 *
 * Los archivos de oferta (Excel/JSON) no traen seriaciones, por eso viven en su propio archivo:
 * {@code src/main/resources/datos/seriaciones.csv}.
 *
 * Formato (UTF-8): una fila por prerrequisito, "clave,prerrequisito"; la materia "clave" requiere
 * haber aprobado "prerrequisito". La primera fila que no sea comentario es el encabezado y se omite.
 * Se omiten las líneas vacías y las que empiezan con "#".
 *
 * Validación contra la oferta:
 * - Si la materia "clave" no está en la oferta, la fila se ignora y se registra una advertencia
 *   (esa materia no se puede elegir, así que su seriación no afecta nada; es normal con plantillas parciales).
 * - Si la materia sí está pero su prerrequisito no, se lanza {@link ParserException}: el alumno nunca
 *   podría cumplir esa seriación.
 * - Una fila mal formada (no son 2 columnas, o alguna está vacía) también lanza {@link ParserException}.
 *
 * Los errores indican el número de línea del archivo. Una instancia guarda las advertencias de la
 * última lectura, por lo que no debe compartirse entre hilos.
 */
public class SeriacionesCsvReader {

    /** Avisos de la última lectura (filas ignoradas). Se limpia en cada lectura. */
    private final List<String> advertencias = new ArrayList<>();

    /** Avisos de la última llamada a {@link #leer}; lista de solo lectura, vacía si no hubo. */
    public List<String> getAdvertencias() {
        return Collections.unmodifiableList(advertencias);
    }

    /**
     * Lee el CSV y construye la tabla de seriaciones.
     *
     * @param in     flujo del archivo (lo cierra quien lo abrió, no este método)
     * @param oferta oferta contra la que se validan las claves
     * @return tabla con las seriaciones que aplican a la oferta
     * @throws ParserException si el archivo no se puede leer, una fila está mal formada o un
     *                         prerrequisito no existe en la oferta
     */
    public TablaSeriaciones leer(InputStream in, OfertaAcademica oferta) throws ParserException {
        if (in == null) {
            throw new ParserException("El InputStream del archivo de seriaciones es nulo.");
        }
        Objects.requireNonNull(oferta, "oferta es obligatoria");
        advertencias.clear();

        Map<String, Set<String>> requisitos = new HashMap<>();
        try {
            BufferedReader lector = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            int numero = 0;
            boolean esperandoEncabezado = true;
            while ((linea = lector.readLine()) != null) {
                numero++;
                if (numero == 1 && linea.startsWith("﻿")) {
                    linea = linea.substring(1); // marca BOM que agrega Excel al guardar como CSV UTF-8
                }
                if (linea.isBlank() || linea.startsWith("#")) {
                    continue;
                }
                if (esperandoEncabezado) {
                    esperandoEncabezado = false;
                    continue;
                }
                procesarFila(linea, numero, oferta, requisitos);
            }
        } catch (IOException e) {
            throw new ParserException("Error al leer el archivo de seriaciones", e);
        }
        return new TablaSeriaciones(requisitos);
    }

    private void procesarFila(String linea, int numero, OfertaAcademica oferta,
                              Map<String, Set<String>> requisitos) throws ParserException {
        String[] columnas = linea.split(",", -1);
        if (columnas.length != 2) {
            throw new ParserException("Seriaciones, línea " + numero + ": se esperaban 2 columnas "
                    + "(clave,prerrequisito) y hay " + columnas.length + ".");
        }
        String clave = columnas[0].trim();
        String prerrequisito = columnas[1].trim();
        if (clave.isEmpty() || prerrequisito.isEmpty()) {
            throw new ParserException("Seriaciones, línea " + numero + ": falta la clave o el prerrequisito.");
        }
        if (oferta.getMateria(clave) == null) {
            advertencias.add("Seriaciones, línea " + numero + ": " + clave
                    + " no está en la oferta cargada; se ignora su seriación con " + prerrequisito + ".");
            return;
        }
        if (oferta.getMateria(prerrequisito) == null) {
            throw new ParserException("Seriaciones, línea " + numero + ": el prerrequisito " + prerrequisito
                    + " de " + clave + " no está en la oferta cargada; el alumno no podría marcarlo como aprobado.");
        }
        requisitos.computeIfAbsent(clave, k -> new HashSet<>()).add(prerrequisito);
    }
}
