package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Solicitudes de Admisión de Nuevo Ingreso.
 * Controla que el expediente del postulante cuente con toda la documentación preliminar
 * y asigna un código único de solicitud (ADM-YYYY-XXXXX).
 */
public final class ValidadorAdmisionNuevoIngreso {

    private static final Pattern PATRON_SOLICITUD = Pattern.compile("^ADM-[0-9]{4}-[0-9]{5}$");

    public record ExpedienteAdmision(
            String codigoSolicitud,
            String nombrePostulante,
            boolean tieneActaNacimiento,
            boolean tieneCertificadoSalud,
            boolean tieneRecordNotasAnterior,
            boolean cuotaEvaluacionPagada
    ) {
        public boolean esExpedienteCompleto() {
            return tieneActaNacimiento && tieneCertificadoSalud && tieneRecordNotasAnterior && cuotaEvaluacionPagada;
        }
    }

    private ValidadorAdmisionNuevoIngreso() {
    }

    public static boolean esCodigoSolicitudValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_SOLICITUD.matcher(codigo.trim().toUpperCase()).matches();
    }
}
