package com.unev.sige.model;

import java.time.LocalDate;

public class ReporteAcademico {

    private int idReporte;
    private String tipoReporte; // Ej: "Boleta de Calificaciones", "Récord de Notas"
    private LocalDate fechaGeneracion;
    private String contenidoDetalle;
    private int idEstudiante;
    private Estudiante estudiante;

    // Constructor por defecto
    public ReporteAcademico() {
        this.fechaGeneracion = LocalDate.now();
    }

    // Constructor completo
    public ReporteAcademico(int idReporte, String tipoReporte, String contenidoDetalle, int idEstudiante) {
        this.idReporte = idReporte;
        this.tipoReporte = tipoReporte;
        this.contenidoDetalle = contenidoDetalle;
        this.idEstudiante = idEstudiante;
        this.fechaGeneracion = LocalDate.now();
    }

    // Getters y Setters
    public int getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(int idReporte) {
        this.idReporte = idReporte;
    }

    public String getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(String tipoReporte) {
        this.tipoReporte = tipoReporte;
    }

    public LocalDate getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDate fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getContenidoDetalle() {
        return contenidoDetalle;
    }

    public void setContenidoDetalle(String contenidoDetalle) {
        this.contenidoDetalle = contenidoDetalle;
    }

    public int getIdEstudiante() {
        return idEstudiante;
    }

    public void setIdEstudiante(int idEstudiante) {
        this.idEstudiante = idEstudiante;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }
}