package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CalificacionTest {
    @Test
    void recalcularFijaNotaFinalYLetra() {
        Calificacion c = new Calificacion();
        c.setNotaParcial1(90);
        c.setNotaParcial2(80);
        c.setNotaExamenFinal(70);
        c.recalcular();
        assertEquals(79.0, c.getNotaFinal(), 0.0);
        assertEquals("C", c.getCalificacionLiteral());
        assertTrue(c.esAprobatoria());
    }

    @Test
    void notaDeSesentaYNueveConNoventaYNueveNoAprueba() {
        Calificacion c = new Calificacion();
        c.setNotaParcial1(69.99);
        c.setNotaParcial2(69.99);
        c.setNotaExamenFinal(69.99);
        c.recalcular();
        assertEquals(69.99, c.getNotaFinal(), 0.0);
        assertFalse(c.esAprobatoria());
        assertEquals("D", c.getCalificacionLiteral());
    }

    @Test
    void rechazaNotasFueraDeRangoEnLosSetters() {
        Calificacion c = new Calificacion();
        assertThrows(IllegalArgumentException.class, () -> c.setNotaParcial1(100.01));
        assertThrows(IllegalArgumentException.class, () -> c.setNotaParcial2(-0.01));
        assertThrows(IllegalArgumentException.class, () -> c.setNotaExamenFinal(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> c.setNotaFinal(101));
    }

    @Test
    void rechazaNotasFueraDeRangoEnElConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new Calificacion(1, 1, 101, 0, 0, 0, "F"));
        assertThrows(IllegalArgumentException.class, () -> new Calificacion(1, 1, 0, -5, 0, 0, "F"));
    }

    @Test
    void aceptaLosLimitesCeroYCien() {
        Calificacion c = new Calificacion(1, 7, 0, 100, 100, 0, "F");
        assertEquals(100.0, c.getNotaParcial2(), 0.0);
        assertEquals(7, c.getIdDetalleInscripcion());
    }

    @Test
    void limitarAjustaAlRangoYTrataNaNComoCero() {
        assertEquals(0.0, Calificacion.limitar(-10), 0.0);
        assertEquals(100.0, Calificacion.limitar(250), 0.0);
        assertEquals(55.5, Calificacion.limitar(55.5), 0.0);
        assertEquals(0.0, Calificacion.limitar(Double.NaN), 0.0);
    }
}
