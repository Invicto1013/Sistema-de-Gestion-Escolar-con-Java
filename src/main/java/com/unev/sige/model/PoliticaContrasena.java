package com.unev.sige.model;

import java.nio.charset.StandardCharsets;

/**
 * Reglas de contraseña de todo el sistema (registro, cambio de contraseña y alta de profesores).
 * Un solo lugar para que pantalla y base de datos apliquen exactamente la misma regla.
 */
public final class PoliticaContrasena {

    public static final int LONGITUD_MINIMA = 8;

    /** BCrypt solo usa los primeros 72 bytes: una clave más larga se recortaría en silencio. */
    public static final int BYTES_MAXIMOS = 72;

    public static final String REQUISITOS =
            "Mínimo " + LONGITUD_MINIMA + " caracteres, con al menos una letra y un número.";

    private PoliticaContrasena() {
    }

    /**
     * @return el mensaje del primer requisito que no se cumple, o null si la contraseña es aceptable
     */
    public static String validar(String contrasena) {
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA) {
            return "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.";
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
            return "La contraseña es demasiado larga (máximo " + BYTES_MAXIMOS + " bytes).";
        }
        boolean letra = contrasena.chars().anyMatch(Character::isLetter);
        boolean numero = contrasena.chars().anyMatch(Character::isDigit);
        if (!letra || !numero) {
            return "La contraseña debe incluir al menos una letra y un número.";
        }
        return null;
    }
}
