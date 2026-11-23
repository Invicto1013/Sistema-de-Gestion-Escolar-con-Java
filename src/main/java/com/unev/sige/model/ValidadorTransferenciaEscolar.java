package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Solicitudes y Actas de Transferencia Escolar Interinstitucional.
 * Gestiona el pase o traslado de alumnos entre diferentes instituciones educativas,
 * validando el código de transferencia y la expedición del certificado de no adeudo.
 */
public final class ValidadorTransferenciaEscolar {

    private static final Pattern PATRON_TRANSFERENCIA = Pattern.compile("^TRF-[0-9]{4}-[A-Z0-9]{5,8}$");

    public record SolicitudTransferencia(
            String codigoTransferencia,
            String centroOrigen,
            String centroDestino,
            String motivo,
            boolean pazYSalvoFinanciero,
            boolean recordNotasAnexo
    ) {
        public boolean puedeProceder() {
            return pazYSalvoFinanciero && recordNotasAnexo;
        }
    }

    private ValidadorTransferenciaEscolar() {
    }

    /**
     * Valida el formato del código oficial de transferencia de matrícula.
     */
    public static boolean esCodigoTransferenciaValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_TRANSFERENCIA.matcher(codigo.trim().toUpperCase()).matches();
    }

    /**
     * Evalúa si una solicitud de transferencia cumple con los recaudos obligatorios.
     */
    public static boolean esSolicitudValida(SolicitudTransferencia solicitud) {
        if (solicitud == null) return false;
        return esCodigoTransferenciaValido(solicitud.codigoTransferencia())
                && solicitud.centroOrigen() != null && !solicitud.centroOrigen().isBlank()
                && solicitud.centroDestino() != null && !solicitud.centroDestino().isBlank()
                && solicitud.puedeProceder();
    }
}
