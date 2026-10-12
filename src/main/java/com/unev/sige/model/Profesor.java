package com.unev.sige.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Docente del centro (tabla `profesores`, comparte id con `usuarios`).
 * Hereda de Usuario. Un profesor imparte varias secciones (1 a muchos).
 */
public class Profesor extends Usuario {

    private String codigoProfesor;
    private String especialidad;
    private List<Seccion> secciones;

    public Profesor() {
        super();
        this.secciones = new ArrayList<>();
    }

    public Profesor(int id, String nombre, String apellido, String correo, String contrasenaHash,
                    String codigoProfesor, String especialidad) {
        super(id, nombre, apellido, correo, contrasenaHash, Rol.PROFESOR);
        this.codigoProfesor = codigoProfesor;
        this.especialidad = especialidad;
        this.secciones = new ArrayList<>();
    }

    // ----- Getters y setters -----

    public String getCodigoProfesor() {
        return codigoProfesor;
    }

    public void setCodigoProfesor(String codigoProfesor) {
        this.codigoProfesor = codigoProfesor;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public List<Seccion> getSecciones() {
        return secciones;
    }

    // ----- Metodos propios -----

    /**
     * Asigna una seccion a este profesor (relacion Profesor-Seccion).
     */
    public void asignarSeccion(Seccion seccion) {
        if (seccion != null && !secciones.contains(seccion)) {
            secciones.add(seccion);
            seccion.setProfesor(this);
        }
    }

    /**
     * Crea una calificacion para una materia inscrita (detalle de inscripcion).
     * Aun no se guarda en la base de datos.
     */
    public Calificacion registrarCalificacion(int idDetalleInscripcion, double notaParcial1, double notaParcial2,
                                              double notaExamenFinal, double notaFinal) {
        return new Calificacion(0, idDetalleInscripcion, notaParcial1, notaParcial2,
                notaExamenFinal, notaFinal, "");
    }

    @Override
    public String toString() {
        return "Profesor{" + getNombreCompleto() + ", especialidad='" + especialidad + "'}";
    }
}
