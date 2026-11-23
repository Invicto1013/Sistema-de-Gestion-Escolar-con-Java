package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculador de Pruebas Físicas y Baremos Deportivos (Educación Física Escolar).
 * Evalúa resistencia, velocidad, flexibilidad y fuerza según el baremo de edad.
 */
public final class CalculadorAptitudFisica {

    public record EvaluacionFisica(
            double velocidadSegundos,  // Test 50m
            int abdominalesUnMinuto,
            double flexibilidadCm,
            double resistenciaMetros   // Test Cooper
    ) { }

    private CalculadorAptitudFisica() {
    }

    public static String calificarDesempeno(EvaluacionFisica eval) {
        if (eval == null) return "SIN_DATOS";
        int puntos = 0;
        if (eval.velocidadSegundos() <= 8.5) puntos++;
        if (eval.abdominalesUnMinuto() >= 30) puntos++;
        if (eval.flexibilidadCm() >= 15.0) puntos++;
        if (eval.resistenciaMetros() >= 1800.0) puntos++;

        return switch (puntos) {
            case 4 -> "EXCELENTE_CONDICION";
            case 3 -> "BUENA_CONDICION";
            case 2 -> "CONDICION_MEDIA";
            default -> "REQUIERE_ACONDICIONAMIENTO";
        };
    }
}
