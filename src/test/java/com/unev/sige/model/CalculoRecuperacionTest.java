package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculoRecuperacionTest {

    @Test
    @DisplayName("Cálculo bajo modalidad Nivelación Compuesta (40% continuo + 60% examen)")
    void calcular_nivelacionCompuesta() {
        // Acumulado: 60, Examen: 80 -> 24 + 48 = 72.0 (Aprueba)
        CalculoRecuperacion.ResultadoRecuperacion res = CalculoRecuperacion.calcular(
                60.0, 80.0, CalculoRecuperacion.ModalidadRecuperacion.NIVELACION_COMPUESTA
        );

        assertEquals(72.0, res.notaFinalRecuperacion(), 0.001);
        assertTrue(res.aprobada());
        assertEquals("C", res.literal());
    }

    @Test
    @DisplayName("Cálculo bajo modalidad Examen de Suficiencia Integral (100% prueba)")
    void calcular_suficienciaIntegral() {
        // Examen directo: 85 -> 85.0 (Aprueba B)
        CalculoRecuperacion.ResultadoRecuperacion res = CalculoRecuperacion.calcular(
                40.0, 85.0, CalculoRecuperacion.ModalidadRecuperacion.SUFICIENCIA_INTEGRAL
        );

        assertEquals(85.0, res.notaFinalRecuperacion(), 0.001);
        assertTrue(res.aprobada());
        assertEquals("B", res.literal());
    }

    @Test
    @DisplayName("Cálculo bajo Ponderación Equilibrada (50% - 50%)")
    void calcular_ponderacionEquilibrada() {
        CalculoRecuperacion.ResultadoRecuperacion res = CalculoRecuperacion.calcular(
                60.0, 80.0, CalculoRecuperacion.ModalidadRecuperacion.PONDERACION_EQUILIBRADA
        );

        assertEquals(70.0, res.notaFinalRecuperacion(), 0.001);
        assertTrue(res.aprobada());
        assertEquals("C", res.literal());
    }
}
