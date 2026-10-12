package com.unev.sige.model;

/**
 * Una materia (Seccion) dentro de una Inscripcion (tabla `detalle_inscripciones`).
 * Tiene una Calificacion (1 a 1) y varios registros de Asistencia.
 */
public class DetalleInscripcion {

    private int id;
    private String estadoMateria; // por defecto "Inscrita" en la BD
    private Seccion seccion;
    private Calificacion calificacion;

    public DetalleInscripcion() {
        this.estadoMateria = "Inscrita";
    }

    public DetalleInscripcion(int id, String estadoMateria, Seccion seccion) {
        this.id = id;
        this.estadoMateria = estadoMateria;
        this.seccion = seccion;
    }

    // ----- Getters y setters -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEstadoMateria() {
        return estadoMateria;
    }

    public void setEstadoMateria(String estadoMateria) {
        this.estadoMateria = estadoMateria;
    }

    public Seccion getSeccion() {
        return seccion;
    }

    public void setSeccion(Seccion seccion) {
        this.seccion = seccion;
    }

    public Calificacion getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Calificacion calificacion) {
        this.calificacion = calificacion;
    }

    @Override
    public String toString() {
        return "DetalleInscripcion{" + id + ", " + seccion + ", estado='" + estadoMateria + "'}";
    }
}
