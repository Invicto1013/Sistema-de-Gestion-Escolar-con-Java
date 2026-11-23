package com.unev.sige.model;

import java.time.LocalDate;
import java.time.Period;

/**
 * Validador de Solicitudes de Permisos, Licencias y Excusas Escolares de Estudiantes.
 * Gestiona la justificación médica o institucional de inasistencias para reprogramación
 * de evaluaciones o pruebas parciales perdidas.
 */
public final class ValidadorExcusaEscolar {

    public static final int PLAZO_MAXIMO_DIAS_JUSTIFICACION = 5;

    public enum TipoExcusa {
        MEDICA(true, "Justificante Médico / Reposo"),
        CALAMIDAD_FAMILIAR(true, "Calamidad o Causa de Fuerza Mayor"),
        REPRESENTACION_INSTITUCIONAL(false, "Representación Deportiva o Académica del Centro"),
        PERSONAL(false, "Asunto Personal / Familiar Particular");

        private final boolean requiereComprobante;
        private final String descripcion;

        TipoExcusa(boolean requiereComprobante, String descripcion) {
            this.requiereComprobante = requiereComprobante;
            this.descripcion = descripcion;
        }

        public boolean requiereComprobante() {
            return requiereComprobante;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public record EvaluacionExcusa(
            boolean esValida,
            boolean habilitaReprogramacionExamen,
            String dictamen
    ) { }

    private ValidadorExcusaEscolar() {
    }

    /**
     * Evalúa si una excusa fue radicada dentro del plazo reglamentario y si habilita reposición de notas.
     */
    public static EvaluacionExcusa evaluar(TipoExcusa tipo, LocalDate fechaInasistencia,
                                          LocalDate fechaPresentacion, boolean adjuntoComprobante) {
        if (tipo == null || fechaInasistencia == null) {
            return new EvaluacionExcusa(false, false, "Datos de excusa incompletos.");
        }

        LocalDate presentacion = fechaPresentacion != null ? fechaPresentacion : LocalDate.now();
        if (presentacion.isBefore(fechaInasistencia)) {
            return new EvaluacionExcusa(false, false, "Fecha de presentación no puede ser previa a la inasistencia.");
        }

        int diasTranscurridos = Period.between(fechaInasistencia, presentacion).getDays();
        if (diasTranscurridos > PLAZO_MAXIMO_DIAS_JUSTIFICACION) {
            return new EvaluacionExcusa(false, false, String.format(
                    "Excusa extemporánea (presentada a los %d días; plazo límite: %d días).",
                    diasTranscurridos, PLAZO_MAXIMO_DIAS_JUSTIFICACION
            ));
        }

        if (tipo.requiereComprobante() && !adjuntoComprobante) {
            return new EvaluacionExcusa(false, false, "Falta adjuntar certificado médico o comprobante justificativo.");
        }

        boolean habilitaExamen = tipo == TipoExcusa.MEDICA
                || tipo == TipoExcusa.CALAMIDAD_FAMILIAR
                || tipo == TipoExcusa.REPRESENTACION_INSTITUCIONAL;

        return new EvaluacionExcusa(true, habilitaExamen, "Excusa aprobada institucionalmente.");
    }
}
