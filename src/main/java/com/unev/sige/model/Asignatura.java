package com.unev.sige.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una materia del plan de estudios (ej. Matematica, Espanol).
 * Pertenece a un Profesor. Se relaciona con Estudiante a traves de la matricula.
 */
public class Asignatura {

    private String codigo;
    private String nombre;
    private String descripcion;
    private String grado;
    private Profesor profesor;
    private List<Estudiante> estudiantesMatriculados;

    public Asignatura() {
        this.estudiantesMatriculados = new ArrayList<>();
    }

    public Asignatura(String codigo, String nombre, String descripcion, String grado, Profesor profesor) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.grado = grado;
        this.profesor = profesor;
        this.estudiantesMatriculados = new ArrayList<>();
    }

    // ----- Getters y setters -----

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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getGrado() {
        return grado;
    }

    public void setGrado(String grado) {
        this.grado = grado;
    }

    public Profesor getProfesor() {
        return profesor;
    }

    public void setProfesor(Profesor profesor) {
        this.profesor = profesor;
    }

    // ----- Metodos propios -----

    /**
     * Matricula a un estudiante en esta asignatura (relacion Asignatura-Estudiante).
     */
    public void matricular(Estudiante estudiante) {
        if (estudiante != null && !estudiantesMatriculados.contains(estudiante)) {
            estudiantesMatriculados.add(estudiante);
            estudiante.matricularEn(this);
        }
    }

    /**
     * Devuelve la lista de estudiantes matriculados en esta asignatura.
     */
    public List<Estudiante> listarEstudiantesMatriculados() {
        return estudiantesMatriculados;
    }

    @Override
    public String toString() {
        return "Asignatura{" + codigo + " - " + nombre + "}";
    }
}
