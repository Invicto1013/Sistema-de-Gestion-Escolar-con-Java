package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadorEvaluacionDocenteTest {

    @Test
    @DisplayName("Calcula promedio de evaluación docente y emite dictamen sobresaliente")
    void calcularPuntajeTotal_sobresaliente() {
        CalculadorEvaluacionDocente.EvaluacionDocente ev = new CalculadorEvaluacionDocente.EvaluacionDocente(
                4.8, 5.0, 4.7, 4.9
        );
        double puntaje = CalculadorEvaluacionDocente.calcularPuntajeTotal(ev);
        assertEquals(4.85, puntaje, 0.01);
        assertEquals("SOBRESALIENTE", CalculadorEvaluacionDocente.calificarCualitativo(puntaje));
    }

    @Test
    @DisplayName("Identifica docentes que requieren capacitación y acompañamiento")
    void calcularPuntajeTotal_requiereCapacitacion() {
        CalculadorEvaluacionDocente.EvaluacionDocente ev = new CalculadorEvaluacionDocente.EvaluacionDocente(
                2.5, 2.0, 3.0, 2.5
        );
        double puntaje = CalculadorEvaluacionDocente.calcularPuntajeTotal(ev);
        assertEquals(2.5, puntaje, 0.01);
        assertEquals("REQUIERE_CAPACITACION", CalculadorEvaluacionDocente.calificarCualitativo(puntaje));
    }
}
