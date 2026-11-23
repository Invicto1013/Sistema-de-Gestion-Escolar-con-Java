package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Reglas escolares para evaluación de asistencia académica.
 * En centros educativos y universidades dominicanas / MINERD:
 * - Un porcentaje de asistencia >= 80% mantiene derecho a examen ordinario.
 * - Un porcentaje entre 70% y 79% sitúa al estudiante en "Alerta por Inasistencias".
 * - Un porcentaje < 70% o acumular más de un umbral de inasistencias injustificadas
 *   conlleva a la pérdida del derecho a examen o condición de "Reprobado por Inasistencia" (R/I).
 */
public final class ReglasAsistencia {

    /** Porcentaje mínimo de asistencia requerido para derecho a examen regular (80%). */
    public static final double PORCENTAJE_MINIMO_DERECHO_EXAMEN = 80.0;

    /** Porcentaje límite para alerta temprana por inasistencias (70%). */
    public static final double PORCENTAJE_ALERTA = 70.0;

    public enum EstadoAsistencia {
        REGULAR("Derecho completo a evaluación"),
        ALERTA("Riesgo por inasistencias acumuladas"),
        SIN_DERECHO("Pérdida de derecho a examen por inasistencia");

        private final String descripcion;

        EstadoAsistencia(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public record EvaluacionAsistencia(
            int clasesTotales,
            int asistencias,
            int inasistencias,
            double porcentajeAsistencia,
            EstadoAsistencia estado,
            boolean tieneDerechoExamen
    ) { }

    private ReglasAsistencia() {
    }

    /**
     * Calcula el porcentaje de asistencia con exactitud decimal fija (2 decimales, HALF_UP).
     */
    public static double calcularPorcentaje(int totalClases, int presentes) {
        if (totalClases <= 0 || presentes <= 0) {
            return 0.0;
        }
        if (presentes >= totalClases) {
            return 100.0;
        }
        return BigDecimal.valueOf(presentes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalClases), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Evalúa el estado de asistencia de una materia a partir de un ResumenAsistencia.
     */
    public static EvaluacionAsistencia evaluar(ResumenAsistencia resumen) {
        if (resumen == null || resumen.clasesRegistradas() == 0) {
            return new EvaluacionAsistencia(0, 0, 0, 100.0, EstadoAsistencia.REGULAR, true);
        }

        int totales = resumen.clasesRegistradas();
        int presentes = Math.max(0, resumen.presentes());
        int ausentes = Math.max(0, resumen.ausentes());
        double pct = calcularPorcentaje(totales, presentes);

        EstadoAsistencia estado;
        boolean derecho;

        if (pct >= PORCENTAJE_MINIMO_DERECHO_EXAMEN) {
            estado = EstadoAsistencia.REGULAR;
            derecho = true;
        } else if (pct >= PORCENTAJE_ALERTA) {
            estado = EstadoAsistencia.ALERTA;
            derecho = true;
        } else {
            estado = EstadoAsistencia.SIN_DERECHO;
            derecho = false;
        }

        return new EvaluacionAsistencia(totales, presentes, ausentes, pct, estado, derecho);
    }

    /**
     * Evalúa el consolidado general de todas las materias de un estudiante.
     */
    public static EvaluacionAsistencia evaluarConsolidado(Collection<ResumenAsistencia> materias) {
        if (materias == null || materias.isEmpty()) {
            return new EvaluacionAsistencia(0, 0, 0, 100.0, EstadoAsistencia.REGULAR, true);
        }

        int totalClases = 0;
        int totalPresentes = 0;
        int totalAusentes = 0;

        for (ResumenAsistencia r : materias) {
            if (r == null) continue;
            totalClases += r.clasesRegistradas();
            totalPresentes += r.presentes();
            totalAusentes += r.ausentes();
        }

        if (totalClases == 0) {
            return new EvaluacionAsistencia(0, 0, 0, 100.0, EstadoAsistencia.REGULAR, true);
        }

        double pct = calcularPorcentaje(totalClases, totalPresentes);
        EstadoAsistencia estado;
        boolean derecho;

        if (pct >= PORCENTAJE_MINIMO_DERECHO_EXAMEN) {
            estado = EstadoAsistencia.REGULAR;
            derecho = true;
        } else if (pct >= PORCENTAJE_ALERTA) {
            estado = EstadoAsistencia.ALERTA;
            derecho = true;
        } else {
            estado = EstadoAsistencia.SIN_DERECHO;
            derecho = false;
        }

        return new EvaluacionAsistencia(totalClases, totalPresentes, totalAusentes, pct, estado, derecho);
    }
}
