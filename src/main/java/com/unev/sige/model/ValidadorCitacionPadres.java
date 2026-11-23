package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Actas y Citaciones a Padres / Tutores por la Dirección Escolar.
 * Genera y valida códigos oficiales de citación (CIT-YYYY-XXXX) y confirma la asistencia del tutor.
 */
public final class ValidadorCitacionPadres {

    private static final Pattern PATRON_CITACION = Pattern.compile("^CIT-[0-9]{4}-[0-9]{4,6}$");

    public record ActaCitacion(
            String codigoCitacion,
            String motivoCitacion,
            String departamentoCitador, // ORIENTACION, DIRECCION, COORDINACION_ACADEMICA
            boolean tutorAsistio,
            String acuerdosFirmados
    ) { }

    private ValidadorCitacionPadres() {
    }

    public static boolean esCodigoCitacionValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_CITACION.matcher(codigo.trim().toUpperCase()).matches();
    }
}
