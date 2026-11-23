package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador y normalizador de números telefónicos dominicanos:
 * Prefijos válidos: +1-809, +1-829 y +1-849 (código de país NANP +1 y códigos de área RD).
 * Usado para contacto de emergencia de estudiantes y tutores, avisos SMS y llamadas institucionales.
 */
public final class ValidadorTelefonoDominicano {

    private static final Pattern PATRON_DIGITOS_RD = Pattern.compile("^(?:1)?(809|829|849)[0-9]{7}$");

    private ValidadorTelefonoDominicano() {
    }

    /**
     * Limpia un teléfono quitando espacios, guiones, puntos y paréntesis.
     */
    public static String limpiar(String telefono) {
        if (telefono == null) return "";
        return telefono.replaceAll("[^0-9]", "");
    }

    /**
     * Valida si un número telefónico pertenece a los códigos de área de República Dominicana.
     */
    public static boolean esValido(String telefono) {
        String limpio = limpiar(telefono);
        return PATRON_DIGITOS_RD.matcher(limpio).matches();
    }

    /**
     * Formatea un teléfono de 10 dígitos (o con 1 inicial) al formato oficial legible (809-555-1234).
     */
    public static String formatear(String telefono) {
        String limpio = limpiar(telefono);
        if (limpio.startsWith("1") && limpio.length() == 11) {
            limpio = limpio.substring(1);
        }
        if (limpio.length() != 10) {
            return telefono != null ? telefono.trim() : "";
        }
        return String.format("(%s) %s-%s",
                limpio.substring(0, 3),
                limpio.substring(3, 6),
                limpio.substring(6, 10));
    }
}
