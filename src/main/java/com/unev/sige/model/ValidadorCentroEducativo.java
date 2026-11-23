package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador del Perfil de Institución Educativa y Campus Escolar.
 * Modelo de datos institucional propio e independiente para colegios, escuelas y universidades privadas:
 * - Código Institucional de Sede o Campus (ej. CAMPUS-01, SEDE-CENTRAL)
 * - Nombre y Siglas Institucionales
 * - RNC de la personería jurídica o entidad titular
 * - Nivel de acreditación o modalidad interna (Bilingüe, Técnico, General)
 */
public final class ValidadorCentroEducativo {

    private static final Pattern PATRON_CODIGO_CAMPUS = Pattern.compile("^[A-Z0-9-]{3,12}$");
    private static final Pattern PATRON_RNC = Pattern.compile("^[0-9]{9}$");

    public record CentroEducativo(
            String codigoCampus,
            String nombreInstitucion,
            String siglas,
            String modalidad,
            String rnc
    ) {
        public String fichaIdentidad() {
            return String.format("%s (%s) - Sede: %s [Modalidad: %s]",
                    nombreInstitucion,
                    siglas != null ? siglas : "SIGE",
                    codigoCampus,
                    modalidad != null ? modalidad : "General");
        }
    }

    private ValidadorCentroEducativo() {
    }

    /**
     * Valida si el código de campus o sede cumple la nomenclatura institucional (3 a 12 caracteres alfanuméricos).
     */
    public static boolean esCodigoCentroValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_CODIGO_CAMPUS.matcher(codigo.trim().toUpperCase()).matches();
    }

    /**
     * Valida el RNC institucional de 9 dígitos.
     */
    public static boolean esRncInstitucionalValido(String rnc) {
        if (rnc == null) return false;
        return PATRON_RNC.matcher(rnc.trim()).matches();
    }

    /**
     * Valida que las siglas institucionales sean alfabéticas de 2 a 8 caracteres.
     */
    public static boolean esSiglasValidas(String siglas) {
        if (siglas == null) return false;
        return siglas.trim().toUpperCase().matches("^[A-Z]{2,8}$");
    }
}
