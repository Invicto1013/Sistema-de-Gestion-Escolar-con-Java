package com.unev.sige.model;

/**
 * Materia del plan de estudios (tabla `asignaturas`), por ejemplo Matematica o Espanol.
 * El profesor y el horario no estan aqui: pertenecen a la Seccion.
 */
public class Asignatura {

    private int id;
    private String codigo;
    private String nombre;
    private int creditos;
    private String descripcion;

    public Asignatura() {
    }

    public Asignatura(int id, String codigo, String nombre, int creditos, String descripcion) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.creditos = creditos;
        this.descripcion = descripcion;
    }

    // ----- Getters y setters -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCreditos() {
        return creditos;
    }

    public void setCreditos(int creditos) {
        this.creditos = creditos;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return "Asignatura{" + codigo + " - " + nombre + "}";
    }
}
