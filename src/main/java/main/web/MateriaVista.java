package main.web;

import main.model.Materia;

/** Materia tal como la pintan las JSP (bean con getters para EL). */
public final class MateriaVista {

    private final String clave;
    private final String nombre;
    private final int semestre;

    public MateriaVista(Materia m) {
        this.clave = m.clave();
        this.nombre = m.nombre();
        this.semestre = m.semestre();
    }

    public String getClave() { return clave; }
    public String getNombre() { return nombre; }
    public int getSemestre() { return semestre; }
}
