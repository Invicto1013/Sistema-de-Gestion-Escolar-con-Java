package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sistema Autónomo de Nivelación y Segunda Oportunidad Académica (Exámenes de Suficiencia y Recuperación).
 * Modelo propio de SIGE con ponderaciones configurables por el consejo académico de la institución:
 * - Modalidad Nivelación Parcial (evaluación continua + prueba de síntesis).
 * - Modalidad Evaluación de Suficiencia Integral (prueba de reposición o suficiencia global).
 */
public final class CalculoRecuperacion {

    public static final double NOTA_MINIMA_APROBAR_DEFAULT = 70.0;

    public enum ModalidadRecuperacion {
        NIVELACION_COMPUESTA(0.40, 0.60, "Nivelación Compuesta (40% Trabajo Continuo + 60% Evaluación)"),
        SUFICIENCIA_INTEGRAL(0.00, 1.00, "Examen de Suficiencia Integral (100% Reposición Directa)"),
        PONDERACION_EQUILIBRADA(0.50, 0.50, "Ponderación Equilibrada (50% Ordinario + 50% Recuperación)");

        private final double pesoAcumulado;
        private final double pesoExamen;
        private final String descripcion;

        ModalidadRecuperacion(double pesoAcumulado, double pesoExamen, String descripcion) {
            this.pesoAcumulado = pesoAcumulado;
            this.pesoExamen = pesoExamen;
            this.descripcion = descripcion;
        }

        public double getPesoAcumulado() {
            return pesoAcumulado;
        }

        public double getPesoExamen() {
            return pesoExamen;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public record ResultadoRecuperacion(
            double notaFinalRecuperacion,
            String literal,
            boolean aprobada,
            ModalidadRecuperacion modalidad
    ) { }

    private CalculoRecuperacion() {
    }

    /**
     * Calcula la calificación final obtenida en proceso de recuperación bajo las modalidades propias de SIGE.
     */
    public static ResultadoRecuperacion calcular(double promedioAcumulado, double notaExamen, ModalidadRecuperacion modalidad) {
        if (modalidad == null) {
            modalidad = ModalidadRecuperacion.PONDERACION_EQUILIBRADA;
        }

        double acum = Calificacion.limitar(promedioAcumulado);
        double examen = Calificacion.limitar(notaExamen);

        BigDecimal pa = BigDecimal.valueOf(modalidad.getPesoAcumulado());
        BigDecimal pe = BigDecimal.valueOf(modalidad.getPesoExamen());

        BigDecimal notaCalculada = BigDecimal.valueOf(acum).multiply(pa)
                .add(BigDecimal.valueOf(examen).multiply(pe))
                .setScale(2, RoundingMode.HALF_UP);

        double notaFinal = notaCalculada.doubleValue();
        boolean aprueba = EscalaCalificaciones.aprueba(notaFinal);
        String literal = EscalaCalificaciones.literal(notaFinal);

        return new ResultadoRecuperacion(notaFinal, literal, aprueba, modalidad);
    }
}
