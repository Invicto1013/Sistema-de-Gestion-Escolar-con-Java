package com.unev.sige.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Alumno del centro (tabla `estudiantes`, comparte id con `usuarios`).
 * Hereda de Usuario.
 */
public class Estudiante extends Usuario {

    private String matricula;
    private String grado;
    private double promedioAcademico;
    private List<Inscripcion> inscripciones;

    public Estudiante() {
        super();
        this.inscripciones = new ArrayList<>();
    }

    public Estudiante(int id, String nombre, String apellido, String correo, String contrasenaHash,
                      String matricula, String grado, double promedioAcademico) {
        super(id, nombre, apellido, correo, contrasenaHash, Rol.ESTUDIANTE);
        this.matricula = matricula;
        this.grado = grado;
        this.promedioAcademico = promedioAcademico;
        this.inscripciones = new ArrayList<>();
    }

    // ----- Getters y setters -----

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getGrado() {
        return grado;
    }

    public void setGrado(String grado) {
        this.grado = grado;
    }

    public double getPromedioAcademico() {
        return promedioAcademico;
    }

    public void setPromedioAcademico(double promedioAcademico) {
        this.promedioAcademico = promedioAcademico;
    }

    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }

    // ----- Metodos propios -----

    /**
     * Agrega una inscripcion (periodo lectivo) a este estudiante.
     */
    public void agregarInscripcion(Inscripcion inscripcion) {
        if (inscripcion != null && !inscripciones.contains(inscripcion)) {
            inscripciones.add(inscripcion);
        }
    }

    /**
     * Calcula el promedio de la nota final de las calificaciones recibidas
     * (se espera que sean las de este estudiante).
     */
    public double obtenerPromedio(List<Calificacion> calificaciones) {
        if (calificaciones == null || calificaciones.isEmpty()) {
            return 0.0;
        }
        double suma = 0.0;
        for (Calificacion c : calificaciones) {
            suma += c.getNotaFinal();
        }
        return suma / calificaciones.size();
    }

    @Override
    public String toString() {
        return "Estudiante{" + getNombreCompleto() + ", matricula='" + matricula + "', grado='" + grado + "'}";
    }
}
