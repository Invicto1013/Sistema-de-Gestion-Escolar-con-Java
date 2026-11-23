package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador oficial de documentos de identidad nacional dominicana (Cédula de Identidad y Electoral).
 * Aplica el algoritmo oficial Módulo 10 (Luhn con pesos alternados 1 y 2) para verificar
 * la legitimidad de las cédulas de docentes, padres/tutores y estudiantes mayores de edad.
 */
public final class ValidadorCedulaDominicana {

    private static final Pattern PATRON_FORMATO = Pattern.compile("^[0-9]{11}$");

    private ValidadorCedulaDominicana() {
    }

    /**
     * Limpia y normaliza una cédula eliminando guiones y espacios.
     */
    public static String limpiar(String cedula) {
        if (cedula == null) return "";
        return cedula.replaceAll("[^0-9]", "");
    }

    /**
     * Valida una cédula dominicana de 11 dígitos mediante el algoritmo oficial Módulo 10.
     */
    public static boolean esValida(String cedula) {
        String limpia = limpiar(cedula);
        if (!PATRON_FORMATO.matcher(limpia).matches()) {
            return false;
        }

        // Rechazar secuencias repetitivas comunes (00000000000, 11111111111, etc.)
        if (limpia.chars().distinct().count() == 1) {
            return false;
        }

        int[] pesos = {1, 2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;

        for (int i = 0; i < 10; i++) {
            int digito = Character.getNumericValue(limpia.charAt(i));
            int producto = digito * pesos[i];
            if (producto >= 10) {
                producto = (producto / 10) + (producto % 10);
            }
            suma += producto;
        }

        int digitoVerificadorCalculado = (10 - (suma % 10)) % 10;
        int digitoVerificadorReal = Character.getNumericValue(limpia.charAt(10));

        return digitoVerificadorCalculado == digitoVerificadorReal;
    }

    /**
     * Formatea una cédula de 11 dígitos al formato estándar visual (001-0000000-0).
     */
    public static String formatear(String cedula) {
        String limpia = limpiar(cedula);
        if (limpia.length() != 11) {
            return cedula != null ? cedula.trim() : "";
        }
        return String.format("%s-%s-%s",
                limpia.substring(0, 3),
                limpia.substring(3, 10),
                limpia.substring(10, 11));
    }
}
