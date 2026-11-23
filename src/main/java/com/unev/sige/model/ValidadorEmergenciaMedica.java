package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Números de Emergencia y Contacto Médico Escolar (911, Cruz Roja, Bomberos y Centros de Salud).
 * Garantiza que la ficha médica y el protocolo de primeros auxilios del plantel cuenten
 * con números de derivación de urgencia plenamente válidos.
 */
public final class ValidadorEmergenciaMedica {

    private static final Pattern PATRON_EMERGENCIA_CORTO = Pattern.compile("^(?:911|112|462)$");

    public record ContactoEmergencia(
            String nombreContacto,
            String parentescoOInstitucion,
            String telefono,
            String tipoSangre,
            String alergiasOCondicion
    ) {
        public boolean esCompleto() {
            return nombreContacto != null && !nombreContacto.isBlank()
                    && telefono != null && !telefono.isBlank();
        }
    }

    private ValidadorEmergenciaMedica() {
    }

    /**
     * Valida si un número es una línea corta de emergencia nacional (ej. 911)
     * o un teléfono dominicano estándar de 10 dígitos.
     */
    public static boolean esLineaEmergenciaValida(String telefono) {
        if (telefono == null) return false;
        String t = telefono.trim();
        if (PATRON_EMERGENCIA_CORTO.matcher(t).matches()) {
            return true;
        }
        return ValidadorTelefonoDominicano.esValido(t);
    }

    /**
     * Valida el grupo y factor sanguíneo (A+, A-, B+, B-, AB+, AB-, O+, O-).
     */
    public static boolean esTipoSangreValido(String tipoSangre) {
        if (tipoSangre == null) return false;
        String ts = tipoSangre.trim().toUpperCase();
        return ts.matches("^(?:A|B|AB|O)[+-]$");
    }
}
