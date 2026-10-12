package com.unev.sige.model;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

/**
 * Clase base de cualquier persona que inicia sesion en el sistema (tabla `usuarios`).
 * Estudiante, Profesor y Padre heredan de esta clase.
 * La contrasena nunca se guarda en texto plano: se almacena el hash BCrypt.
 */
public class Usuario {

    private int id;
    private String nombre;
    private String apellido;
    private String correo;
    private String contrasenaHash;
    private Rol rol;
    private boolean debeCambiarContrasena;

    private boolean activo = true;

    public Usuario() {
    }

    public Usuario(int id, String nombre, String apellido, String correo, String contrasenaHash, Rol rol) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
    }

    // ----- Getters y setters (encapsulamiento) -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    protected String getContrasenaHash() {
        return contrasenaHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isDebeCambiarContrasena() {
        return debeCambiarContrasena;
    }

    public void setDebeCambiarContrasena(boolean debeCambiarContrasena) {
        this.debeCambiarContrasena = debeCambiarContrasena;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // ----- Metodos propios -----

    /**
     * Genera el hash BCrypt de una contrasena en texto plano.
     */
    public static String hashear(String contrasenaPlana) {
        return BCrypt.hashpw(contrasenaPlana, BCrypt.gensalt());
    }

    /**
     * Valida que el correo y la contrasena ingresados coincidan con los del usuario.
     * Se usa desde LoginController.
     */
    public boolean autenticar(String correoIngresado, String contrasenaIngresada) {
        if (correoIngresado == null || contrasenaIngresada == null
                || correo == null || contrasenaHash == null) {
            return false;
        }
        if (!correo.equalsIgnoreCase(correoIngresado.trim())) {
            return false;
        }
        try {
            return BCrypt.checkpw(contrasenaIngresada, contrasenaHash);
        } catch (IllegalArgumentException e) {
            // El valor guardado en la base de datos no es un hash BCrypt valido.
            return false;
        }
    }

    /**
     * Cambia la contrasena en memoria (se guarda hasheada).
     * Para persistir el cambio hay que actualizar la tabla `usuarios`.
     */
    public void cambiarContrasena(String nuevaContrasena) {
        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
            this.contrasenaHash = hashear(nuevaContrasena);
        }
    }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", correo='" + correo + "', rol=" + rol + "}";
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
