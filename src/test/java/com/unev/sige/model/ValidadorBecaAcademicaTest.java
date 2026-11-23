package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorBecaAcademicaTest {

    @Test
    @DisplayName("Beca total del 100% para estudiantes con índice >= 95 sin sanciones")
    void evaluar_becaTotal() {
        ValidadorBecaAcademica.EvaluacionBeca ev = ValidadorBecaAcademica.evaluar(96.5, false, 10000.0);

        assertEquals(ValidadorBecaAcademica.TipoBeca.EXCELENCIA_TOTAL, ev.becaAsignada());
        assertEquals(10000.0, ev.montoDescuento(), 0.001);
        assertEquals(0.0, ev.montoFinalPagar(), 0.001);
        assertTrue(ev.mantieneBeca());
    }

    @Test
    @DisplayName("Media beca (50%) para índice entre 90 y 94.99")
    void evaluar_mediaBeca() {
        ValidadorBecaAcademica.EvaluacionBeca ev = ValidadorBecaAcademica.evaluar(92.0, false, 8000.0);

        assertEquals(ValidadorBecaAcademica.TipoBeca.MEDIA_BECA, ev.becaAsignada());
        assertEquals(4000.0, ev.montoDescuento(), 0.001);
        assertEquals(4000.0, ev.montoFinalPagar(), 0.001);
        assertTrue(ev.mantieneBeca());
    }

    @Test
    @DisplayName("Pérdida de beca si el estudiante tiene sanción de conducta disciplinaria")
    void evaluar_conSancion_pierdeBeca() {
        // Índice de 98 pero con sanción conductual
        ValidadorBecaAcademica.EvaluacionBeca ev = ValidadorBecaAcademica.evaluar(98.0, true, 10000.0);

        assertEquals(ValidadorBecaAcademica.TipoBeca.NINGUNA, ev.becaAsignada());
        assertEquals(0.0, ev.montoDescuento(), 0.001);
        assertEquals(10000.0, ev.montoFinalPagar(), 0.001);
        assertFalse(ev.mantieneBeca());
    }
}
