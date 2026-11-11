package com.unev.sige.data;

/**
 * Se lanza cuando un usuario intenta modificar algo que no le corresponde
 * (por ejemplo, un profesor que intenta calificar una seccion que no es suya).
 */
public class PermisoDenegadoException extends Exception {

    private static final long serialVersionUID = 1L;

    public PermisoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
