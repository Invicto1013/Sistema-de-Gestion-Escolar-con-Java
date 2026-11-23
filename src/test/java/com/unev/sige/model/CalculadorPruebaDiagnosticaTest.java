package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadorPruebaDiagnosticaTest {

    @Test
    @DisplayName("Evalúa prueba diagnóstica con desempeño satisfactorio sin refuerzo")
    void evaluar_satisfactorio() {
        CalculadorPruebaDiagnostica.ResultadoDiagnostico r = CalculadorPruebaDiagnostica.evaluar(80, 85);

        assertEquals(82.5, r.promedioDiagnostico(), 0.001);
        assertEquals(CalculadorPruebaDiagnostica.NivelDesempeno.SATISFACTORIO, r.nivel());
        assertFalse(r.requiereRefuerzoPedagogico());
    }

    @Test
    @DisplayName("Activa plan de refuerzo pedagógico si alguna competencia base es deficiente")
    void evaluar_conRefuerzo() {
        // Mat 60 y Lectura 75 -> promedio 67.5 -> Nivel EN_DESARROLLO
        CalculadorPruebaDiagnostica.ResultadoDiagnostico r = CalculadorPruebaDiagnostica.evaluar(60, 75);

        assertEquals(67.5, r.promedioDiagnostico(), 0.001);
        assertEquals(CalculadorPruebaDiagnostica.NivelDesempeno.EN_DESARROLLO, r.nivel());
        assertTrue(r.requiereRefuerzoPedagogico());
    }
}
