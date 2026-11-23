package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Calculador de Ponderación para Ferias Científicas y Proyectos de Innovación Escolar.
 * Pondera originalidad, rigor investigativo, exposición oral y prototipo práctico.
 */
public final class CalculadorFeriaCientifica {

    public record EvaluacionProyecto(
            double originalidad,  // 0 a 25
            double rigorCientifico, // 0 a 30
            double exposicionOral,  // 0 a 25
            double prototipoPractico // 0 a 20
    ) { }

    private CalculadorFeriaCientifica() {
    }

    public static double calcularPuntajeTotal(EvaluacionProyecto eval) {
        if (eval == null) return 0.0;
        BigDecimal suma = BigDecimal.valueOf(Math.max(0.0, Math.min(25.0, eval.originalidad())))
                .add(BigDecimal.valueOf(Math.max(0.0, Math.min(30.0, eval.rigorCientifico()))))
                .add(BigDecimal.valueOf(Math.max(0.0, Math.min(25.0, eval.exposicionOral()))))
                .add(BigDecimal.valueOf(Math.max(0.0, Math.min(20.0, eval.prototipoPractico()))));

        return suma.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String determinarLugar(double puntajeTotal) {
        if (puntajeTotal >= 95.0) return "PRIMER_LUGAR_ORO";
        if (puntajeTotal >= 90.0) return "SEGUNDO_LUGAR_PLATA";
        if (puntajeTotal >= 85.0) return "TERCER_LUGAR_BRONCE";
        if (puntajeTotal >= 75.0) return "MENCION_HONORIFICA";
        return "PARTICIPACION";
    }
}
