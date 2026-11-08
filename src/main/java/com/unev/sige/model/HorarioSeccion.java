package com.unev.sige.model;

import java.time.LocalTime;

/**
 * Bloque horario de una sección escolar (tabla `horarios_secciones`).
 */
public class HorarioSeccion {
    private int id;
    private int idSeccion;
    private String asignaturaNombre;
    private String profesorNombre;
    private String seccionEtiqueta;
    private String diaSemana; // LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String aula;

    public HorarioSeccion() {
    }

    public HorarioSeccion(int id, int idSeccion, String diaSemana, LocalTime horaInicio, LocalTime horaFin, String aula) {
        this.id = id;
        this.idSeccion = idSeccion;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.aula = aula;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdSeccion() { return idSeccion; }
    public void setIdSeccion(int idSeccion) { this.idSeccion = idSeccion; }

    public String getAsignaturaNombre() { return asignaturaNombre; }
    public void setAsignaturaNombre(String asignaturaNombre) { this.asignaturaNombre = asignaturaNombre; }

    public String getProfesorNombre() { return profesorNombre; }
    public void setProfesorNombre(String profesorNombre) { this.profesorNombre = profesorNombre; }

    public String getSeccionEtiqueta() { return seccionEtiqueta; }
    public void setSeccionEtiqueta(String seccionEtiqueta) { this.seccionEtiqueta = seccionEtiqueta; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public String getAula() { return aula; }
    public void setAula(String aula) { this.aula = aula; }

    public String getRangoHora() {
        return (horaInicio != null ? horaInicio.toString() : "") + " - " + (horaFin != null ? horaFin.toString() : "");
    }
}
