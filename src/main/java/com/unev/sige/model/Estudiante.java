package com.unev.sige.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa a un alumno matriculado en el colegio.
 * Hereda de Usuario (id, nombreUsuario, contrasena, correo, rol).
 */
public class Estudiante extends Usuario {

    private String matricula;
    private LocalDate fechaNacimiento;
    private String grado;
    private String seccion;
    private List<Asignatura> listaAsignaturas;

    public Estudiante() {
        super();
        this.listaAsignaturas = new ArrayList<>();
    }

    public Estudiante(int id, String nombreUsuario, String contrasena, String nombreCompleto, String correo,
                       String matricula, LocalDate fechaNacimiento, String grado, String seccion) {
        super(id, nombreUsuario, contrasena, nombreCompleto, correo, Rol.ESTUDIANTE);
        this.matricula = matricula;
        this.fechaNacimiento = fechaNacimiento;
        this.grado = grado;
        this.seccion = seccion;
        this.listaAsignaturas = new ArrayList<>();
    }

    // ----- Getters y setters -----

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getGrado() {
        return grado;
    }

    public void setGrado(String grado) {
        this.grado = grado;
    }

    public String getSeccion() {
        return seccion;
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    public List<Asignatura> getListaAsignaturas() {
        return listaAsignaturas;
    }

    public void setListaAsignaturas(List<Asignatura> listaAsignaturas) {
        this.listaAsignaturas = listaAsignaturas;
    }

    // ----- Metodos propios -----

    /**
     * Matricula al estudiante en una asignatura (relacion Estudiante-Asignatura).
     */
    public void matricularEn(Asignatura asignatura) {
        if (asignatura != null && !listaAsignaturas.contains(asignatura)) {
            listaAsignaturas.add(asignatura);
        }
    }

    /**
     * Calcula el promedio del estudiante a partir de una lista de calificaciones.
     */
    public double obtenerPromedio(List<Calificacion> calificaciones) {
        if (calificaciones == null || calificaciones.isEmpty()) {
            return 0.0;
        }
        double suma = 0.0;
        int cantidad = 0;
        for (Calificacion c : calificaciones) {
            if (c.getEstudiante().equals(this)) {
                suma += c.getValor();
                cantidad++;
            }
        }
        return cantidad == 0 ? 0.0 : suma / cantidad;
    }

    @Override
    public String toString() {
        return "Estudiante{" + getNombreCompleto() + ", matricula='" + matricula + "', grado='" + grado + "'}";
    }
}
