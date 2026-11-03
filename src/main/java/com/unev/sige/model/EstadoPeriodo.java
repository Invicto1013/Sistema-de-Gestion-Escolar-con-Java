package com.unev.sige.model;

/** Estado de un periodo lectivo: ABIERTO admite inscripciones y notas; CERRADO las bloquea. */
public enum EstadoPeriodo {
    ABIERTO, CERRADO;

    /** Convierte el texto guardado en MySQL; un periodo sin registro (datos antiguos) cuenta como ABIERTO. */
    public static EstadoPeriodo desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return ABIERTO;
        }
        return valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
