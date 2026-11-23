package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Números de Folio y Registro del Libro de Nacimientos (Oficialía del Estado Civil / JCE).
 * En la inscripción escolar dominicana de nivel inicial y primaria, el Acta de Nacimiento
 * se registra formalmente con Oficialía, Libro, Folio y Acta.
 */
public final class ValidadorActaNacimientoEscolar {

    private static final Pattern PATRON_NUMERO = Pattern.compile("^[0-9]{1,6}$");

    public record RegistroNacimiento(
            String oficialia,
            String libro,
            String folio,
            String acta,
            String anioRegistro
    ) {
        public String registroOficial() {
            return String.format("Ofic. %s, Libro %s, Folio %s, Acta %s (%s)",
                    oficialia, libro, folio, acta, anioRegistro != null ? anioRegistro : "");
        }
    }

    private ValidadorActaNacimientoEscolar() {
    }

    /**
     * Valida que los campos registrales del acta de nacimiento sean numéricos y válidos.
     */
    public static boolean esRegistroValido(String oficialia, String libro, String folio, String acta) {
        if (oficialia == null || libro == null || folio == null || acta == null) {
            return false;
        }

        return PATRON_NUMERO.matcher(oficialia.trim()).matches()
                && PATRON_NUMERO.matcher(libro.trim()).matches()
                && PATRON_NUMERO.matcher(folio.trim()).matches()
                && PATRON_NUMERO.matcher(acta.trim()).matches();
    }
}
