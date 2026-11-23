package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadorFeriaCientificaTest {

    @Test
    @DisplayName("Calcula puntaje total de proyecto científico con máxima distinción (Oro)")
    void feria_oro() {
        CalculadorFeriaCientifica.EvaluacionProyecto p = new CalculadorFeriaCientifica.EvaluacionProyecto(
                25.0, 30.0, 24.0, 19.0 // 98.0 puntos
        );
        double total = CalculadorFeriaCientifica.calcularPuntajeTotal(p);
        assertEquals(98.0, total, 0.001);
        assertEquals("PRIMER_LUGAR_ORO", CalculadorFeriaCientifica.determinarLugar(total));
    }

    @Test
    @DisplayName("Pondera proyecto con mención honorífica")
    void feria_mencion() {
        CalculadorFeriaCientifica.EvaluacionProyecto p = new CalculadorFeriaCientifica.EvaluacionProyecto(
                20.0, 22.0, 20.0, 16.0 // 78.0 puntos
        );
        double total = CalculadorFeriaCientifica.calcularPuntajeTotal(p);
        assertEquals(78.0, total, 0.001);
        assertEquals("MENCION_HONORIFICA", CalculadorFeriaCientifica.determinarLugar(total));
    }
}
