package com.unev.sige.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Registra si un Estudiante estuvo presente en una Asignatura en una fecha determinada.
 * Relaciona a un Estudiante con una Asignatura en una fecha concreta.
 */
public class Asistencia {

    private int id;
    private Estudiante estudiante;
    private Asignatura asignatura;
    private LocalDate fecha;
    private boolean presente;
    private String observacion;

    public Asistencia() {
    }

    public Asistencia(int id, Estudiante estudiante, Asignatura asignatura, LocalDate fecha,
                       boolean presente, String observacion) {
        this.id = id;
        this.estudiante = estudiante;
        this.asignatura = asignatura;
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

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public void setAsignatura(Asignatura asignatura) {
        this.asignatura = asignatura;
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
     * Calcula el porcentaje de asistencia de un estudiante a partir de una lista de registros.
     */
    public static double calcularPorcentajeAsistencia(Estudiante estudiante, List<Asistencia> registros) {
        if (registros == null || registros.isEmpty()) {
            return 0.0;
        }
        long total = 0;
        long presentes = 0;
        for (Asistencia a : registros) {
            if (a.getEstudiante().equals(estudiante)) {
                total++;
                if (a.isPresente()) {
                    presentes++;
                }
            }
        }
        return total == 0 ? 0.0 : (presentes * 100.0) / total;
    }

    @Override
    public String toString() {
        return "Asistencia{" + estudiante.getNombreCompleto() + " - " + asignatura.getNombre()
                + " (" + fecha + "): " + (presente ? "Presente" : "Ausente") + "}";
    }
}
