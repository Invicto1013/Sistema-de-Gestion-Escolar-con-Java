package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Calculador de Métricas de Retención y Riesgo de Deserción Escolar.
 * Aplica analítica predictiva temprana combinando inasistencias, rendimiento de notas
 * y sanciones de conducta para detectar estudiantes en riesgo de abandono del centro.
 */
public final class CalculadorRiesgoDesercion {

    public enum NivelRiesgo {
        BAJO("Riesgo Bajo / Desempeño Regular"),
        MEDIO("Riesgo Medio / Monitoreo Recomendado"),
        ALTO("Riesgo Alto / Intervención Prioritaria");

        private final String descripcion;

        NivelRiesgo(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public record DiagnosticoRiesgo(
            double indiceRiesgoPonderado,
            NivelRiesgo nivel,
            boolean requiereIntervencionInmediata,
            String motivoPrincipal
    ) { }

    private CalculadorRiesgoDesercion() {
    }

    /**
     * Calcula el índice de riesgo escolar de 0 a 100:
     * - Inasistencias (peso 40%)
     * - Bajo promedio de calificaciones (peso 40%)
     * - Sanciones disciplinarias (peso 20%)
     */
    public static DiagnosticoRiesgo evaluar(double porcentajeInasistencia, double promedioCalificaciones, int faltasConducta) {
        double inasist = Math.max(0.0, Math.min(100.0, porcentajeInasistencia));
        double prom = Calificacion.limitar(promedioCalificaciones);
        int faltas = Math.max(0, faltasConducta);

        // Componente de notas: si el promedio es 100 el riesgo es 0; si es 50 el riesgo es 50.
        double factorNotas = Math.max(0.0, 100.0 - prom);

        // Componente de faltas: cada falta suma 20 puntos de riesgo disciplinario (tope 100)
        double factorConducta = Math.min(100.0, faltas * 20.0);

        BigDecimal score = BigDecimal.valueOf(inasist).multiply(BigDecimal.valueOf(0.40))
                .add(BigDecimal.valueOf(factorNotas).multiply(BigDecimal.valueOf(0.40)))
                .add(BigDecimal.valueOf(factorConducta).multiply(BigDecimal.valueOf(0.20)))
                .setScale(2, RoundingMode.HALF_UP);

        double totalScore = score.doubleValue();

        NivelRiesgo nivel;
        String motivo;
        if (totalScore >= 60.0) {
            nivel = NivelRiesgo.ALTO;
            motivo = "Combinación crítica de inasistencias o bajas calificaciones";
        } else if (totalScore >= 35.0) {
            nivel = NivelRiesgo.MEDIO;
            motivo = "Indicios tempranos de rezago académico o ausencias reiteradas";
        } else {
            nivel = NivelRiesgo.BAJO;
            motivo = "Indicadores dentro de rangos normales de retención";
        }

        boolean urgente = nivel == NivelRiesgo.ALTO || inasist >= 30.0;

        return new DiagnosticoRiesgo(totalScore, nivel, urgente, motivo);
    }
}
