package com.unev.sige.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa a un docente del colegio.
 * Hereda de Usuario. Un Profesor imparte varias Asignaturas (1 a muchos).
 */
public class Profesor extends Usuario {

    private String especialidad;
    private int anosExperiencia;
    private List<Asignatura> listaAsignaturasAsignadas;

    public Profesor() {
        super();
        this.listaAsignaturasAsignadas = new ArrayList<>();
    }

    public Profesor(int id, String nombreUsuario, String contrasena, String nombreCompleto, String correo,
                     String especialidad, int anosExperiencia) {
        super(id, nombreUsuario, contrasena, nombreCompleto, correo, Rol.PROFESOR);
        this.especialidad = especialidad;
        this.anosExperiencia = anosExperiencia;
        this.listaAsignaturasAsignadas = new ArrayList<>();
    }

    // ----- Getters y setters -----

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public int getAnosExperiencia() {
        return anosExperiencia;
    }

    public void setAnosExperiencia(int anosExperiencia) {
        this.anosExperiencia = anosExperiencia;
    }

    public List<Asignatura> getListaAsignaturasAsignadas() {
        return listaAsignaturasAsignadas;
    }

    public void setListaAsignaturasAsignadas(List<Asignatura> listaAsignaturasAsignadas) {
        this.listaAsignaturasAsignadas = listaAsignaturasAsignadas;
    }

    // ----- Metodos propios -----

    /**
     * Asigna una asignatura adicional a este profesor (relacion Profesor-Asignatura).
     */
    public void asignarAsignatura(Asignatura asignatura) {
        if (asignatura != null && !listaAsignaturasAsignadas.contains(asignatura)) {
            listaAsignaturasAsignadas.add(asignatura);
            asignatura.setProfesor(this);
        }
    }

    /**
     * Crea y devuelve una nueva calificacion para un estudiante en una asignatura.
     */
    public Calificacion registrarCalificacion(Estudiante estudiante, Asignatura asignatura, String periodo,
                                               double valor, TipoCalificacion tipo) {
        return new Calificacion(0, estudiante, asignatura, periodo, valor, tipo);
    }

    @Override
    public String toString() {
        return "Profesor{" + getNombreCompleto() + ", especialidad='" + especialidad + "'}";
    }
}
