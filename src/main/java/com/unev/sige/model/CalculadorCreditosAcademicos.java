package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Calcula la acumulación de créditos académicos de un estudiante
 * hacia los requisitos de graduación del SIGE.
 */
public final class CalculadorCreditosAcademicos {

    private static final BigDecimal NOTA_MINIMA_CREDITO = new BigDecimal("70.00");
    private static final int CREDITOS_GRADUACION = 180;

    public record AsignaturaCursada(
            String codigoAsignatura,
            String nombre,
            int creditosAsignados,
            BigDecimal notaFinal
    ) {
        public AsignaturaCursada {
            Objects.requireNonNull(codigoAsignatura);
            if (creditosAsignados < 1 || creditosAsignados > 10) {
                throw new IllegalArgumentException("Créditos fuera de rango [1,10]: " + creditosAsignados);
            }
            if (notaFinal == null || notaFinal.compareTo(BigDecimal.ZERO) < 0
                    || notaFinal.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Nota final inválida");
            }
        }

        public boolean aprobada() {
            return notaFinal.compareTo(NOTA_MINIMA_CREDITO) >= 0;
        }
    }

    public record ResumenCreditos(
            int creditosAprobados,
            int creditosPendientes,
            int creditosRequeridos,
            BigDecimal promedioAcumulado,
            boolean eligibleGraduacion
    ) {}

    private CalculadorCreditosAcademicos() {}

    public static ResumenCreditos calcular(List<AsignaturaCursada> asignaturas) {
        Objects.requireNonNull(asignaturas, "La lista de asignaturas no puede ser nula");

        int aprobados = 0;
        BigDecimal sumaProductos = BigDecimal.ZERO;
        int totalCreditos = 0;

        for (AsignaturaCursada a : asignaturas) {
            Objects.requireNonNull(a, "Ninguna asignatura puede ser nula");
            if (a.aprobada()) {
                aprobados += a.creditosAsignados();
            }
            sumaProductos = sumaProductos.add(
                    a.notaFinal().multiply(new BigDecimal(a.creditosAsignados())));
            totalCreditos += a.creditosAsignados();
        }

        BigDecimal promedio = BigDecimal.ZERO;
        if (totalCreditos > 0) {
            promedio = sumaProductos.divide(new BigDecimal(totalCreditos), 2, RoundingMode.HALF_UP);
        }

        int pendientes = Math.max(0, CREDITOS_GRADUACION - aprobados);
        boolean eligible = aprobados >= CREDITOS_GRADUACION;

        return new ResumenCreditos(aprobados, pendientes, CREDITOS_GRADUACION, promedio, eligible);
    }

    public static int getCreditosRequeridosGraduacion() {
        return CREDITOS_GRADUACION;
    }
}
