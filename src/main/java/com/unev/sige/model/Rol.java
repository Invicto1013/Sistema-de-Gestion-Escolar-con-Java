package com.unev.sige.model;

/**
 * Roles posibles de un Usuario dentro del sistema SIGE.
 */
public enum Rol {
    ADMIN("Administrador"),
    PROFESOR("Profesor"),
    ESTUDIANTE("Estudiante"),
    PADRE("Padre / Tutor"),
    DIRECTOR("Director/a"),
    SECRETARIA("Secretario/a");

    private final String etiqueta;

    Rol(String etiqueta) { this.etiqueta = etiqueta; }

    /** Nombre legible para mostrar en pantalla. */
    public String etiqueta() { return etiqueta; }

    /** Roles del personal administrativo que no tienen una tabla de perfil propia. */
    public boolean esPersonalAdministrativo() { return this == DIRECTOR || this == SECRETARIA; }
}
