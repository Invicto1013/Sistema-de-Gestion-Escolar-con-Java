package com.unev.sige.model;

import java.util.ArrayList;
import java.util.List;

public class Padre extends Usuario {
    private int idPadre;
    private String telefonoContacto;
    private String ocupacion;
    private List<Estudiante> estudiantesA_Cargo;

    public Padre(int id, String nombreUsuario, String contrasena, String nombreCompleto, String correo, Rol rol, 
                 int idPadre, String telefonoContacto, String ocupacion) {
        super(id, nombreUsuario, contrasena, nombreCompleto, correo, rol);
        this.idPadre = idPadre;
        this.telefonoContacto = telefonoContacto;
        this.ocupacion = ocupacion;
        this.estudiantesA_Cargo = new ArrayList<>();
    }

    // Getters y Setters
    public int getIdPadre() { return idPadre; }
    public void setIdPadre(int idPadre) { this.idPadre = idPadre; }

    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }

    public String getOcupacion() { return ocupacion; }
    public void setOcupacion(String ocupacion) { this.ocupacion = ocupacion; }

    public List<Estudiante> getEstudiantesA_Cargo() { return estudiantesA_Cargo; }
    public void agregarEstudiante(Estudiante estudiante) { this.estudiantesA_Cargo.add(estudiante); }
}