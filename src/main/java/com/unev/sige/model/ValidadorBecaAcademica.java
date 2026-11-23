package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculador y validador de Becas Académicas y Exoneraciones Estudiantiles:
 * Clasifica la elegibilidad de becas según el índice académico acumulado:
 * - Beca a la Excelencia Total (100%): Índice >= 95.0 sin materias reprobadas ni sanciones graves.
 * - Media Beca al Mérito (50%): Índice >= 90.0 y < 95.0.
 * - Estímulo al Esfuerzo (25%): Índice >= 85.0 y < 90.0.
 * - Sin Beca (0%): Índice < 85.0 o con sanciones de orientación activas.
 */
public final class ValidadorBecaAcademica {

    public enum TipoBeca {
        EXCELENCIA_TOTAL(100, "Beca de Excelencia Total (100%)"),
        MEDIA_BECA(50, "Media Beca por Mérito Académico (50%)"),
        ESTIMULO_ESFUERZO(25, "Estímulo al Esfuerzo Académico (25%)"),
        NINGUNA(0, "Sin beneficio de beca");

        private final int porcentajeExoneracion;
        private final String descripcion;

        TipoBeca(int porcentaje, String descripcion) {
            this.porcentajeExoneracion = porcentaje;
            this.descripcion = descripcion;
        }

        public int getPorcentajeExoneracion() {
            return porcentajeExoneracion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public record EvaluacionBeca(
            double indice,
            TipoBeca becaAsignada,
            double montoMatriculaOriginal,
            double montoDescuento,
            double montoFinalPagar,
            boolean mantieneBeca
    ) { }

    private ValidadorBecaAcademica() {
    }

    /**
     * Evalúa la beca y calcula el costo final de la matrícula escolar.
     */
    public static EvaluacionBeca evaluar(double indiceAcademico, boolean tieneSancionConducta, double costoMatricula) {
        double ind = Calificacion.limitar(indiceAcademico);
        double costo = Math.max(0.0, costoMatricula);

        TipoBeca beca;
        if (tieneSancionConducta || ind < 85.0) {
            beca = TipoBeca.NINGUNA;
        } else if (ind >= 95.0) {
            beca = TipoBeca.EXCELENCIA_TOTAL;
        } else if (ind >= 90.0) {
            beca = TipoBeca.MEDIA_BECA;
        } else {
            beca = TipoBeca.ESTIMULO_ESFUERZO;
        }

        BigDecimal costoBd = BigDecimal.valueOf(costo);
        BigDecimal pct = BigDecimal.valueOf(beca.getPorcentajeExoneracion()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal descuento = costoBd.multiply(pct).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalPagar = costoBd.subtract(descuento).setScale(2, RoundingMode.HALF_UP);

        boolean mantiene = beca != TipoBeca.NINGUNA;

        return new EvaluacionBeca(ind, beca, costo, descuento.doubleValue(), finalPagar.doubleValue(), mantiene);
    }
}
