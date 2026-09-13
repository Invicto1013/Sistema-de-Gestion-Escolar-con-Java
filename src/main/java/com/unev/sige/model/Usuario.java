package com.unev.sige.model;

import java.util.Objects;

/**
 * Clase base que representa a cualquier persona que inicia sesion en el sistema.
 * Estudiante y Profesor heredan de esta clase (herencia).
 */
public class Usuario {

    private int id;
    private String nombreUsuario;
    private String contrasena;
    private String nombreCompleto;
    private String correo;
    private Rol rol;

    public Usuario() {
    }

    public Usuario(int id, String nombreUsuario, String contrasena, String nombreCompleto, String correo, Rol rol) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.contrasena = contrasena;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.rol = rol;
    }

    // ----- Getters y setters (encapsulamiento) -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    protected String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    // ----- Metodos propios -----

    /**
     * Valida que el usuario y la contrasena ingresados coincidan con los del sistema.
     * Se usara desde LoginController.
     */
    public boolean autenticar(String usuarioIngresado, String contrasenaIngresada) {
        if (usuarioIngresado == null || contrasenaIngresada == null) {
            return false;
        }
        return this.nombreUsuario.equals(usuarioIngresado) && this.contrasena.equals(contrasenaIngresada);
    }

    /**
     * Permite cambiar la contrasena actual por una nueva.
     */
    public void cambiarContrasena(String nuevaContrasena) {
        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
            this.contrasena = nuevaContrasena;
        }
    }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", nombreUsuario='" + nombreUsuario + "', rol=" + rol + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return id == usuario.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
