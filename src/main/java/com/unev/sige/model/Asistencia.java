package com.unev.sige.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Asistencia de un estudiante a una materia inscrita en una fecha (tabla `asistencias`).
 * Hay un solo registro por materia inscrita y dia.
 */
public class Asistencia {

    private int id;
    private int idDetalleInscripcion;
    private LocalDate fecha;
    private boolean presente;
    private String observacion;

    public Asistencia() {
    }

    public Asistencia(int id, int idDetalleInscripcion, LocalDate fecha, boolean presente, String observacion) {
        this.id = id;
        this.idDetalleInscripcion = idDetalleInscripcion;
        this.fecha = fecha;
        this.presente = presente;
        this.observacion = observacion;
    }

    // ----- Getters y setters -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdDetalleInscripcion() {
        return idDetalleInscripcion;
    }

    public void setIdDetalleInscripcion(int idDetalleInscripcion) {
        this.idDetalleInscripcion = idDetalleInscripcion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public boolean isPresente() {
        return presente;
    }

    public void setPresente(boolean presente) {
        this.presente = presente;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    // ----- Metodos propios -----

    /**
     * Calcula el porcentaje de asistencia a partir de los registros de UNA materia inscrita.
     */
    public static double calcularPorcentajeAsistencia(List<Asistencia> registros) {
        if (registros == null || registros.isEmpty()) {
            return 0.0;
        }
        long presentes = registros.stream().filter(Asistencia::isPresente).count();
        return (presentes * 100.0) / registros.size();
    }

    @Override
    public String toString() {
        return "Asistencia{detalle=" + idDetalleInscripcion + " (" + fecha + "): "
                + (presente ? "Presente" : "Ausente") + "}";
    }
}
