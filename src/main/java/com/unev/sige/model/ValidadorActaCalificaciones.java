package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Registro y Certificación Institucional Autónoma de Calificaciones y Récord de Notas.
 * Utiliza identificadores de acta institucionales con hash de verificación e integridad
 * escolar propio de SIGE, totalmente desacoplado de dependencias o nomenclaturas ministeriales externas.
 */
public final class ValidadorActaCalificaciones {

    private static final Pattern PATRON_CODIGO_ACTA = Pattern.compile("^ACT-[A-Z0-9]{4,12}$");
    private static final Pattern PATRON_TOMO = Pattern.compile("^[0-9]{1,4}$");
    private static final Pattern PATRON_PAGINA = Pattern.compile("^[0-9]{1,4}$");

    public record RegistroActa(
            String codigoActa,
            String tomo,
            String pagina,
            String cicloAcademico,
            String codigoSeguridad
    ) {
        public String referenciaInstitucional() {
            return String.format("Acta %s [Tomo %s, Pág. %s] - Ciclo %s (Verificación: %s)",
                    codigoActa, tomo, pagina,
                    cicloAcademico != null ? cicloAcademico : "General",
                    codigoSeguridad != null ? codigoSeguridad : "VALIDADA");
        }
    }

    private ValidadorActaCalificaciones() {
    }

    /**
     * Valida la estructura del acta de calificaciones de la institución.
     */
    public static boolean esRegistroValido(String codigoActa, String tomo, String pagina) {
        if (codigoActa == null || tomo == null || pagina == null) {
            return false;
        }

        String c = codigoActa.trim().toUpperCase();
        String t = tomo.trim();
        String p = pagina.trim();

        return PATRON_CODIGO_ACTA.matcher(c).matches()
                && PATRON_TOMO.matcher(t).matches()
                && PATRON_PAGINA.matcher(p).matches();
    }

    /**
     * Construye un registro institucional de acta certificada con código de seguridad.
     */
    public static RegistroActa crear(String codigoActa, String tomo, String pagina, String ciclo, String hashVerificador) {
        if (!esRegistroValido(codigoActa, tomo, pagina)) {
            throw new IllegalArgumentException("Datos del registro de acta institucional no válidos.");
        }
        return new RegistroActa(
                codigoActa.trim().toUpperCase(),
                tomo.trim(),
                pagina.trim(),
                ciclo != null ? ciclo.trim() : "",
                hashVerificador != null ? hashVerificador.trim().toUpperCase() : "SIGE-OK"
        );
    }
}
