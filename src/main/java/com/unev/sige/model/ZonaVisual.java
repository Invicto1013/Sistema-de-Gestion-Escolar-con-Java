package com.unev.sige.model;

import java.util.regex.Pattern;

/**
 * Zona horaria con la que se MUESTRAN las fechas guardadas por MySQL (que usa la hora de su servidor, UTC en Aiven).
 * Por defecto -04:00 (Republica Dominicana). Se puede cambiar con la variable SIGE_ZONA_HORARIA (ej. -05:00).
 * Solo cambia como se ven las fechas; los valores guardados no se tocan.
 */
public final class ZonaVisual {

    public static final String PREDETERMINADA = "-04:00";
    private static final Pattern DESPLAZAMIENTO = Pattern.compile("[+-](0[0-9]|1[0-4]):[0-5][0-9]");

    private ZonaVisual() {
    }

    /** Devuelve el desplazamiento valido o la zona predeterminada si el valor falta o es invalido. */
    public static String normalizar(String valor) {
        String limpio = valor == null ? "" : valor.trim();
        return DESPLAZAMIENTO.matcher(limpio).matches() ? limpio : PREDETERMINADA;
    }

    public static String desplazamiento() {
        return normalizar(System.getenv("SIGE_ZONA_HORARIA"));
    }

    /** Expresion SQL que convierte una columna DATETIME de la hora del servidor a la hora de la zona visual. */
    public static String sql(String columna) {
        return "CONVERT_TZ(" + columna + ", @@session.time_zone, '" + desplazamiento() + "')";
    }
}
