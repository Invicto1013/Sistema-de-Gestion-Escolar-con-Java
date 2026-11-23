package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Generador y validador de Códigos de Verificación e Integridad Digital de Boletas y Récords Académicos.
 * Permite emitir diplomas, certificaciones de estudio y boletines de notas con un token
 * criptográfico/alfanumérico único verificable a través del portal institucional:
 * https://sige.edu.do/verificar/{token}
 */
public final class ValidadorFirmaDigitalDocumento {

    private static final Pattern PATRON_TOKEN = Pattern.compile("^DOC-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$");

    public record DocumentoCertificado(
            String codigoDocumento,
            String tipoDocumento,
            String matriculaEstudiante,
            String urlVerificacion,
            String hashSeguridad
    ) { }

    private ValidadorFirmaDigitalDocumento() {
    }

    /**
     * Valida si un código de verificación documental cumple el estándar de seguridad de SIGE.
     */
    public static boolean esCodigoValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_TOKEN.matcher(codigo.trim().toUpperCase()).matches();
    }

    /**
     * Genera la URL pública de validación del documento para el portal web institucional.
     */
    public static String generarUrlVerificacion(String dominioBase, String codigoDocumento) {
        if (!esCodigoValido(codigoDocumento)) {
            throw new IllegalArgumentException("Código de documento con formato inválido.");
        }
        String dom = (dominioBase == null || dominioBase.isBlank()) ? "sige.edu.do" : dominioBase.trim();
        if (dom.endsWith("/")) {
            dom = dom.substring(0, dom.length() - 1);
        }
        return String.format("https://%s/verificar/%s", dom, codigoDocumento.trim().toUpperCase());
    }

    /**
     * Construye un registro de documento certificado con su URL de consulta oficial.
     */
    public static DocumentoCertificado certificar(String codigoDocumento, String tipoDocumento,
                                                 String matriculaEstudiante, String hashSeguridad) {
        String url = generarUrlVerificacion("sige.edu.do", codigoDocumento);
        return new DocumentoCertificado(
                codigoDocumento.trim().toUpperCase(),
                tipoDocumento != null ? tipoDocumento.trim() : "BOLETA_OFICIAL",
                matriculaEstudiante != null ? matriculaEstudiante.trim() : "",
                url,
                hashSeguridad != null ? hashSeguridad.trim() : "VALIDO"
        );
    }
}
