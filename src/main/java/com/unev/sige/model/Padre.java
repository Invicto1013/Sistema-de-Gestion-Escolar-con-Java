package com.unev.sige.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Padre, madre o tutor (tabla `padres`, enlazada a `usuarios` por id_usuario).
 * La relacion con los estudiantes esta en la tabla `estudiante_padre`.
 */
public class Padre extends Usuario {

    private int idPadre;
    private String telefonoContacto;
    private String ocupacion;
    private List<Estudiante> estudiantesACargo;

    public Padre(int id, String nombre, String apellido, String correo, String contrasenaHash,
                 int idPadre, String telefonoContacto, String ocupacion) {
        super(id, nombre, apellido, correo, contrasenaHash, Rol.PADRE);
        this.idPadre = idPadre;
        this.telefonoContacto = telefonoContacto;
        this.ocupacion = ocupacion;
        this.estudiantesACargo = new ArrayList<>();
    }

    // Getters y setters
    public int getIdPadre() { return idPadre; }
    public void setIdPadre(int idPadre) { this.idPadre = idPadre; }

    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }

    public String getOcupacion() { return ocupacion; }
    public void setOcupacion(String ocupacion) { this.ocupacion = ocupacion; }

    public List<Estudiante> getEstudiantesACargo() { return estudiantesACargo; }

    public void agregarEstudiante(Estudiante estudiante) {
        if (estudiante != null && !estudiantesACargo.contains(estudiante)) {
            estudiantesACargo.add(estudiante);
        }
    }

    @Override
    public String toString() {
        return "Padre{" + getNombreCompleto() + ", telefono='" + telefonoContacto + "'}";
    }
}
