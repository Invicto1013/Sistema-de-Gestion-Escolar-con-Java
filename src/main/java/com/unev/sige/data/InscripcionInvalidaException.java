package com.unev.sige.data;

/**
 * Se lanza cuando una inscripcion incumple una regla del negocio (sin cupo, materia repetida...).
 * El mensaje esta pensado para mostrarse tal cual al usuario.
 */
public class InscripcionInvalidaException extends Exception {

    public InscripcionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
