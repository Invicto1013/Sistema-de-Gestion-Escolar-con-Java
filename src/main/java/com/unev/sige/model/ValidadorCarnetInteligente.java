package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Generador y Validador de Identificadores para Carnets Estudiantiles Inteligentes (QR / Código de Barras).
 * Estructura del payload del código QR escolar:
 * SIGE-ID:{CUE}:{ANO_VIGENCIA}:{HASH_FIRMA}
 * Permite escaneo ágil en portería, comedor, biblioteca y eventos institucionales.
 */
public final class ValidadorCarnetInteligente {

    private static final Pattern PATRON_CARNET_QR = Pattern.compile("^SIGE-ID:SIGE-[0-9]{4}-[0-9]{4,6}:[0-9]{4}:[A-Z0-9]{6,12}$");

    public record CarnetDigital(
            String codigoQrPayload,
            String cueEstudiante,
            int anioVigencia,
            String hashFirma,
            boolean estaVigente
    ) { }

    private ValidadorCarnetInteligente() {
    }

    /**
     * Valida el formato del contenido leído desde el código QR del carnet escolar.
     */
    public static boolean esPayloadValido(String payload) {
        if (payload == null) return false;
        return PATRON_CARNET_QR.matcher(payload.trim()).matches();
    }

    /**
     * Genera el contenido del código QR para un estudiante y año escolar.
     */
    public static String generarPayloadQr(String cueEstudiante, int anioVigencia, String hashFirma) {
        if (cueEstudiante == null || !cueEstudiante.startsWith("SIGE-")) {
            throw new IllegalArgumentException("CUE de estudiante inválido para emitir carnet.");
        }
        String firma = (hashFirma != null && !hashFirma.isBlank())
                ? hashFirma.trim().toUpperCase()
                : "SIGE01";
        return String.format("SIGE-ID:%s:%d:%s", cueEstudiante.trim(), Math.max(2000, anioVigencia), firma);
    }

    /**
     * Analiza un payload QR y verifica si está vigente respecto al año lectivo actual.
     */
    public static CarnetDigital analizar(String payload, int anioActual) {
        if (!esPayloadValido(payload)) {
            throw new IllegalArgumentException("Payload de carnet inválido.");
        }
        String[] partes = payload.trim().split(":");
        String cue = partes[1];
        int vigencia = Integer.parseInt(partes[2]);
        String firma = partes[3];
        boolean vigente = vigencia >= anioActual;

        return new CarnetDigital(payload.trim(), cue, vigencia, firma, vigente);
    }
}
