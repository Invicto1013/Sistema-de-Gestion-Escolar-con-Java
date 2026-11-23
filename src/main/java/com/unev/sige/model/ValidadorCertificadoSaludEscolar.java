package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Folios y Certificados de Salud Escolar y Esquema de Vacunación (PAI - MINERD / Salud Pública).
 * Controla que los alumnos que ingresan por primera vez o realizan actividades de educación física
 * cuenten con certificado médico y esquema de vacunas acreditado.
 */
public final class ValidadorCertificadoSaludEscolar {

    private static final Pattern PATRON_EXEQUATUR = Pattern.compile("^[0-9]{3,6}-[0-9]{2,4}$");

    public record CertificadoSalud(
            String codigoCertificado,
            String exequaturMedico,
            String centroSalud,
            boolean esquemaVacunacionCompleto,
            boolean aptoParaEducacionFisica
    ) { }

    private ValidadorCertificadoSaludEscolar() {
    }

    /**
     * Valida el formato del Exequátur Médico emitido por el Ministerio de Salud Pública de RD (ej. 1234-15).
     */
    public static boolean esExequaturValido(String exequatur) {
        if (exequatur == null) return false;
        return PATRON_EXEQUATUR.matcher(exequatur.trim()).matches();
    }

    /**
     * Determina si el expediente de salud habilita al estudiante para matrícula sin restricciones.
     */
    public static boolean esExpedienteApto(CertificadoSalud certificado) {
        if (certificado == null) return false;
        return certificado.esquemaVacunacionCompleto()
                && esExequaturValido(certificado.exequaturMedico())
                && certificado.centroSalud() != null && !certificado.centroSalud().isBlank();
    }
}
