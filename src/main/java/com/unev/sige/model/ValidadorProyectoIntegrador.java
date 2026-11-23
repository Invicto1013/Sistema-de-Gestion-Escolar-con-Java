package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Valida y califica proyectos integradores (capstone) de estudiantes
 * según rúbrica institucional del SIGE.
 */
public final class ValidadorProyectoIntegrador {

    private static final BigDecimal NOTA_APROBACION_PROYECTO = new BigDecimal("70.00");
    private static final int MAX_INTEGRANTES = 5;
    private static final int MIN_INTEGRANTES = 1;

    public record RubricaEvaluacion(
            BigDecimal planteamientoProblema,   // 20%
            BigDecimal marcoTeorico,            // 20%
            BigDecimal metodologia,             // 20%
            BigDecimal resultadosAnalisis,      // 25%
            BigDecimal presentacionOral         // 15%
    ) {
        public RubricaEvaluacion {
            validarCriterio(planteamientoProblema, "Planteamiento");
            validarCriterio(marcoTeorico, "Marco teórico");
            validarCriterio(metodologia, "Metodología");
            validarCriterio(resultadosAnalisis, "Resultados");
            validarCriterio(presentacionOral, "Presentación");
        }

        private static void validarCriterio(BigDecimal valor, String nombre) {
            if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0
                    || valor.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Criterio inválido: " + nombre);
            }
        }

        public BigDecimal calcularNotaFinal() {
            BigDecimal ponderado =
                    planteamientoProblema.multiply(new BigDecimal("0.20"))
                    .add(marcoTeorico.multiply(new BigDecimal("0.20")))
                    .add(metodologia.multiply(new BigDecimal("0.20")))
                    .add(resultadosAnalisis.multiply(new BigDecimal("0.25")))
                    .add(presentacionOral.multiply(new BigDecimal("0.15")));
            return ponderado.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public record ProyectoIntegrador(
            String codigoProyecto,
            String titulo,
            List<String> codigosIntegrantes,
            RubricaEvaluacion rubrica
    ) {}

    private ValidadorProyectoIntegrador() {}

    public static ResultadoValidacion validar(ProyectoIntegrador proyecto) {
        Objects.requireNonNull(proyecto, "El proyecto no puede ser nulo");

        if (proyecto.codigoProyecto() == null || !proyecto.codigoProyecto().startsWith("PROY-")) {
            return ResultadoValidacion.rechazar("Código de proyecto debe iniciar con PROY-");
        }
        if (proyecto.titulo() == null || proyecto.titulo().isBlank()) {
            return ResultadoValidacion.rechazar("El título del proyecto es obligatorio");
        }
        if (proyecto.titulo().length() < 10) {
            return ResultadoValidacion.rechazar("El título debe tener al menos 10 caracteres");
        }
        if (proyecto.codigosIntegrantes() == null
                || proyecto.codigosIntegrantes().size() < MIN_INTEGRANTES
                || proyecto.codigosIntegrantes().size() > MAX_INTEGRANTES) {
            return ResultadoValidacion.rechazar(
                    "El proyecto debe tener entre " + MIN_INTEGRANTES + " y " + MAX_INTEGRANTES + " integrantes");
        }
        if (proyecto.rubrica() == null) {
            return ResultadoValidacion.rechazar("La rúbrica de evaluación es obligatoria");
        }
        return ResultadoValidacion.aprobar(proyecto.rubrica().calcularNotaFinal());
    }

    public static boolean aprobado(RubricaEvaluacion rubrica) {
        Objects.requireNonNull(rubrica);
        return rubrica.calcularNotaFinal().compareTo(NOTA_APROBACION_PROYECTO) >= 0;
    }

    public record ResultadoValidacion(boolean valido, String mensaje, BigDecimal notaFinal) {
        static ResultadoValidacion aprobar(BigDecimal nota) {
            return new ResultadoValidacion(true, "Proyecto integrador válido", nota);
        }
        static ResultadoValidacion rechazar(String razon) {
            return new ResultadoValidacion(false, razon, null);
        }
    }
}
