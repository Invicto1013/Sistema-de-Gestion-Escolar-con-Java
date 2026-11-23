package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de correos electrónicos institucionales (@sige.edu.do o dominio escolar configurado).
 * Permite distinguir cuentas oficiales de estudiantes y docentes frente a correos personales
 * de padres/tutores, y valida que las credenciales de SSO del Aula Virtual cumplan el formato normativo.
 */
public final class ValidadorCorreoInstitucional {

    public static final String DOMINIO_PREDETERMINADO = "sige.edu.do";
    private static final Pattern PATRON_EMAIL_GENERAL =
            Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$");

    private ValidadorCorreoInstitucional() {
    }

    /**
     * Comprueba si el correo cumple la sintaxis básica RFC 5322 simplificada.
     */
    public static boolean esFormatoValido(String email) {
        if (email == null) return false;
        String e = email.trim();
        return !e.isEmpty() && e.length() <= 254 && PATRON_EMAIL_GENERAL.matcher(e).matches();
    }

    /**
     * Valida si un correo pertenece al dominio institucional del centro escolar.
     */
    public static boolean esCorreoInstitucional(String email, String dominioEsperado) {
        if (!esFormatoValido(email)) return false;
        String dom = (dominioEsperado == null || dominioEsperado.isBlank()) ? DOMINIO_PREDETERMINADO : dominioEsperado.trim().toLowerCase();
        String correoLimpio = email.trim().toLowerCase();
        return correoLimpio.endsWith("@" + dom);
    }

    /**
     * Genera la sugerencia de correo institucional para un nuevo alumno o docente a partir de sus nombres.
     * Ejemplo: "Juan", "Perez", "2026", "sige.edu.do" -> "juan.perez26@sige.edu.do"
     */
    /** Convierte «Gómez» en «Gomez» (y la ñ en n) para que las tildes no se pierdan al armar el correo. */
    private static String sinTildes(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public static String sugerirCorreo(String primerNombre, String primerApellido, String anio, String dominio) {
        String n = primerNombre == null ? "usuario" : sinTildes(primerNombre).trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        String a = primerApellido == null ? "estudiante" : sinTildes(primerApellido).trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        String sufijo = (anio != null && anio.length() >= 2) ? anio.substring(anio.length() - 2) : "";
        String dom = (dominio != null && !dominio.isBlank()) ? dominio.trim().toLowerCase() : DOMINIO_PREDETERMINADO;

        return String.format("%s.%s%s@%s", n, a, sufijo, dom);
    }
}
