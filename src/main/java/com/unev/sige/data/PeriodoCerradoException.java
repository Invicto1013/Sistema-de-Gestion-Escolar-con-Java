package com.unev.sige.data;

/**
 * Se lanza al intentar registrar notas o inscribir en un periodo lectivo cerrado. Es un caso de
 * PermisoDenegadoException, asi que las pantallas que ya muestran ese mensaje siguen funcionando.
 */
public class PeriodoCerradoException extends PermisoDenegadoException {

    private static final long serialVersionUID = 1L;

    public PeriodoCerradoException(String mensaje) {
        super(mensaje);
    }
}
