package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de identificadores institucionales autónomos y propios de SIGE.
 * Diseñado con formato y nomenclatura independiente de plataformas gubernamentales externas:
 * - Código Único de Estudiante (CUE): SIGE-YYYY-XXXXX (ej. SIGE-2026-00142)
 * - Código Institucional de Asignatura: SIGE-CURR-XXX (ej. SIGE-MAT-101)
 * - Identificador de Matrícula Institucional: Formato libre o alfanumérico autónomo del centro.
 */
public final class IdentificadorEscolar {

    private static final Pattern PATRON_CUE_SIGE = Pattern.compile("^SIGE-20[0-9]{2}-[0-9]{4,6}$");
    private static final Pattern PATRON_MATRICULA_ESTANDAR = Pattern.compile("^(?:[A-Z0-9]{2,6}-)?20[0-9]{2}-[0-9]{4,6}$");
    private static final Pattern PATRON_CODIGO_MATERIA = Pattern.compile("^(?:SIGE-)?[A-Z]{2,4}-?[0-9]{3}$");

    private IdentificadorEscolar() {
    }

    /**
     * Valida el Código Único de Estudiante (CUE) de la plataforma SIGE.
     */
    public static boolean esCueSigeValido(String cue) {
        if (cue == null) return false;
        return PATRON_CUE_SIGE.matcher(cue.trim().toUpperCase()).matches();
    }

    /**
     * Valida una matrícula escolar bajo el formato institucional del centro.
     */
    public static boolean esMatriculaValida(String matricula) {
        if (matricula == null) return false;
        return PATRON_MATRICULA_ESTANDAR.matcher(matricula.trim().toUpperCase()).matches();
    }

    /**
     * Valida un código de asignatura del catálogo propio del centro educativo.
     */
    public static boolean esCodigoMateriaValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_CODIGO_MATERIA.matcher(codigo.trim().toUpperCase()).matches();
    }

    /**
     * Genera el Código Único de Estudiante (CUE) autónomo de SIGE.
     */
    public static String generarCue(int anio, int consecutivo) {
        return String.format("SIGE-%04d-%05d", Math.max(2000, anio), Math.max(1, consecutivo));
    }

    /**
     * Normaliza una matrícula poniéndola en mayúsculas y sin espacios.
     */
    public static String normalizarMatricula(String matricula) {
        if (matricula == null) return "";
        return matricula.trim().toUpperCase();
    }
}
