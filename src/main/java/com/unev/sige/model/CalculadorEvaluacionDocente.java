package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculador de Evaluación del Desempeño Docente por parte de los Estudiantes y Dirección.
 * Pondera dominio pedagógico, puntualidad, claridad expositiva y trato respetuoso.
 */
public final class CalculadorEvaluacionDocente {

    public record EvaluacionDocente(
            double dominioTema,       // 1 a 5
            double puntualidad,       // 1 a 5
            double metodologia,       // 1 a 5
            double relacionEstudiante // 1 a 5
    ) { }

    private CalculadorEvaluacionDocente() {
    }

    public static double calcularPuntajeTotal(EvaluacionDocente eval) {
        if (eval == null) return 0.0;
        BigDecimal suma = BigDecimal.valueOf(Math.max(1.0, Math.min(5.0, eval.dominioTema())))
                .add(BigDecimal.valueOf(Math.max(1.0, Math.min(5.0, eval.puntualidad()))))
                .add(BigDecimal.valueOf(Math.max(1.0, Math.min(5.0, eval.metodologia()))))
                .add(BigDecimal.valueOf(Math.max(1.0, Math.min(5.0, eval.relacionEstudiante()))));

        return suma.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String calificarCualitativo(double puntaje) {
        if (puntaje >= 4.5) return "SOBRESALIENTE";
        if (puntaje >= 3.8) return "MUY_BUENO";
        if (puntaje >= 3.0) return "SATISFACTORIO";
        return "REQUIERE_CAPACITACION";
    }
}
