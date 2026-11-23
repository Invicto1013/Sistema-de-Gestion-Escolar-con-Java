package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculador y Evaluador de Pruebas Diagnósticas de Entrada (Inicio de Año Escolar).
 * Permite medir el nivel cognitivo de los estudiantes en lectura y matemáticas al inicio
 * del ciclo para orientar los planes de refuerzo pedagógico.
 */
public final class CalculadorPruebaDiagnostica {

    public enum NivelDesempeno {
        AVANZADO,
        SATISFACTORIO,
        EN_DESARROLLO,
        INICIAL
    }

    public record ResultadoDiagnostico(
            double puntajeMatematicas,
            double puntajeLectura,
            double promedioDiagnostico,
            NivelDesempeno nivel,
            boolean requiereRefuerzoPedagogico
    ) { }

    private CalculadorPruebaDiagnostica() {
    }

    public static ResultadoDiagnostico evaluar(double matematicas, double lectura) {
        double m = Calificacion.limitar(matematicas);
        double l = Calificacion.limitar(lectura);

        BigDecimal prom = BigDecimal.valueOf(m).add(BigDecimal.valueOf(l))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        double promVal = prom.doubleValue();

        NivelDesempeno nivel;
        if (promVal >= 85.0) {
            nivel = NivelDesempeno.AVANZADO;
        } else if (promVal >= 70.0) {
            nivel = NivelDesempeno.SATISFACTORIO;
        } else if (promVal >= 60.0) {
            nivel = NivelDesempeno.EN_DESARROLLO;
        } else {
            nivel = NivelDesempeno.INICIAL;
        }

        boolean refuerzo = promVal < 70.0 || m < 65.0 || l < 65.0;

        return new ResultadoDiagnostico(m, l, promVal, nivel, refuerzo);
    }
}
