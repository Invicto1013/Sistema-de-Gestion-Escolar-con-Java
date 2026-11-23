package com.unev.sige.model;

import java.util.Collection;

/**
 * Gestor de la Escuela para Padres y Talleres de Formación a Familias.
 * Acredita la participación de los tutores en actividades formativas del plantel.
 */
public final class GestorEscuelaParaPadres {

    public static final int TALLERES_MINIMOS_CICLO = 3;

    public record AsistenciaTaller(
            String nombreTaller,
            String fecha,
            boolean asistio
    ) { }

    private GestorEscuelaParaPadres() {
    }

    public static boolean cumpleCompromisoFamiliar(Collection<AsistenciaTaller> talleres) {
        if (talleres == null) return false;
        long asistencias = talleres.stream().filter(t -> t != null && t.asistio()).count();
        return asistencias >= TALLERES_MINIMOS_CICLO;
    }
}
