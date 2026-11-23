package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Valida y registra planes de refuerzo académico para estudiantes
 * con bajo rendimiento, conforme a los criterios del SIGE.
 */
public final class ValidadorPlanRefuerzoAcademico {

    private static final BigDecimal NOTA_MINIMA_REFUERZO = new BigDecimal("70.00");
    private static final int MAX_SESIONES = 30;
    private static final int MIN_SESIONES = 3;

    public enum EstatusRefuerzo { ACTIVO, COMPLETADO, ABANDONADO, SUSPENDIDO }

    public record PlanRefuerzo(
            String codigoPlan,
            String codigoEstudiante,
            String asignatura,
            BigDecimal notaInicial,
            int sesionesPlanificadas,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstatusRefuerzo estatus
    ) {}

    private ValidadorPlanRefuerzoAcademico() {}

    public static boolean requiereRefuerzo(BigDecimal nota) {
        Objects.requireNonNull(nota, "La nota no puede ser nula");
        return nota.compareTo(NOTA_MINIMA_REFUERZO) < 0;
    }

    public static ResultadoValidacion validar(PlanRefuerzo plan) {
        Objects.requireNonNull(plan, "El plan no puede ser nulo");

        if (plan.codigoPlan() == null || !plan.codigoPlan().startsWith("REF-")) {
            return ResultadoValidacion.rechazar("Código de plan inválido; debe iniciar con REF-");
        }
        if (!requiereRefuerzo(plan.notaInicial())) {
            return ResultadoValidacion.rechazar(
                    "El estudiante no requiere refuerzo (nota " + plan.notaInicial() + " >= 70)");
        }
        if (plan.sesionesPlanificadas() < MIN_SESIONES || plan.sesionesPlanificadas() > MAX_SESIONES) {
            return ResultadoValidacion.rechazar(
                    "Las sesiones deben estar entre " + MIN_SESIONES + " y " + MAX_SESIONES);
        }
        if (plan.fechaInicio() == null || plan.fechaFin() == null) {
            return ResultadoValidacion.rechazar("Fechas de inicio y fin son obligatorias");
        }
        if (!plan.fechaFin().isAfter(plan.fechaInicio())) {
            return ResultadoValidacion.rechazar("La fecha de fin debe ser posterior a la de inicio");
        }
        if (plan.asignatura() == null || plan.asignatura().isBlank()) {
            return ResultadoValidacion.rechazar("La asignatura no puede estar vacía");
        }
        return ResultadoValidacion.aprobar();
    }

    public static BigDecimal calcularPorcentajeMejora(BigDecimal notaInicial, BigDecimal notaFinal) {
        Objects.requireNonNull(notaInicial);
        Objects.requireNonNull(notaFinal);
        if (notaInicial.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return notaFinal.subtract(notaInicial)
                .divide(notaInicial, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public record ResultadoValidacion(boolean aprobado, String mensaje) {
        static ResultadoValidacion aprobar() {
            return new ResultadoValidacion(true, "Plan de refuerzo válido");
        }
        static ResultadoValidacion rechazar(String razon) {
            return new ResultadoValidacion(false, razon);
        }
    }
}
