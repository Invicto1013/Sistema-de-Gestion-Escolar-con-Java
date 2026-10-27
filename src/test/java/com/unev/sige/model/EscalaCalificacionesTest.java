package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EscalaCalificacionesTest {
    @Test
    void calculaPromedioPonderadoYRedondeaADosDecimales() {
        assertEquals(79.0, EscalaCalificaciones.calcularNotaFinal(90, 80, 70), 0.001);
        assertEquals(85.02, EscalaCalificaciones.calcularNotaFinal(85.03, 85.03, 85), 0.001);
    }

    @Test
    void clasificaLosLimitesDeLaEscalaDeLetras() {
        assertEquals("A", EscalaCalificaciones.literal(90));
        assertEquals("B", EscalaCalificaciones.literal(89.99));
        assertEquals("C", EscalaCalificaciones.literal(70));
        assertEquals("F", EscalaCalificaciones.literal(59.99));
    }

    @Test
    void aplicaElLimiteDeAprobacionEnElBorde() {
        assertTrue(EscalaCalificaciones.aprueba(70));
        assertFalse(EscalaCalificaciones.aprueba(69.99));
    }

    @Test
    void redondeaConExactitudDecimalDondeDoubleFallaba() {
        assertEquals(73.45, EscalaCalificaciones.calcularNotaFinal(67.19, 57.68, 89.96), 0.0);
        assertEquals(76.62, EscalaCalificaciones.calcularNotaFinal(95.15, 58.54, 76.27), 0.0);
        assertEquals(72.70, EscalaCalificaciones.calcularNotaFinal(62.63, 58.26, 91.07), 0.0);
    }

    @Test
    void notasExtremasDanResultadosExactos() {
        assertEquals(0.0, EscalaCalificaciones.calcularNotaFinal(0, 0, 0), 0.0);
        assertEquals(100.0, EscalaCalificaciones.calcularNotaFinal(100, 100, 100), 0.0);
        assertEquals(70.0, EscalaCalificaciones.calcularNotaFinal(70, 70, 70), 0.0);
    }
}
