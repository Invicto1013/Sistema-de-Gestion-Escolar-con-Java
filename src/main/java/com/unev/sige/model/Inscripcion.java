package com.unev.sige.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Inscripcion de un Estudiante en un periodo lectivo (tabla `inscripciones`).
 * Agrupa las materias inscritas mediante DetalleInscripcion.
 */
public class Inscripcion {

    private int id;
    private LocalDate fechaInscripcion;
    private String periodoLectivo;
    private String estado; // por defecto "Activa" en la BD
    private Estudiante estudiante;
    private List<DetalleInscripcion> detalles;

    public Inscripcion() {
        this.estado = "Activa";
        this.detalles = new ArrayList<>();
    }

    public Inscripcion(int id, LocalDate fechaInscripcion, String periodoLectivo, String estado,
                       Estudiante estudiante) {
        this.id = id;
        this.fechaInscripcion = fechaInscripcion;
        this.periodoLectivo = periodoLectivo;
        this.estado = estado;
        this.estudiante = estudiante;
        this.detalles = new ArrayList<>();
    }

    // ----- Getters y setters -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getFechaInscripcion() {
        return fechaInscripcion;
    }

    public void setFechaInscripcion(LocalDate fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }

    public String getPeriodoLectivo() {
        return periodoLectivo;
    }

    public void setPeriodoLectivo(String periodoLectivo) {
        this.periodoLectivo = periodoLectivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public List<DetalleInscripcion> getDetalles() {
        return detalles;
    }

    // ----- Metodos propios -----

    /**
     * Inscribe al estudiante en una seccion dentro de esta inscripcion.
     */
    public void agregarDetalle(DetalleInscripcion detalle) {
        if (detalle != null && !detalles.contains(detalle)) {
            detalles.add(detalle);
        }
    }

    @Override
    public String toString() {
        return "Inscripcion{" + id + ", periodo='" + periodoLectivo + "', estado='" + estado + "'}";
    }
}
