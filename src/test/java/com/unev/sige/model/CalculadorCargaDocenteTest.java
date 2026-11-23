package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadorCargaDocenteTest {

    @Test
    @DisplayName("Carga moderada para docente de tiempo completo")
    void evaluar_cargaModerada() {
        CalculadorCargaDocente.EvaluacionCarga ev = CalculadorCargaDocente.evaluar(
                25, 4, 120, CalculadorCargaDocente.ModalidadContrato.TIEMPO_COMPLETO
        );

        assertEquals(25, ev.totalHorasSemanales());
        assertEquals(4, ev.totalSecciones());
        assertEquals(120, ev.totalEstudiantes());
        assertEquals(30.0, ev.promedioEstudiantesPorSeccion(), 0.001);
        assertFalse(ev.sobrecargaHoras());
        assertFalse(ev.sobrecargaSecciones());
        assertEquals("CARGA_MODERADA", ev.estadoCarga());
    }

    @Test
    @DisplayName("Detecta sobrecarga horaria al superar límite semanal del contrato")
    void evaluar_sobrecargaHoras() {
        CalculadorCargaDocente.EvaluacionCarga ev = CalculadorCargaDocente.evaluar(
                24, 4, 100, CalculadorCargaDocente.ModalidadContrato.MEDIO_TIEMPO
        ); // Medio tiempo max 20 horas

        assertTrue(ev.sobrecargaHoras());
        assertEquals("SOBRECARGA", ev.estadoCarga());
    }

    @Test
    @DisplayName("Detecta sobrecarga por exceso de secciones (> 6)")
    void evaluar_sobrecargaSecciones() {
        CalculadorCargaDocente.EvaluacionCarga ev = CalculadorCargaDocente.evaluar(
                35, 8, 240, CalculadorCargaDocente.ModalidadContrato.TIEMPO_COMPLETO
        );

        assertFalse(ev.sobrecargaHoras());
        assertTrue(ev.sobrecargaSecciones());
        assertEquals("SOBRECARGA", ev.estadoCarga());
    }
}
